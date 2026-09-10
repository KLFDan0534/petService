# AI Development Context（AI 开发上下文）

## 你在开发什么

「栖屿宠护」Android App——宠物寄养/照护服务平台的移动客户端，5 角色（OWNER/KEEPER/MERCHANT/CUSTOMER_SERVICE/ADMIN），复用既有 Spring Boot 后端 `/api` 契约，视觉遵循 Warm Editorial 设计语言。

## 事实速览

- 后端：Spring Boot 3.2.5，JWT 双 Token，`Result{code,message,data}`，字段 `_wsh`，55 表
- 端点清单：06-api/endpoints.md；认证：06-api/authentication.md；实时：SSE×3 + 轮询兜底
- 角色/权限：04-user-roles/；业务状态机：05-business/state-machines.md
- 页面规范：03-pages/pages/page-001~022
- 设计 Token：01-design-system/tokens.md（视觉权威延续项目 `设计语言/` 审计账本，Services.vue 为参考页）

## 你的工作方式

1. 按 09-development/implementation-order.md 的 Phase 顺序
2. 每页实现前：读页面规范 → 组件规范 → Token → API → 权限
3. 一切视觉与结构决策以本规格包为准；规格冲突查 99-reference/conflicts.md 的裁决
4. 规格未覆盖 → 按 implementation-rules.md 默认规则保守实现并记录

## 成功标准

- 视觉与 Token 表一致（不允许第二套颜色/字号）
- 全部页面四态齐全、权限正确、状态机正确
- 下单→支付→履约→评价主链路真机可用
