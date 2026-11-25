# V20261118_001 执行记录：删除 pet_order_wsh.price_per_day_wsh

- **执行时间**：2026-11-19 19:15（GMT+8）
- **目标库**：`pet_service` @ `127.0.0.1:3308`（容器 `pet-mysql`，MySQL 8.0.46，本地开发库）
- **执行人**：WorkBuddy（经用户授权直接执行）
- **脚本**：`V20261118_001__drop_price_per_day_wsh.sql`

## 执行前状态

| 项 | 值 |
|---|---|
| 列存在 | 是，`NOT NULL` 无默认值 |
| 订单行数 | 615 |
| `SUM(total_amount_wsh)` | 212631.00 |
| `SUM(final_amount_wsh)` | 208684.53 |
| `SUM(settlement_amount_wsh)` | 170168.82 |
| `price_per_day_wsh <> unit_price_wsh` | 0 行 |
| `unit_price_wsh IS NULL` | **1 行**（见下） |

### 执行前修正（必须记录）

`id_wsh=56 / ORD202609100016`（night，3 夜，已取消）的 `unit_price_wsh` 为 `NULL`，
`price_per_day_wsh=120.00`。若直接删列会丢掉该单唯一单价快照，故先做等价回填：

```sql
UPDATE pet_order_wsh SET unit_price_wsh = price_per_day_wsh
WHERE unit_price_wsh IS NULL AND price_per_day_wsh IS NOT NULL;
-- 影响 1 行
```

## 备份

- 整表 dump：`.workbuddy-ai/tmp/db-migration-20261119/pet_order_wsh_before.sql`（427 KB，615 行）
- 列级备份表：`pet_order_wsh_colbak_20261119`（`id_wsh / days_wsh / price_per_day_wsh`，615 行）

## 执行结果

| 校验项 | 结果 | 期望 |
|---|---|---|
| `column_still_exists` | 0 | 0 |
| 订单行数 | 615 | 615 |
| `SUM(total_amount_wsh)` | 212631.00 | 不变 |
| `SUM(final_amount_wsh)` | 208684.53 | 不变 |
| `SUM(settlement_amount_wsh)` | 170168.82 | 不变 |
| `unit_price_null_rows` | 0 | 0 |

## 回滚方式

`mysql -D pet_service < V20261118_001__rollback.sql`
（按 `unit_price_wsh` 回填，语义与原写入逻辑一致）

## 备注

- 执行后 `pet_order_wsh` 的「NOT NULL 且无默认值」列只剩
  `order_no_wsh / owner_id_wsh / pet_id_wsh / merchant_id_wsh / start_date_wsh /
  end_date_wsh / total_amount_wsh / final_amount_wsh`，全部由 `doCreateOrder` 显式赋值，
  新代码 INSERT 不会因缺列失败。
