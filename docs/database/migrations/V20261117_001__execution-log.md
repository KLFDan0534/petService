# V20261117_001 生产补齐迁移执行记录

| 项 | 值 |
|---|---|
| 脚本 | `docs/database/migrations/V20261117_001__prod_catchup.sql` |
| 版本 | V20261117_001 |
| 执行时间 | 2026-11-17 21:05–21:07（北京时间） |
| 目标库 | 生产 ECS `8.148.223.70` / Docker `pet-mysql` / `pet_service` |
| 触发原因 | 生产库停留在 2026-10-09 部署版本，从未执行过 V20261110_001 及之后的任何迁移 |
| 备份 | `/opt/backup/20261117-205616/pet_service.sql`（292,801 字节，52 `CREATE TABLE`）+ jar + dist + 配置 |

## 结构差异（schema.sql 为权威基线）

| 类型 | 内容 |
|---|---|
| 缺失表 | `agent_session_wsh`(7 列)、`mq_consume_log_wsh`(4 列) |
| 缺失列 | `payment_wsh.purpose_wsh`；`pet_order_wsh` 的 `pickup_code_wsh`/`checkout_confirmed_at_wsh`/`overtime_hours_wsh`/`overtime_amount_wsh`/`overtime_payment_id_wsh` |
| 实库多余列 | `wallet_transaction_wsh` 9 列、`file_record_wsh` 2 列等历史残留，不影响运行，未清理 |

## 已审批口径的复用与偏差

本脚本复用 `V20261110_001/002`、`V20261112_001`、`V20261115_002/004` 的目标状态，但因生产库旧值不是 `boarding` 而是更早的 `day`/`date_range`，WHERE 断言按实库旧值改写：

| 服务 ID | 名称 | 迁移前 | 迁移后 | 依据 |
|---|---|---|---|---|
| 1,2 | 基础/豪华寄养 | day/1440/date_range | night/interval/1440 | V20261110_002 |
| 7,8,20,21,25,26,35 | 寄养类 | day/1440/date_range | night/interval/1440 | V20261110_001 白名单 |
| 9,30 | 日间寄养 | day/1440/date_range | session/slot/540 | V20261110_002 |
| 3 | 上门遛狗 | time/30/time_slot | session/slot/30 | V20261110_002 |

**未改动**（按审批 forbidden 清单保留原值）：`29` 术后护理（day/1440/在售）、`36` 临时暂停服务（day/480/下架）。

## 执行结果

| 核验项 | 期望 | 实际 |
|---|---|---|
| 表数量 | 54 | 54 ✓ |
| `affected_night` / `affected_daycare` / `affected_walk` | 9 / 2 / 1 | 9 / 2 / 1 ✓ |
| 门店坐标补齐 | 1 | 1 ✓ |
| 订单笔数 / 金额合计 / 支付笔数 | 59 / 21150.00 / 46 | 59 / 21150.00 / 46 ✓（零改动） |
| 迁移后单位分布 | night 9 / session 25 / day 2 | night 9 / session 25 / day 2 ✓ |

## 遗留问题（未擅自处理）

1. **服务 29「术后护理」在售但单位仍为 `day`**：新版 `BookingUnit` 只认 `night/session/hour`，该服务下单会返回 400 `UNSUPPORTED_SERVICE_UNIT`。原审批将其冻结是因为「时长口径未确认」，需业务侧给出 1440(夜) 还是 540(次) 后再映射。
2. **服务 36「临时暂停服务」** 已下架（status=0），单位 `day/480` 未映射，重新上架前需先定单位。
3. **未去列默认值**：`pet_service_wsh.unit_wsh DEFAULT 'day'`、`booking_mode_wsh DEFAULT 'date_range'` 仍存在（V20261110_001 段 3 曾要求去掉）。应用侧始终显式写单位，故不影响运行；如需与新 schema 完全一致可单独执行。
4. **未执行 V20261115_003 索引**：投诉/工单查询索引属性能优化，本次未纳入。
