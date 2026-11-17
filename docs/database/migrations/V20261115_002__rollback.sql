-- ============================================================================
-- V20261115_002__rollback.sql
-- Version : V20261115_002（回滚）
-- Date    : 2026-11-15
--
-- 回滚目标：把 V20261115_002__agent_session.sql 恢复成执行前状态。
--
-- !! 危险：DROP TABLE 会连同会话数据一起删除，不可逆。!!
--
-- 执行前必须完成：
--   1. 备份：mysqldump --single-transaction pet_service > backup_$(date +%Y%m%d-%H%M).sql
--   2. 记录行数：SELECT COUNT(*) FROM agent_session_wsh;
--   3. 判定：
--        * 行数 = 0  → 可安全 DROP（表是空的，没有业务数据损失）
--        * 行数 > 0  → **禁止 DROP**。此时只能撤销台账，保留表；
--                      表里有会话数据说明功能已在用，删表等于删用户会话。
--
-- 若本脚本是在「本来就没有该表」的库上误执行：
--   下面的 DROP 是 IF EXISTS，不会报错，也不会产生任何变更。
-- ============================================================================

SELECT '=== ROLLBACK V20261115_002 ===' AS section;

-- ---------- 0) 先看行数，人工确认后再往下执行 ----------
SELECT COUNT(*) AS row_count_before_drop
FROM `agent_session_wsh`;

-- ---------- 1) 撤销台账（安全，无数据影响） ----------
DELETE FROM `migration_ledger_wsh`
WHERE `version_wsh` = 'V20261115_002_agent_session';

-- ---------- 2) 删除表（默认注释，仅在确认为空时手工放开） ----------
-- 放开前必须满足：上面 row_count_before_drop = 0
-- DROP TABLE IF EXISTS `agent_session_wsh`;

SELECT '=== 台账已撤销；是否删表由人工决定（见脚本内注释）===' AS section;

SELECT COUNT(*) AS ledger_after
FROM `migration_ledger_wsh`
WHERE `version_wsh` = 'V20261115_002_agent_session';
