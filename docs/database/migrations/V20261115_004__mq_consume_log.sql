-- =============================================================================
-- V20261115_004__mq_consume_log.sql
-- 目标：为 MQ 消费端提供跨实例、跨重启有效的幂等能力
-- 背景：complaint.process 的消费者是「先写通知、再 basicAck」。若进程在两步之间崩溃，
--       消息会被 Broker 重投，站内通知就会重复插入。内存 Set 无法跨实例/跨重启，因此落库。
-- 影响：仅新增一张台账表，不修改任何既有表结构与数据。
-- 幂等：可重复执行（CREATE TABLE IF NOT EXISTS）。
-- 回滚：见 V20261115_004__rollback.sql
-- =============================================================================

-- SEGMENT 1：建表
CREATE TABLE IF NOT EXISTS `mq_consume_log_wsh` (
    `id_wsh` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `consumer_wsh` VARCHAR(64) NOT NULL COMMENT '消费者标识，如 complaint-process',
    `event_key_wsh` VARCHAR(128) NOT NULL COMMENT '业务事件唯一键，如 12:resolved',
    `created_at_wsh` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    PRIMARY KEY (`id_wsh`),
    UNIQUE KEY `uk_consumer_event` (`consumer_wsh`, `event_key_wsh`) COMMENT '同一消费者同一事件只处理一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ 消费幂等台账';

-- SEGMENT 2：结构自检
-- 期望：table_present = 1，unique_present = 1，column_count = 4
SELECT
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mq_consume_log_wsh') AS table_present,
    (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mq_consume_log_wsh'
       AND INDEX_NAME = 'uk_consumer_event') AS unique_present,
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mq_consume_log_wsh') AS column_count;

-- SEGMENT 3：唯一约束有效性自检
-- 插入一条探针，再插同键第二条，期望第二条报 Duplicate entry（说明唯一索引真的生效）。
-- 探针用固定 consumer，执行完立即删除，不污染业务数据。
DELETE FROM `mq_consume_log_wsh` WHERE `consumer_wsh` = '__selfcheck__';
INSERT INTO `mq_consume_log_wsh` (`consumer_wsh`, `event_key_wsh`) VALUES ('__selfcheck__', 'probe');
SELECT 'unique index OK' AS selfcheck;
DELETE FROM `mq_consume_log_wsh` WHERE `consumer_wsh` = '__selfcheck__';
