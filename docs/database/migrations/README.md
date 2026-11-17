# 数据库迁移脚本目录说明

本目录是 PetService **正式数据库迁移脚本**的唯一存放位置（阶段 D 审批 §六.10）。

## 当前正式迁移

| 文件 | 版本 | 状态 |
|---|---|---|
| `V20261110_001__pricing_unit_unify.sql` | V20261110_001 | 计价体系统一治理：补 6 个字段、去 `DEFAULT 'day'`/`'date_range'`、7 条白名单服务 `boarding → night` |
| `V20261110_001__rollback.sql` | V20261110_001（回滚） | 逐段回滚；DDL 部分需人工审批，非安全自动回滚 |
| `V20261110_001__execution-log.md` | V20261110_001 | 执行证据：备份、基线、逐段核验、迁移前后逐条 diff |

执行纪律（脚本头已声明）：MySQL DDL 隐式提交，**整个迁移不具备事务原子性**；必须逐段执行、逐段核验；执行前必须备份并验证备份可读取；历史订单表零 UPDATE。

## 正式初始化 SQL（不在本目录）

| 文件 | 用途 |
|---|---|
| `pet-admin/src/main/resources/db/schema.sql` | 唯一被 compose 挂载为 `01-schema.sql` 的建表脚本 |
| `pet-admin/src/main/resources/db/seed.sql` | 种子数据（未挂载，`spring.sql.init.mode=never`，需手工执行） |
| `pet-admin/src/main/resources/h2-schema.sql` | H2 测试库建表 |
| `docs/database/complete_schema.sql` | 权威结构基线（52 表，已合并按夜迁移列） |

四份已在阶段 D 同步：均无 `DEFAULT 'day'`/`'date_range'`、COMMENT 为 `night/session/hour` 口径、seed 服务单位显式使用 `night/session` 并带 `duration_minutes_wsh`/`booking_mode_wsh`。

## 历史归档脚本（**禁止作为当前初始化方案、禁止执行**）

以下文件位于 `.workbuddy-ai/tmp/dbaudit/backup_deleted/`，为审计中间产物/历史归档，**不属于当前初始化流程**，compose 与 `spring.sql.init` 均未引用：

| 归档文件 | 状态 | 风险 |
|---|---|---|
| `docs/2026-11-03-boarding-unit-migration.sql` | 历史归档，禁止执行 | 会把 `unit_wsh` 从 `天/day` 改成旧单位 `boarding`，回滚段还会改回 `day` |
| `docs/2026-11-09-night-pricing-checkout-migration.sql` | 历史归档，内容正确但已废弃 | 头部注明「本地演示库增量迁移、不适用于线上历史库」；当前正式版本为 `V20261110_001__pricing_unit_unify.sql` |
| `docs/2026-11-03-merge-delivered-into-received.sql`、`2026-11-03-order-keeper-nullable.sql`、`2026-11-05-drop-keeper-price.sql` | 历史归档 | 与计价单位无关，勿作为当前脚本 |
| `database/full-schema.sql` | 历史归档，禁止执行 | `unit_wsh DEFAULT '次'`，INSERT 全为 `'day'`，会重建旧口径全库 |
| `database/fix-missing-tables.sql`、`database/seed-missing-tables.sql` | 历史临时脚本 | 后者 INSERT `'day'` |
| `database/complete_schema.sql`（归档版） | 历史归档 | 未合并按夜迁移列、注释乱码 |
| `pet-admin/target/classes/db/{schema,seed}.sql`、`h2-schema.sql`（归档副本） | 编译产物副本 | 归档附带，非源 |

另 `.workbuddy-ai/tmp/dbaudit/live_schema_dump.sql` 为实库只读快照（审计中间产物），`*.tsv` / `diff_*.txt` 为结构比对中间产物，均不得作为初始化方案。

## 待业务确认项（不伪造完成状态）

服务 `1, 2, 3, 9, 29, 30, 36` 与历史订单 `60, 61, 62, 16, 42, 56` 按阶段 D 审批保留原值，列入异常清单；未自动映射、未回填快照。详见 `docs/audit/pricing-system-final-report.md`。
