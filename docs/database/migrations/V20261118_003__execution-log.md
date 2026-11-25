# V20261118_003 执行记录：删除 pet_order_wsh.days_wsh

- **执行时间**：2026-11-19 19:16（GMT+8）
- **目标库**：`pet_service` @ `127.0.0.1:3308`（容器 `pet-mysql`，MySQL 8.0.46，本地开发库）
- **执行人**：WorkBuddy（经用户授权直接执行）
- **脚本**：`V20261118_003__drop_days_wsh.sql`
- **前置**：`V20261118_001` 已执行（同日 19:15）

## 执行前状态

| 项 | 值 |
|---|---|
| 列存在 | 是，`NOT NULL` 无默认值 |
| 订单行数 | 615 |
| 金额合计 | total 212631.00 / final 208684.53 / settlement 170168.82 |
| `quantity_wsh IS NULL` | 0 行 |

## 前置校验的真实数据结论（重要）

脚本输出的 `inconsistent_days_rows = 250` 是**校验 SQL 的口径假设有误**，不是数据问题：

- 该 CASE 假设 `session → days=1`，但库里有 250 条历史 session 单 `days = quantity = 2~4`
  （早期演示数据，当前代码已强制 session 数量为 1）
- 用列备份表逐行比对后，`days_wsh` 与 `quantity_wsh` 的真实关系是：

| 计费单位 | 行数 | days 与 quantity 的关系 |
|---|---|---|
| night | 132 | 完全相等 |
| session | 394 | 完全相等（含上述 250 条历史多数量单） |
| hour | 89 | **不等**：`days` 恒为 1，`quantity` 为 2~6（槽位数） |

**结论：删除 `days_wsh` 无数据损失**，`quantity_wsh` 完整保留了原有信息；
反过来说，这 89 条按小时订单（2~6 个槽位）若用 `days` 替代 `quantity`，
会按 1 个槽位计价并少算容量——这是选择「删 days、保 quantity」的直接数据证据。

## 备份

- 整表 dump：`.workbuddy-ai/tmp/db-migration-20261119/pet_order_wsh_before.sql`（与 001 共用，删列前导出）
- 列级备份表：`pet_order_wsh_colbak_20261119`

## 执行结果

| 校验项 | 结果 | 期望 |
|---|---|---|
| `column_still_exists` | 0 | 0 |
| 订单行数 | 615 | 615 |
| 金额合计 | 与执行前完全一致 | 不变 |
| `quantity_null_rows` | 0 | 0 |
| `days = quantity` 行数 | 526 | — |
| `days <> quantity` 行数 | 89（全部 hour） | — |

## 回滚方式

`mysql -D pet_service < V20261118_003__rollback.sql`
（按 `night→quantity / session·hour→1 / 其它→quantity` 回填）

## 后续清理

确认业务无异常后可删除临时备份：

```sql
DROP TABLE pet_order_wsh_colbak_20261119;
```

## 备注

- 本地 8080 后端当时未运行，不存在「旧代码仍写已删列」的窗口。
- 新代码 + 新 schema 已由 `mvn -o test` 验证（pet-business 404 / pet-admin 251 全绿）。
