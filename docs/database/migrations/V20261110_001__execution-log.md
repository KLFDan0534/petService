# V20261110_001 迁移执行记录

| 项 | 值 |
|---|---|
| 脚本 | `docs/database/migrations/V20261110_001__pricing_unit_unify.sql` |
| 回滚 | `docs/database/migrations/V20261110_001__rollback.sql` |
| 版本 | V20261110_001 |
| 执行时间 | 2026-11-10 18:27–18:29（北京时间） |
| 目标库 | Docker `pet-mysql` / `127.0.0.1:3308` / `pet_service` / MySQL 8.0.46 |
| 备份 | `.workbuddy-ai/tmp/pricing-migration-20261110/pet_service_full_backup.sql`（306,149 字节，SHA256 `ad10bb0e80277b9201146d55a2a0e343bbd8e66a48b75a749b1892859431b653`，52 `CREATE TABLE` / 51 `INSERT INTO` / `-- Dump completed`） |
| 基线 | 同目录 `before_*.tsv` / `after_*.tsv` |
| 执行方式 | 主脚本按 `-- SEGMENT n :` 标记切分为 `seg_0..seg_5.sql`，**逐段执行、逐段核验**（DDL 隐式提交，非原子） |

## 版本守卫

SEGMENT 0 输出 `order_cols_present=0`、`purpose_present=0`，确认迁移未执行过，允许开始。各段 DDL 均为 `information_schema` 判断 + `PREPARE/EXECUTE` 幂等写法，SEGMENT 4 DML 用显式事务 + ID 白名单 + 旧值断言。

## 逐段执行结果

| 段 | 内容 | 核验结果 |
|---|---|---|
| 0 | PRE-CHECK / 版本守卫 | `base_tables=52`；白名单 7 条均 `boarding/1440/interval`；未确认 7 条原值符合；订单 `59 / 22120.00 / 1140.00 / 190.00 / 20790.00 / 240.00`；订单单位 `day35/session21/boarding3`；`payment_rows=46`；`mismatched_payments=0`；服务单位 `session22/boarding11/day2/time1` — **全部符合预期** |
| 1 | `pet_order_wsh` 补 5 列 | 5 列全部创建：`pickup_code_wsh varchar(4) NULL`、`checkout_confirmed_at_wsh datetime NULL`、`overtime_hours_wsh int NOT NULL DEFAULT 0`、`overtime_amount_wsh decimal(10,2) NOT NULL DEFAULT 0.00`、`overtime_payment_id_wsh bigint NULL`；`order_row_count=59` |
| 2 | `payment_wsh.purpose_wsh` | `varchar(32) NOT NULL DEFAULT 'order'`；46 行全部 `purpose_wsh='order'`；`payment_rows=46`；`file_record_wsh.purpose_wsh` 仍 `varchar(50)` 未被误伤 |
| 3 | 去默认值 / 修 COMMENT | `unit_wsh`、`billing_unit_wsh`、`booking_mode_wsh` 均为 `<NO DEFAULT>`；`quantity_wsh DEFAULT 1` 保留并修正 COMMENT；CHECK 约束数 `0`（未加约束）；订单金额不变 |
| 4 | 白名单 7 条 DML | `whitelist_not_migrated=0`、`unexpected_night_services=0`；7 条均为 `night/1440/interval`；未确认 7 条逐行原值不变 |
| 5 | POST-CHECK | `pet_order_columns=68`、`pet_service_columns=15`、`payment_columns=12`；金额汇总与段 0 完全一致；历史订单新增列 `pickup_code/checkout_ts/overtime/overtime_pay` 全为 NULL/0（0 条被回填）；专项订单 `60/61/62/16/42/56` 原值逐字不变 |

## 迁移前后逐条比对（D5）

| 数据集 | 结果 | SHA256 |
|---|---|---|
| `pet_order_wsh`（59 行，含全部金额/优惠/状态字段） | **IDENTICAL** | `86512cefbb5d42eb5f755a7af10510f9394239a71c50bc72992afd199bc16840` |
| `order_snapshot_wsh`（8 行） | **IDENTICAL** | `0c58921b67632d64ec5a263dd3ea3d7d18f9749722d9ef450a2d3463566168cb` |
| `payment_wsh`（46 行） | **IDENTICAL** | `bb9532679771bf191b51a8794becc3067f701eedaad555d30f64922aecb8861b` |
| `pet_service_wsh`（36 行） | 恰好 7 行变化，仅 `unit_wsh: boarding → night`（ID 7/8/20/21/25/26/35）；其余 29 行逐字不变 | before `f84ff28c…` → after `4794d807…` |

**结论**：历史订单、快照、支付记录零改动；未确认服务零改动；白名单服务仅单位字段变化。

## 单位分布（迁移后）

```
pet_service_wsh : night 7 | session 22 | boarding 4 | day 2 | time 1
pet_order_wsh   : day 35 | session 21 | boarding 3   （历史快照，全部原值保留）
```

> `boarding/day/time` 仍存在于**未确认服务**与**历史订单快照**中，符合审批「不自动映射未确认服务」「历史订单原值保留」。可销售口径由业务代码 `BookingUnit.validateUnitSupported()` 保证：非法单位服务无法下单。

## 未执行 / 明确排除

- 未对 `pet_order_wsh` 执行任何 `UPDATE`（历史订单表零数据更新）。
- 未对 `payment_wsh` 执行任何 `UPDATE`（`DEFAULT 'order'` 自动回填，D0 已验证 46 行均为普通预付）。
- 未对未确认服务 `1,2,3,9,29,30,36` 执行任何更新。
- 未加 CHECK 约束（存量非法单位不得为满足约束而被篡改）。
- 未回填历史接走码 / 快照 / `night-v1` 规则版本。
- 未执行 `DROP DATABASE` / `TRUNCATE` / 数据卷重建 / `DROP TABLE` / `DROP COLUMN`。
- 未声称整个迁移具备事务原子性；DDL 逐段执行、逐段核验。

## 遗留兼容性问题（按审批 §三 单独输出，未擅自处理）

1. **历史履约中订单缺接走码**：25 条未完成订单（`in_progress 8` / `received 3` / `delivered 3` / `confirmed 11`）的 `pickup_code_wsh` 为 NULL，`assertPickupCode` 会拒绝 NULL。这是补列前即存在的既有状态（种子数据由 SQL 直接插入），补列未改变其任何字段值。按审批「不能擅自修改历史交易记录」，**未回填**，需业务侧另行确认处理路径。
2. **历史快照缺 `night-v1`**：8 行 `order_snapshot_wsh` 均不含 `pricing_rule_version='night-v1'`。因历史订单单位为 `day/boarding`，`completeOrder` 按 `normalize` 宽松判断不会进入按夜分支，故不会触发 `requireNightPricingRule` 的 500 强校验；但后续若强制历史订单按夜结算会失败。**未补造快照**。
3. **未确认服务**：`1,2,3,9,29,30,36` 保持原单位，列入待业务确认清单，不伪造完成状态。
4. **专项历史订单**：`60/61/62`（boarding 旧口径）、`16`（服务单位与记录歧义）、`42`（券差额已确认）、`56`（负单价异常）全部原值保留，仅标注。
