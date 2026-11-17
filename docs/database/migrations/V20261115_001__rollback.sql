-- ===========================================================================
-- V20261115_001 — 回滚脚本
--
-- 回滚目标：把 V20261115_001__baseline_media_and_ledger.sql 恢复成执行前状态。
--
-- 重要：回滚会删除 pet_service_media_wsh 表，**该表若已有业务数据将一并丢失**。
--       执行前必须先确认表中行数，并重新备份。
--
-- 判定是否可安全回滚：
--   SELECT COUNT(*) FROM pet_service_media_wsh;
--   * 结果为 0  → 可安全 DROP
--   * 结果 > 0  → 说明已有服务图片数据，**禁止 DROP**，只回滚台账行即可
--                 （媒体表是 v6 迁移的正向结构，删表会让代码失去存储位置）
--
-- 若本脚本是在生产库误执行：生产库本来就有这两张表，
--   CREATE TABLE IF NOT EXISTS 未产生任何变更，INSERT IGNORE 也已被忽略，
--   此时**不需要回滚**，请先核验而非直接执行。
-- ===========================================================================

-- ---------- 1) 撤销台账行 ----------
DELETE FROM `migration_ledger_wsh`
WHERE `version_wsh` = 'v6_service_product_media'
  AND `summary_wsh` = '服务产品媒体表与迁移台账基线（基线补齐）';

-- ---------- 2) 删除媒体表（仅在确认为空时手工执行） ----------
-- 默认注释掉：留表不删对启动无影响，删表是不可逆操作，必须人工判断。
-- DROP TABLE IF EXISTS `pet_service_media_wsh`;

-- ---------- 3) 删除台账表（仅当本表由本脚本新建且无其他台账行时手工执行） ----------
-- 默认注释掉：台账表一旦被其他迁移写入，删除会丢失迁移历史。
-- 判定：SELECT COUNT(*) FROM migration_ledger_wsh;
-- DROP TABLE IF EXISTS `migration_ledger_wsh`;
