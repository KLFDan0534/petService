-- =============================================================================
-- V20261115_004__rollback.sql
-- 回滚 V20261115_004__mq_consume_log.sql
--
-- 注意：这张表只存「哪条事件已经处理过」，不含业务数据。删除它不会丢任何业务记录，
-- 但会让消费端暂时失去幂等保护（重复投递会再次产生通知）。
-- 因此回滚前请先确认：消费者代码是否已同步回退到无幂等版本。
-- =============================================================================

-- SEGMENT 1：报告当前台账规模（供回滚留痕）
SELECT
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mq_consume_log_wsh') AS table_present,
    (SELECT COUNT(*) FROM `mq_consume_log_wsh`) AS row_count_before_drop;

-- SEGMENT 2：删表（IF EXISTS，可重复执行）
DROP TABLE IF EXISTS `mq_consume_log_wsh`;

-- SEGMENT 3：自检，期望 table_present = 0
SELECT
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mq_consume_log_wsh') AS table_present;
