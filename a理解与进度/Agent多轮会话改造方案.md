# Agent 多轮会话改造方案（最终定稿 v2）

> **状态**：方案已定稿 · **未修改任何代码**
> **日期**：2026-11-11
> **依据**：第一性原理拆解 + GitHub 一手源码调研（LangGraph / OpenAI Agents SDK / Rasa / TypeChat）
> **适用项目**：`D:\code\ideaProject\petService`

---

## 0. v2 相对 v1 的变更（用户最终决策）

| # | 最终决策 | 对 v1 的修正 |
|---|---|---|
| 1 | **新建会话后没发消息就不入库；发了消息才算新建会话成功** | 取消「单独建会话接口」。首条消息**处理成功后**才在一个事务里落地「会话行 + 消息」，空会话永不入库 |
| 2 | **历史会话还是要有的** | 恢复会话列表 / 详情 / 删除三个接口，前端加历史会话侧边栏 |
| 3 | **前端还是改成聊天窗口** | 不采用「保留卡片只加输入框」的极简 UI，改回完整气泡式聊天界面 |
| 4 | **AI 输出格式错误，还是需要重试** | 恢复 TypeChat 式 1 次修复重试（附错误原文回喂），两次失败才降级 |
| 5 | **记事尽量保留该有的，不要极简** | `state_json` 保留完整字段（意图 / 方案 / 状态 / 下一步 / 回复 / 日志 / 定位 / 重试计数 / 标题 / 最近错误），不做字段裁剪 |

其余结论不变：状态落库、删 `PlanTokenStore`、一会话一单、沿用 `next_action` 枚举、金额只由后端报价给出。

---

## 1. 一句话结论

把智能下单从「一句话 → 一张方案卡片 → 确认」改成「**一个能记住的会话**」：用户在同一会话里反复补充、修改、反悔，系统基于**落库的结构化状态**增量更新方案，直到用户显式点确认才创建订单。

改造的核心不是「让模型更聪明」，而是**把状态从提示词里拿出来，放进数据库**。

---

## 2. 现状与断点（已逐行核实源码）

现有实现位于 `pet-ai/src/main/java/com/pet/ai/`：

| 环节 | 文件 | 现状 |
|---|---|---|
| 接口 | `controller/AgentController.java:81-176` | `/chat`（客服问答，收 `history_wsh` 但后端不持久化）、`/plan`（单条文本出方案）、`/confirm`（凭 token 落单） |
| 编排 | `service/impl/AgentServiceImpl.java:148-175` | `plan()` 每次重新解析一条文本，`buildPlan()` 一次跑完全部匹配 |
| LLM | `AgentServiceImpl.java:421-510` | `extractRequirement()` 每轮只发 `system + user` 两条消息；`buildExtractionPrompt()` 是静态模板，要求「拿不准输出 null」 |
| 缓存 | `service/PlanTokenStore.java` | 纯内存 `ConcurrentHashMap`，10 分钟 TTL，`NEW → CLAIMED → CONSUMED` 状态机 |
| 前端 | `frontend/src/views/user/Agent.vue` | 表单 → 方案确认卡片 → 结果三段式；「重新描述」直接 `plan = null` 从头再来 |

**断点**：`AgentPlanRequestDTO` 只有 `input_wsh`，没有 `session_id` / `history`；`PlanTokenStore` 没有 `peek/update`；`Agent.vue` 没有修改方案的输入框。因此「改成 5 晚」会被当成一条孤立需求，宠物、日期、门店、预算全部丢失，日期回落到「明天」（`AgentServiceImpl.java:232-234`）。

---

## 3. 已确认的决策（完整）

| # | 决策 | 落实方式 |
|---|---|---|
| 1 | **方案状态落库** | 新建 `agent_session_wsh` 表存结构化状态，删除内存 `PlanTokenStore` |
| 2 | **不保留旧接口/旧页**，测试可改可删，`Agent.vue` 跟随最新 | 删 `/plan`、`/chat`、删 `PlanTokenStore`，两个旧测试重写，`Agent.vue` 原地重写（不新建页面文件） |
| 3 | **一个会话不能下多单** | 用 `status` 字段的一次 CAS 同时实现幂等与「一会话一单」，不引入 token / revision / 图框架 |
| 4 | 历史窗口按建议 | 复用 `getSessionMessages` 取最近 6 轮 + 当前状态摘要 |
| 5 | **沿用现有 `next_action` 枚举** | 生成方式改为服务端确定性计算 `firstMissingField`，前端映射表不动 |
| 6 | **空会话不入库，首条消息成功才建会话** | 见 6.0、6.3、6.5 |
| 7 | **历史会话要有**，前端做成聊天窗口 | 会话列表 / 详情 / 删除接口 + 左侧会话栏 + 右侧气泡区 |
| 8 | **LLM 输出格式错误要重试**，状态字段保留完整 | TypeChat 式 1 次修复重试；`state_json` 不做极简裁剪，见 6.7 |

---

## 4. 第一性原理拆解

剥掉框架名词，这个系统只有四个不可再分的部件：

1. **状态（State）**：一组确定性业务字段（宠物 / 入住日 / 夜数 / 门店 / 预算）+ 对话消息。
   → **多轮 = 状态被反复 patch，不是历史文本被反复喂给模型。** 状态留在提示词里就只能靠模型「回忆」，必然漂移；放进数据库，多轮只是 `UPDATE state_json`。
2. **策略（Policy）**：LLM 只做一件事 —— `(当前状态, 用户这句话) → 字段增量`。它**不**决定价格、**不**决定订单、**不**决定「还缺什么」、**不**决定「可以下单了」。
3. **副作用（Side effect）**：`createOrder + pay` 不可逆，必须有**显式提交点**，只能由用户确认事件触发。
4. **控制循环**：每轮 `读状态 → 推理增量 → 校验/合并 → 只读重算报价 → 写回状态 → 返回「缺什么 / 可确认」`，终止条件唯一 = 用户提交。

**推论 A**：字段提取只需要「当前状态 + 本轮这句话」；历史的唯一作用是理解指代与回退（「还是 3 晚吧」）以及生成自然语言回复。二者是独立通路，不必把全量历史塞进抽取提示词。

**推论 B**：`plan_token` 本质是「无会话时的临时状态句柄」。状态一旦按 `session_id` 落库，token 就是冗余，整套状态机可删。

**推论 C**：确认必须是显式事件，不能由 LLM 从语义里猜「可以下单了」。现有 `confirm` 独立接口本来就是对的，保留。

**推论 D**：会话是「一次对话的产物」，不是「先占位的容器」。所以创建时机应跟随第一条有效消息，而不是用户点开页面。

---

## 5. 开源实现对照（一手源码）

| 项目 | 关键源码 | 可迁移的结论 |
|---|---|---|
| **Rasa**（`RasaHQ/rasa-sdk`） | `rasa_sdk/forms.py` · `FormValidationAction.next_requested_slot()` / `FormAction._should_request_slot()` | 必填字段由**服务端配置**（`required_slots`），不是模型定；`next_requested_slot()` 取首个为 `None` 的槽位 → `SlotSet(REQUESTED_SLOT, ...)` |
| **TypeChat**（`microsoft/TypeChat`） | `typescript/src/typechat.ts` · `createJsonTranslator.translate()` | 模型输出必须过 schema 校验；失败把诊断原文回喂再请求一次，`attemptRepair = false` 后停止，机制上杜绝死循环 |
| **LangGraph**（`langchain-ai/langgraph`） | `libs/checkpoint/.../base/__init__.py` 的 `BaseCheckpointSaver`；`types.py` 的 `interrupt()`；官方文档 `Rules of interrupts` | 状态按 `thread_id` checkpoint 落库；确认 = 暂停/恢复；「副作用放在批准之后、独立节点、且幂等」，因为恢复会**重跑整个节点** |
| **OpenAI Agents SDK**（`openai/openai-agents-python`） | `src/agents/memory/session.py` 的 `Session` Protocol（`session_id` / `get_items(limit)`）；`run_internal/run_steps.py` 的 `NextStep*` | 会话历史与业务状态**分存**；按 `session_id` 取最近 N 条；`max_turns=10` 有界循环；`to_input_list()` 把上一轮产物转成下一轮输入 |

四处关键片段（均已逐字核对）：

```python
# rasa_sdk/forms.py —— 追问协议是状态的确定性函数
missing_slots = (slot_name for slot_name in required_slots
                 if tracker.slots.get(slot_name) is None)
return SlotSet(REQUESTED_SLOT, next(missing_slots, None))
```

```typescript
// typechat.ts —— 校验失败只修复一次
prompt.push({ role: "user", content: typeChat.createRepairPrompt(failure.message) });
attemptRepair = false;
```

```markdown
# LangGraph 官方文档 · Rules of interrupts
✅ Use idempotent operations before interrupt
✅ Place side effects after interrupt calls
✅ Separate side effects into separate nodes when possible
```

```python
# openai-agents-python · session.py
async def get_items(self, limit: int | None = None) -> list[TResponseInputItem]:
    """When specified, returns the latest N items in chronological order."""
```

---

## 6. 目标设计（完整版）

### 6.0 会话创建时机：延迟入库（v2 新增规则）

- **没有「新建会话」接口**。用户点开 `/agent` 页面时，前端 `sessionId = null`，**不发任何后端请求**，数据库零记录。
- 用户发出第一条消息：`POST /api/agent/message`，请求体**不带** `session_id`。
- 服务端先在内存里跑完一整轮（LLM 抽取 → 合并 → 校验 → 报价 → 生成回复），**全部成功后**才开启事务，一次性落地：
  1. 生成 `session_id`（复用 `AiChatHistoryService.newSessionId()`）
  2. `INSERT agent_session_wsh`（`state_json` + `status` + `title`）
  3. `INSERT ai_chat_history_wsh` 两条：`user` 消息 + `assistant` 回复
- 返回 `session_id` 给前端保存，并刷新历史会话列表。
- **任一步失败（模型不可用、两次解析都失败、业务校验异常）→ 不写任何记录**，返回错误，前端不保存 `session_id`，用户重说即可。
- 后续轮次带 `session_id` 续聊：读状态 → 处理 → 事务内 `UPDATE state` + 追加两条消息。

> 结果：**空会话永远不入库**，历史会话列表里只有「至少说过一句话」的会话。

### 6.1 三层结构

| 层 | 存什么 | 载体 |
|---|---|---|
| 对话层 | 每轮 user / assistant 消息 | **复用现有** `ai_chat_history_wsh` + `AiChatHistoryService`，按 `session_id` 隔离 |
| 状态层 | 当前用户意图 + 已匹配方案 + 会话状态 | **新建** `agent_session_wsh` 一行一个会话，`state_json_wsh` 存 JSON |
| 业务层 | 校验 / 匹配 / 报价 / 落单 | **复用** `AgentServiceImpl` 现有私有方法，只换编排入口 |

数据流：

```
用户一句话 →（首轮：先在内存跑完，成功后才建会话入库 / 续轮：读会话状态）
→ LLM 输出字段增量 → 服务端合并 + 校验 + 重算报价 → 写回状态层 + 追加消息
→ 返回「追问 / 方案卡片」
→ （用户点确认）confirm 事务：CAS 占位 → createOrder → createPayment → 可选支付
```

### 6.2 会话状态机

| 状态 | 含义 | 迁移 |
|---|---|---|
| *（未创建）* | 页面已打开、还没发过消息 | 首条消息成功后 → 建行，进入 `COLLECTING` / `AWAIT_CONFIRM` |
| `COLLECTING` | 必填字段未齐 | 每轮 message 后重算，齐了 → `AWAIT_CONFIRM` |
| `AWAIT_CONFIRM` | 方案已生成，等用户确认 | 继续修改回到 `COLLECTING` 或仍为 `AWAIT_CONFIRM`；confirm 成功 → `CONFIRMED` |
| `CONFIRMED` | 已下单，**终态** | 不再接受 message / confirm（实现「一会话一单」） |

`next_action_wsh` 的定位：**它就是 Rasa `REQUESTED_SLOT` 的弱化版** —— 服务端算出的「待请求字段」，驱动 UI 追问，不是给 LLM 的输入，也不由 LLM 生成。`firstMissingField(intent)` 按固定顺序检查，映射到现有枚举（`ask_days` / `ask_location` / `ask_pet_identity` / `unsupported_service_type` …）。

### 6.3 一轮对话的完整流程

`POST /api/agent/message`（`session_id` 可空）：

1. **首轮（无 session_id）**：构造空状态；**续轮**：读会话（校验 `user_id`），状态须非 `CONFIRMED`；
2. 组装 LLM 输入：`system`（增量抽取契约，注入当天日期 + 当前状态摘要）+ 最近 6 轮对话（复用 `getSessionMessages`）+ 本轮 user 消息；
3. LLM 输出 → Jackson 转 `AgentPromptResult`（**复用现有 DTO 作为 schema**）并做字段类型/枚举校验；
4. **格式错误重试（TypeChat 式，最多 1 次）**：把上一轮原始输出 + `createRepairPrompt(错误原文)` 追加进消息再请求一次；`llmRetryCount` 记入状态；两次都失败 →
   - 首轮：**不落库**，返回「没太听清，请再说一遍」；
   - 续轮：**保持上一版状态不变**，仍写入 user 消息 + assistant 澄清回复（保证对话历史完整），返回提示；
5. **合并**：patch 中非 null 字段覆盖 `intent`，null 字段**沿用旧值**（关键改动，不再「拿不准就清空」）；
6. 确定性重算，全部复用 `AgentServiceImpl` 现有私有方法：
   - `BookingUnit.normalize` 单位门控 + `NON_BOARDING_TYPES` 服务类型门控
   - `matchPet` / `parseStartDate`（含 `start.isAfter(LocalDate.now())`，防跨天会话后日期过期）/ 夜数范围
   - `merchantMapper.searchNearby` + `keeperService.searchNearby` + `rankKeepers` + `pickNightService`
   - `previewNightTotal`（**金额唯一来源**，仍走 `orderService.previewOrder`）
   → 得到新的 `AgentPlanSnapshot`；
7. `nextAction = firstMissingField(intent)`；字段齐 → `AWAIT_CONFIRM` 并返回方案卡片，缺 → `COLLECTING` 并返回追问；生成 `reply` 与本轮 `logs`；
8. **事务内落地**：首轮 `INSERT` 会话行 + 两条消息；续轮 `UPDATE state_json` + `status` + `updated_at` + 追加两条消息；
9. 返回 `AgentTurnResult`（`session_id` / `reply` / `status` / `next_action` / `plan_card` / `logs`）。

### 6.4 确认下单（幂等关键）

`POST /api/agent/confirm`，在同一个 `@Transactional` 内：

```sql
UPDATE agent_session_wsh SET status_wsh = 'CONFIRMED'
WHERE session_id_wsh = ? AND user_id_wsh = ? AND status_wsh = 'AWAIT_CONFIRM';
```

- 影响行数 ≠ 1 → 拒绝（重复点击、已下单、状态不对都挡住）；
- 通过后复用现有 `verifyAutoPayAuthorization` / 手机号校验 / `buildOrderRequest` / `createOrder` / `createPayment` / `pay` / `step9SendNotification`；
- **失败时整个事务回滚，`status` 自动退回 `AWAIT_CONFIRM`，天然可重试** —— 不再需要 `PlanTokenStore.release()`；
- CAS 与建单在同一事务，不需要 `afterCommit` 回调。

### 6.5 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| 新增 | `POST /api/agent/message` | **一轮对话**。`session_id` 为空 = 首轮（成功后才建会话并入库）；有值 = 续聊 |
| 新增 | `GET /api/agent/sessions` | **历史会话列表**：只返回有消息的会话，按 `updated_at` 倒序，返回 `session_id / title / status / created_at / updated_at` |
| 新增 | `GET /api/agent/sessions/{sid}` | 恢复会话：消息列表 + 当前方案卡片（刷新页面不丢） |
| 新增 | `DELETE /api/agent/sessions/{sid}` | 删除历史会话：删状态行 + 复用 `clearSession` 逻辑删消息 |
| 改造 | `POST /api/agent/confirm` | 入参 `plan_token_wsh` → `session_id_wsh`，`auto_pay` / `payment_password` 保留 |
| **删除** | `POST /api/agent/plan`、`POST /api/agent/chat` | 不再兼容 |

**没有** `POST /api/agent/sessions` —— 空会话不允许入库（决策 6）。

> 注意：`/api/ai/chat`（`AiChatServiceImpl`，RAG 客服）是**另一套**，保留不动，别删错。

### 6.6 LLM 输出契约与重试

直接复用 `AgentPromptResult` 的蛇形 JSON 作为输出 schema（零新增 DTO）：

```json
{"pet_name":"布丁","pet_type":null,"service_type":"BOARDING",
 "start_date":"2026-11-15","nights":5,"merchant_keyword":"衡山路",
 "max_price_per_night":150,"unit":"night"}
```

提示词相对现有 `buildExtractionPrompt()` 只改两点：① 注入「当前已确认字段」摘要；② 把「拿不准输出 null」改成「**未提及的字段输出 null，服务端会沿用旧值**」，防止模型脑补。

修复提示（对齐 TypeChat `createRepairPrompt`）：

```
你上一次输出的 JSON 不合法，原因：
"""
{具体校验错误，如：nights 必须是正整数 / unit 只能是 night|session|hour}
"""
请按同一 schema 重新输出一个完整合法的 JSON 对象，不要输出其它文字。
```

重试上限 **1 次**，从机制上杜绝死循环；两次失败按 6.3 第 4 步降级。自然语言 `reply` 由服务端按状态生成并存入状态；若后续要更自然的模型回复，可让输出增加 `reply` 字段，不影响主流程。

### 6.7 会话状态字段（完整保留，不做极简裁剪）

`state_json_wsh` 对应 `AgentSessionState`：

| 字段 | 类型 | 用途 |
|---|---|---|
| `intent` | `AgentPromptResult`（复用） | 用户意图 / 槽位，含 null 语义与兼容字段 |
| `plan` | `AgentPlanSnapshot`（复用） | 已匹配方案 + 报价 |
| `status` | String | `COLLECTING` / `AWAIT_CONFIRM` / `CONFIRMED` |
| `nextAction` | String | 现有 `next_action` 枚举，服务端算 |
| `reply` | String | 本轮助手回复，刷新时可直接回放 |
| `logs` | `List<String>` | 每轮执行日志（沿用现有 `plan` 的 logs 概念） |
| `latitude` / `longitude` / `address` | Double / String | 首次定位后沿用，避免每轮都要求前端传坐标 |
| `llmRetryCount` | int | 本轮修复重试次数（每轮重置），诊断用 |
| `firstUserInput` | String | 首条原始输入，用于会话列表摘要与订单备注 |
| `lastError` | String | 最近一次解析/校验失败原因，诊断用 |

`agent_session_wsh` 另存一个 `title_wsh` 冗余列（首条用户输入截断），让历史会话列表一次查询即可显示摘要，不必联查聊天表。

---

## 7. 数据层：基于现有数据能改到什么程度（核心章节）

### 7.1 零改动复用的现有数据库表

| 表 | 复用方式 | 是否需要改表结构 |
|---|---|---|
| `ai_chat_history_wsh`（`schema.sql:898`、`h2-schema.sql:851`） | 存每轮 user/assistant 消息，已有 `session_id_wsh` + `(user_id, session_id)` 索引 | **不需要** |
| `ai_config_wsh` | LLM 配置，继续走 `AiConfigStore.getActiveConfig(USAGE_AGENT)` | 不需要 |
| `user_wsh` / `pet_wsh` / `merchant_wsh` / `keeper_wsh` / `pet_service_wsh` | 匹配与校验数据源 | 不需要 |
| `pet_order_wsh` / `payment_wsh` | 下单与支付 | 不需要 |

**为什么不需要给 `ai_chat_history_wsh` 加 `scene` 字段**：下单会话列表改由新建的 `agent_session_wsh` 提供，客服页继续用原来的 `/api/ai/history/sessions`，两边按不同的 `session_id` 自然隔离。

**为什么不需要给现有业务表加字段**：所有新状态都收在 `agent_session_wsh.state_json_wsh` 里，订单表保持原样。

### 7.2 零改动复用的现有 Java 代码

| 现有资产 | 复用方式 |
|---|---|
| `dto/AgentPromptResult.java` | 直接作为 LLM 输出 schema 与「用户意图 / 槽位」载体 |
| `dto/AgentPlanSnapshot.java` | 直接作为「已匹配方案 + 报价」载体 |
| `dto/AgentExecuteResult.java` | confirm 结果返回体，字段齐全 |
| `dto/AgentChatRequestDTO.java` / `AgentChatResponseDTO.java` | 随 `/chat` 删除而删除（不复用） |
| `AgentServiceImpl` 私有方法 | `resolveLocation` / `matchPet` / `parseStartDate` / `rankKeepers` / `pickKeeperAndService` / `pickNightService` / `previewNightTotal` / `buildOrderRequest` / `verifyAutoPayAuthorization` / `step9SendNotification` / `safeMessage` —— **全部原样复用** |
| `LlmChatService.chat(List<Map>)` / `LlmChatServiceImpl` | 已支持多轮消息与动态配置，**零改动** |
| `AiChatHistoryService` + `AiChatHistory` + `AiChatHistoryMapper` | `saveSimple` / `getSessionMessages` / `clearSession` / `newSessionId` 直接注入使用。`pet-ai/pom.xml:33-35` 已依赖 `pet-business`，**无需改 pom** |
| `Agent.vue` 的 `nextActionText()` 映射表、`unitLabel`、`money` | 前端工具函数复用 |

### 7.3 必须新增

| 类型 | 文件 / 对象 | 说明 |
|---|---|---|
| 表 | `agent_session_wsh`（MySQL + H2 各一份） | 见 7.6 DDL，含 `title_wsh` |
| 实体 | `pet-ai/.../entity/AgentSession.java` | 映射新表 |
| Mapper | `pet-ai/.../mapper/AgentSessionMapper.java` | 按 `session_id + user_id` 查、按用户列会话、CAS 更新 status、插入/删除 |
| DTO | `dto/AgentSessionState.java` | 完整状态壳，字段见 6.7 |
| DTO | `dto/AgentMessageRequestDTO.java` | `{ session_id?, content, latitude?, longitude?, address? }` |
| DTO | `dto/AgentTurnResult.java` | `{ session_id, reply, status, next_action, requires_user_input, plan_card, logs }` |
| DTO | `dto/AgentSessionSummaryVO.java` | 历史会话列表项 |
| 服务 | `service/AgentSessionStore.java` | `state_json` 读写（Jackson）、首轮事务落地、CAS、列表查询 |
| 测试 | `pet-admin/src/test/.../AgentConversationTest.java` | 见第 8 节 |
| 前端 | `api/ai.js` 新增 4 个函数；`Agent.vue` 重写 | 见第 8 节 |

### 7.4 必须修改

| 文件 | 改动 |
|---|---|
| `service/AgentService.java` | 接口从 `plan/confirm` 改为 `message / listSessions / getSession / clearSession / confirm` |
| `service/impl/AgentServiceImpl.java` | 构造参数 `PlanTokenStore` → `AgentSessionMapper` + `AiChatHistoryService`；`buildPlan()` 拆成「读状态 → 合并 patch → 复用现有方法重算 → 事务内写回」；首轮延迟建会话；`confirm()` 头部换成 status CAS |
| `controller/AgentController.java` | 删 `/plan`、`/chat`；新增 4 个会话/消息接口；`/confirm` 换参数；删掉 `SYSTEM_PROMPT`（"小宠"客服设定） |
| `dto/AgentConfirmRequestDTO.java` | `plan_token_wsh` → `session_id_wsh`，其余两个字段保留 |
| `dto/AgentPlanResult.java` | 改造成方案卡片返回体：`plan_token_wsh` → `session_id_wsh`，加 `reply_wsh`，其余展示字段与 `status_wsh` / `next_action_wsh` 保留 |
| `schema.sql` + `h2-schema.sql` | 各加一条 `agent_session_wsh` 建表 |
| `frontend/src/api/ai.js` | 删 `executeAgentPlan` / `aiChat`，新增 `sendAgentMessage / listAgentSessions / getAgentSession / clearAgentSession`，`executeAgentConfirm` 换参数 |
| `frontend/src/views/user/Agent.vue` | **原地重写为聊天窗口**：左侧历史会话栏 + 右侧消息气泡（user / assistant / 方案卡片 / 结果卡片）+ 底部输入框；方案卡片内保留自动支付开关与支付密码 |
| 路由 | `frontend/src/router/index.js:335-337` 的 `/agent` → `Agent.vue` **保持不变** |
| 旧测试 | `AgentNightBookingTest` / `AgentPaymentAuthorizationTest` **重写**：不再构造 `PlanTokenStore`，改 mock `AgentSessionMapper`；断言改为「多轮 message → confirm」 |

### 7.5 必须删除

- `service/PlanTokenStore.java`（整个类，状态机被 DB CAS 取代）
- `dto/AgentPlanRequestDTO.java`
- `AgentController` 中的 `/api/agent/plan` 与 `/api/agent/chat`
- `Agent.vue` 的旧表单 / 旧确认卡片（被重写覆盖，不保留兼容分支）
- 两个旧测试中关于 token 的断言

### 7.6 DDL

MySQL（`pet-admin/src/main/resources/db/schema.sql` 追加）：

```sql
CREATE TABLE IF NOT EXISTS `agent_session_wsh` (
    `session_id_wsh` VARCHAR(64) PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `title_wsh` VARCHAR(200),
    `state_json_wsh` TEXT,
    `status_wsh` VARCHAR(20) NOT NULL DEFAULT 'COLLECTING',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_agent_session_user` (`user_id_wsh`, `updated_at_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能下单会话状态';
```

H2（`pet-admin/src/main/resources/h2-schema.sql` 追加）：按该文件现有风格（`:851-863`）去掉 `ENGINE` / `CHARSET` / `COMMENT`，其余相同。**两份脚本必须同步**，否则测试库起不来。

---

## 8. 逐文件改造说明（速查）

**后端 `pet-ai`**

- `AgentServiceImpl` 是唯一大改文件：保留全部私有方法与业务红线，只替换「入口编排」「状态存取」「首轮事务落地」。
- `confirm` 事务结构沿用现有 `try / catch (NeedUserInputException) / catch (Exception) + setRollbackOnly()`，只把 `planTokenStore.claim()` 换成 CAS，并删掉 `release` / `consumeOnCommit`。
- 金额、单位门控、14:00/14:30 锚点、备注 HTML 转义、支付密码校验、MQ 通知失败非致命 —— **一律不动**。
- 首轮「先算完再入库」与 confirm 的 CAS 都放在 `@Transactional` 方法里，失败整体回滚，天然不产生空会话或不完整状态。

**前端 `Agent.vue`（聊天窗口 + 历史会话）**

- 左侧历史会话栏：进页面调 `GET /api/agent/sessions` 拉列表（可为空）；点击切换会话 → `GET /sessions/{sid}` 恢复消息与方案卡片；「新会话」按钮**只清本地 `sessionId`，不调后端**（对应空会话不入库）。交互可参考 `AiChat.vue:180` 与 `UserLayout.vue:199` 的会话列表写法，但因提示词与返回结构不同，不复用该组件本身。
- 右侧气泡区：user 文本 / assistant 文本 / 方案卡片 / 结果卡片；底部输入框常驻。
- 首次进入 `sessionId = null`，**不发建会话请求**；第一条消息成功后保存返回的 `session_id` 并刷新左侧列表。
- 刷新页面从本地存储恢复 `sessionId` → 调详情接口恢复；方案卡片每轮覆盖式刷新（因此不需要 revision 乐观锁）。
- `nextActionText()` 映射表原样保留；自动支付开关与 6 位支付密码放在方案卡片内，逻辑沿用。

**测试**

- 旧两个测试重写为新契约；新增 `AgentConversationTest` 覆盖：① 未发消息时会话列表为空；② 首条消息成功后才出现 1 条会话；③ 首条消息失败时列表仍为空；④ 第 1 轮订 3 晚 → 第 2 轮改 5 晚 → 第 3 轮换门店，未提及字段沿用；⑤ 「还是 3 晚吧」回退；⑥ 跨天后旧日期被拒；⑦ 同一会话并发 confirm 只成功一次；⑧ LLM 格式错误恰好重试 1 次，两次失败状态不丢。

---

## 9. 分步实施与验收（每步停下汇报）

| 步 | 内容 | 验收 |
|---|---|---|
| 1 | 建 `agent_session_wsh`（两份 schema）+ entity/mapper + `AgentSessionStore`；**不动业务行为** | 应用正常启动，H2 与 MySQL 脚本均可建表 |
| 2 | 会话层：`message` 接口骨架 + 首轮延迟入库 + 历史会话列表/详情/删除（复用 `AiChatHistoryService`） | **未发消息时列表为空**；首条消息成功后列表出现 1 条；故意让处理失败时列表仍为空 |
| 3 | 对话核心：`AgentServiceImpl` 改成每轮增量编排，含 1 次修复重试 + 状态合并 + 报价重算 | `AgentConversationTest` 多轮与重试用例全绿 |
| 4 | confirm 改造：status CAS 幂等 + 旧接口删除 + 两个旧测试重写 | 全部 Agent 测试通过；并发 confirm 只成一单 |
| 5 | 前端 `Agent.vue` 聊天窗口 + 历史会话栏 + `api/ai.js` + 前端单测 | 手动走通「新会话 → 首句 → 改夜数 → 换门店 → 切换历史会话 → 确认 → 自动支付」 |

---

## 10. 风险与红线

**红线（现有代码已做对，调研也印证，禁止改）**

- 金额只来自 `orderService.previewOrder`；LLM 永不产生价格、永不产生订单；
- 只支持按夜寄养，`BookingUnit.normalize` 门控保留；
- `preview`（只读）与 `createOrder`（写）分离；副作用只在确认之后；
- `confirm` 必须是独立显式事件，不能由 LLM 语义触发。

**风险**

- 提示词改造后模型可能不守 JSON 契约：沿用 `parseJson()` 的围栏容错 + 1 次修复重试，仍失败按 6.3 降级，不把非法 JSON 写进历史；
- 跨天会话后旧入住日可能已过期，合并阶段必须重跑 `start.isAfter(LocalDate.now())`；
- 会话状态用 JSON 列，字段增删靠 Jackson 兼容，升级时注意给 `AgentSessionState` 保留默认值；
- `state_json_wsh` 不含敏感信息（支付密码只在 confirm 请求里出现，不落库）；
- 历史会话只增不减，`DELETE` 由用户主动触发；如需自动清理，后续加定时任务按 `updated_at` 处理即可。

---

## 附录 A · 源码与文档出处（均为一手）

| 来源 | 位置 |
|---|---|
| 本项目现状 | `pet-ai/.../controller/AgentController.java`、`service/impl/AgentServiceImpl.java`、`service/PlanTokenStore.java`、`frontend/src/views/user/Agent.vue` |
| 现有会话基础设施 | `pet-admin/src/main/resources/db/schema.sql:898-909`、`h2-schema.sql:851-863`、`pet-business/.../service/AiChatHistoryService.java`、`controller/AiChatHistoryController.java` |
| 模块依赖 | `pet-ai/pom.xml:33-35`（已依赖 `pet-business`） |
| 前端路由 | `frontend/src/router/index.js:335-337`（`/agent` → `Agent.vue`） |
| Rasa | `RasaHQ/rasa-sdk` · `rasa_sdk/forms.py`；`RasaHQ/rasa` · `rasa/core/actions/forms.py` |
| TypeChat | `microsoft/TypeChat` · `typescript/src/typechat.ts`、`ts/validate.ts` |
| LangGraph | `langchain-ai/langgraph` · `libs/checkpoint/langgraph/checkpoint/base/__init__.py`、`libs/langgraph/langgraph/types.py`；官方文档 `docs.langchain.com/oss/python/langgraph/interrupts` |
| OpenAI Agents SDK | `openai/openai-agents-python` · `src/agents/memory/session.py`、`memory/sqlite_session.py`、`run_internal/run_steps.py`、`result.py` |

---

## 附录 B · 实施回写（v3，与代码对齐）

> 第 1–10 章是**设计时**的定稿。落地过程中有两轮对抗式审查（每轮一个只读子代理），
> 修掉了若干设计阶段没考虑到的问题。本附录记录**最终实现与设计的差异**，读文档时以这里为准。

### B.1 接口与字段的最终形态

| 项 | 设计稿 | 最终实现 | 原因 |
|---|---|---|---|
| `POST /api/agent/message` 的 `session_id_wsh` | 空 = 首轮，由服务端生成 | **前端在首轮之前就生成并一直沿用**；服务端仅在为空时兜底生成 | 否则网络超时重发会多建一个会话 |
| 新增字段 | — | `request_id_wsh`（前端每轮生成，`@Size(64)`） | 超时重发时服务端按 `state.lastRequestId` 命中即回放，不重复调模型、不重复记消息 |
| 「首轮」判定 | 请求里有没有 `session_id` | **库里有没有这一行**（`row == null`） | 让「前端预生成ID」也能走「成功才入库」 |
| `AgentSessionState` 新增 | — | `lastRequestId` | 幂等去重的槽位（单槽位，只对紧邻上一轮有效） |

### B.2 事务边界：`message()` 不再整体包事务

设计稿写的是「同一事务」。最终**去掉了方法级 `@Transactional`**，原因是一轮里有 2 次大模型调用（原始 + 修复），
几秒钟占着数据库连接，10 个并发用户就能把连接池耗尽。

最终结构：

```
message()                      ← 无事务，只加「会话分段锁」
  ├─ 读状态 / LLM 抽取 / 合并 / 匹配 / 报价     ← 全在事务外
  └─ persistTurn()             ← TransactionTemplate 只包这一段
        ├─ insert 或条件 update（状态）
        └─ 两条 saveSimple（消息）
```

`confirm()` 仍然整体 `@Transactional`（里面没有 LLM 调用），`TransactionSynchronization.afterCommit` 的 MQ 通知照旧生效。

### B.3 并发：会话分段锁

`message()` 用 `ReentrantLock[64]`（按 `sessionId.hashCode()` 取模）保证**同一会话同一时刻只处理一轮**；
拿不到锁直接返回 `409 上一句还在处理中`。

- 没有这把锁时，「读状态 → 调模型（数秒）→ 写状态」是无保护的读改写，两个标签页 / 连点两次会互相整份覆盖。
- 用分段锁而不是「每会话一把锁」，是为了避免会话数无限增长带来的内存泄漏。
- **已知局限**：这是单实例锁。多实例部署时同一会话仍可能并发，那时需要换成「`state_version_wsh` 乐观锁」或分布式锁。

### B.4 审查修掉的问题（两轮共 22 条，按性质归类）

**会直接造成资损或数据错乱的**

1. `setRollbackOnly()` + 正常 return 会抛 `UnexpectedRollbackException` → 改为直接上抛 `BusinessException`。
2. CAS 把会话改成 `CONFIRMED` 之后，又被后续 `updateById(row)` 整行覆盖回 `AWAIT_CONFIRM` → **能重复下单**。
   已删除整行 update，改为两条带状态条件的语句（`updateState` 带 `status <> 'CONFIRMED'`，`updateConfirmedState` 带 `= 'CONFIRMED'`），影响行数不为 1 即报错回滚。
3. 默认「明天」被固化进状态，跨天后再确认就是过期日期 → 只固化用户明确给出的日期，过期即清空并追问。

**会造成用户可见错误的**

4. 消息只按 `created_at`（秒级）排序，同一事务插入的一问一答顺序不确定 → 加 `id_wsh` 兜底。
5. 方案卡片累积，点旧卡片实际下的是新方案 → 前端全局只保留最新一张；非 `await_confirm` 一律移除。
6. `nights` 小数被 Jackson 静默截断成整数 → 先用 `readTree` 判 `isNonIntegralNumber`。
7. 历史回灌把带「预估 ¥450」的助手回复喂回模型，模型把历史价格当预算 → 只喂用户原话。
8. 门店偏好匹配不到时字段永久留在状态里，用户说「随便找一家」也永远回到同一个错误 → 无匹配就清掉该偏好，按无偏好继续推荐。
9. 模型偶尔用 `days` 表达「几晚」，但 `buildPlan` 优先取旧 `nights` → 用户说「改成 5 天」静默无效。合并时 `days` 回填到 `nights`。
10. 续轮写状态失败时一律提示「已完成下单」，实际可能是会话被删了 → 区分两种情况。

**可用性 / 安全 / 运维**

11. 首轮无幂等键 → 见 B.1。
12. MQ 通知在事务提交前发送（消费者查不到订单，回滚时还会发幽灵通知）→ `notifyAfterCommit`。
13. `deleteSession` 无事务、先删状态再清消息 → 加 `@Transactional`，改为先清消息后删状态行。
14. 确认成功的结果不落库，刷新即丢订单号 → `confirm` 内 `saveSimple` 写入订单号。
15. LLM 调用在事务内占连接 → 见 B.2。
16. 前端把 400/409 业务错误一律显示成「网络异常，这句话没发出去」→ 取 `e.response.data.message`。
17. `session_id_wsh` 无长度/格式校验，超 64 字符先烧一次 LLM 再 500 → 加 `@Size(64)` + `@Pattern`。
18. 客户端传「已被别人占用的会话ID」→ 主键冲突 → 500，且构成 ID 存在性探针 → 捕获 `DuplicateKeyException` 转 400。
19. 在途请求期间点「删除会话 / 新建会话」，响应会落到错误的会话视图（甚至把 `CONFIRMED` 写到新会话上锁死输入框）→ 加 `sending || confirming` 守卫 + 响应落点校验。
20. LLM 抛异常会绕过降级逻辑直接 500 → 调用处包一层，异常降级成「没听清」。

### B.5 测试覆盖

`mvn -o test` 全量 **524 项通过**（0 失败 / 1 跳过，跳过项为改造前既有）。

`AgentConversationTest`（18）覆盖：空会话不入库 / 首条成功才建会话 / 首条失败不入库 / 恰好重试 1 次 /
第 2 轮只改夜数 / 回退 / 换门店 / 续轮失败保留旧方案 / `ask_days` 收集 / 已下单拒绝 / 并发 confirm 不被覆盖 /
过期日期 / `nights` 小数 / **前端预生成会话ID** / **同 `request_id` 重发回放** / **会话ID被占用返回 400** /
**带事务模板的分支能落库**。

`InMemoryAgentSessionStore` 已改成「重复插入抛 `DuplicateKeyException`」，
不再静默覆盖——否则「两个用户撞同一个会话ID」在单测里永远发现不了。

### B.6 遗留（明确不做）

| 项 | 为什么不做 |
|---|---|
| 「在途重发」的强幂等 | 需要一张 `agent_request` 占位表（唯一键 `(user_id, request_id)` + RUNNING/DONE）。当前用会话锁 + 409 挡住，收益/复杂度不划算 |
| 多实例下的并发正确性 | 同上，需要乐观锁或分布式锁；当前部署是单实例 docker-compose |
| 跨天过期方案在 `getSession` 里就不展示卡片 | 目前由 `confirm` 拦下并返回 `ask_days`，前端已按此移除卡片 |
