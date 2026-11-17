# V20261110_002 迁移执行记录

| 项 | 值 |
|---|---|
| 版本 | V20261110_002（阶段 F 正式实施） |
| 脚本 | `V20261110_002__phase_f_pending_services.sql` / `V20261110_002__rollback.sql` |
| 执行时间 | 2026-11-10 21:25–21:26（北京时间） |
| 目标库 | Docker `pet-mysql` / `127.0.0.1:3308` / `pet_service` / MySQL 8.0.46 |
| 独立备份 | `.workbuddy-ai/tmp/pricing-migration-20261110/pet_service_before_V20261110_002.sql`（308,579 字节，SHA256 `6c7b907b3fe7084b8677e00bb1d902c9c7a03fc2cfc473968acbc3d094fd834b`，52 `CREATE TABLE` / 51 `INSERT INTO` / `-- Dump completed`） |
| 阶段 D 原始备份 | `pet_service_full_backup.sql` SHA256 `ad10bb0e…` **未被覆盖** |
| 执行方式 | 主脚本按 `-- SEGMENT n :` 切分为 `f_seg_0/1/2.sql`，逐段执行、逐段核验（执行 SQL 与正式脚本逐字一致） |

## 前置核对（步骤①③）

- 容器/库/版本/连接一致：`application.yml:46` 与 `docker-compose.yml:25,29` 均指向 `localhost:3308/pet_service`。
- 5 条服务前置值与授权书 100% 吻合；`booking_mode_wsh` 原值：1/2=`interval`、3=`time_slot`、9/30=`interval`。
- 历史基线：`orders=59 / snapshots=8 / payments=46 / refunds=5`，`sum_total=22120.00`、`sum_final=20790.00`、`sum_discount=1140.00`。
- 四表 SHA256 基线：`pet_order_wsh 86512cef…`、`order_snapshot_wsh 0c58921b…`、`payment_wsh bb953267…`、`refund_wsh b2f36fee…`。

## SEGMENT 0（前置断言 / 版本守卫）

| 断言 | 结果 |
|---|---|
| 5 行前置值 | 全部与授权书一致 |
| `guard_already_migrated` | **0**（未重复执行） |
| ID 29 | `day/1440/date_range/120.00/1` |
| ID 36 | `day/480/date_range/150.00/0` |
| `already_migrated_night` | **7** |
| 订单汇总 / refunds | `59 / 22120.00 / 20790.00 / 1140.00` / `5` |
| 迁移前分布 | `boarding/interval 4`、`day/date_range 2`、`night/interval 7`、`session/slot 22`、`time/time_slot 1` |

## SEGMENT 1（5 条受保护 UPDATE，显式事务）— **实际影响行数**

| 语句 | `ROW_COUNT()` 实测 |
|---|---|
| ID 1 → `night/interval/1440` | **1** |
| ID 2 → `night/interval/1440` | **1** |
| ID 3 → `session/slot/30` | **1** |
| ID 9 → `session/slot/540` | **1** |
| ID 30 → `session/slot/540` | **1** |
| `migrated_rows` | **5** |
| `price_or_status_changed` | **0** |
| `already_migrated_intact` | **7** |
| 事务 | `COMMIT` 成功，exit 0 |

每条 `WHERE` 均含 `id_wsh` + 旧 `unit_wsh` + 旧 `duration_minutes_wsh` + 旧 `booking_mode_wsh` + 旧 `price_wsh`（仅只读断言）；`SET` 仅含 `unit_wsh / booking_mode_wsh / duration_minutes_wsh`。

## SEGMENT 2（迁移后核验）

| 项 | 结果 |
|---|---|
| 5 条目标值 | `1 night/interval/1440/88.00/1`、`2 night/interval/1440/168.00/1`、`3 session/slot/30/39.00/1`、`9 session/slot/540/60.00/1`、`30 session/slot/540/70.00/1` — 全部正确 |
| ID 29 / 36 / 已迁移 7 条 | 逐字不变 |
| `mode_mismatch` | **0** |
| `illegal_units_onshelf` | **1**（仅 ID 29，冻结） |
| 迁移后分布 | `night/interval 9`、`session/slot 25`、`day/date_range 2`（29、36） |
| 订单 / refunds | `59 / 22120.00 / 20790.00` / `5` |

## 迁移前后逐条 diff（步骤⑤）

`phaseF_after_services.tsv` 与阶段 D `after_services.tsv` 对比：**恰好 10 行差异（5 删 + 5 增）**，仅 ID 1/2/3/9/30，且每行仅 `unit_wsh`、`duration_minutes_wsh`（仅 9/30）、`booking_mode_wsh`（仅 3/9/30）变化；`name_wsh`、`type_wsh`、`category_id_wsh`、`price_wsh`、`status_wsh`、`deleted_wsh` 全部不变。

## 历史财务保护（步骤⑤，迁移后实测）

| 表 | 迁移前 SHA256 | 迁移后 SHA256 | 结论 |
|---|---|---|---|
| `pet_order_wsh` | `86512cefbb5d42eb5f755a7af10510f9394239a71c50bc72992afd199bc16840` | 同左 | **IDENTICAL** |
| `order_snapshot_wsh` | `0c58921b67632d64ec5a263dd3ea3d7d18f9749722d9ef450a2d3463566168cb` | 同左 | **IDENTICAL** |
| `payment_wsh` | `bb9532679771bf191b51a8794becc3067f701eedaad555d30f64922aecb8861b` | 同左 | **IDENTICAL** |
| `refund_wsh` | `b2f36feeb3d3400a6ade679e67c4823f431bedc6de9c55a46109138cbc6ac787` | 同左 | **IDENTICAL** |

专项订单 `16/42/56/60/61/62` 逐字不变；行数 `59/8/46/5`、金额 `22120.00/20790.00` 不变。

## 严禁项确认

- ID 29：零改动（冻结，时长待确认）；ID 36：零改动（`status_wsh=0`）；已迁移 7 条：零改动。
- `pet_order_wsh` / `order_snapshot_wsh` / `payment_wsh` / `refund_wsh` / 财务表：零写入。
- 无 DDL、无 `DROP`/`TRUNCATE`、无数据卷重建、无测试订单写入生产库。
- 未修改 `V20261110_001` 任何文件。
