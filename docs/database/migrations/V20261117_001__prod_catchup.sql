-- ============================================================================
-- 生产库补齐迁移 2026-11-17
-- 目标：pet_service @ pet-mysql
-- 依据：V20261110_001/002、V20261112_001、V20261115_002/004（已审批口径）
-- 原则：只做加法（新表/新列）+ 已审批白名单 DML；不改历史订单、不改价格、不删表
-- ============================================================================

SELECT '=== 0. 前置基线 ===' AS section;
SELECT (SELECT COUNT(*) FROM pet_order_wsh) AS orders,
       (SELECT SUM(final_amount_wsh) FROM pet_order_wsh) AS sum_final,
       (SELECT COUNT(*) FROM payment_wsh) AS payments,
       (SELECT COUNT(*) FROM information_schema.tables
         WHERE table_schema=DATABASE()) AS tables_cnt;

-- ---------------------------------------------------------------------------
-- 1. agent_session_wsh（V20261115_002 权威定义，与 schema.sql 逐列一致）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_session_wsh` (
    `session_id_wsh` VARCHAR(64) NOT NULL COMMENT '会话ID（复用 ai_chat_history_wsh.session_id_wsh）',
    `user_id_wsh` BIGINT NOT NULL COMMENT '用户ID',
    `title_wsh` VARCHAR(200) COMMENT '会话标题（首条用户输入截断）',
    `state_json_wsh` TEXT COMMENT '结构化会话状态 JSON',
    `status_wsh` VARCHAR(20) NOT NULL DEFAULT 'COLLECTING' COMMENT 'COLLECTING/AWAIT_CONFIRM/CONFIRMED',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`session_id_wsh`),
    INDEX `idx_agent_session_user` (`user_id_wsh`, `updated_at_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能下单会话状态';

-- ---------------------------------------------------------------------------
-- 2. mq_consume_log_wsh（V20261115_004）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mq_consume_log_wsh` (
    `id_wsh` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `consumer_wsh` VARCHAR(64) NOT NULL COMMENT '消费者标识，如 complaint-process',
    `event_key_wsh` VARCHAR(128) NOT NULL COMMENT '业务事件唯一键，如 12:resolved',
    `created_at_wsh` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    PRIMARY KEY (`id_wsh`),
    UNIQUE KEY `uk_consumer_event` (`consumer_wsh`, `event_key_wsh`) COMMENT '同一消费者同一事件只处理一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ 消费幂等台账';

-- ---------------------------------------------------------------------------
-- 3. payment_wsh.purpose_wsh
-- ---------------------------------------------------------------------------
SET @s := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE payment_wsh ADD COLUMN purpose_wsh VARCHAR(32) NOT NULL DEFAULT ''order'' COMMENT ''order=原预付；night_overtime=晚接补费''',
  'SELECT 0') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='payment_wsh' AND column_name='purpose_wsh');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 4. pet_order_wsh 5 个新列（逐列幂等）
-- ---------------------------------------------------------------------------
SET @s := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE pet_order_wsh ADD COLUMN pickup_code_wsh VARCHAR(4) COMMENT ''独立接走交接码，与送达码不同，字符串保留前导0''',
  'SELECT 0') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='pet_order_wsh' AND column_name='pickup_code_wsh');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE pet_order_wsh ADD COLUMN checkout_confirmed_at_wsh DATETIME NULL COMMENT ''首次合法接走确认的服务端时间T，冻结后不修改''',
  'SELECT 0') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='pet_order_wsh' AND column_name='checkout_confirmed_at_wsh');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE pet_order_wsh ADD COLUMN overtime_hours_wsh INT NOT NULL DEFAULT 0 COMMENT ''实际晚接小时数H（非预订夜数、非预约估算）''',
  'SELECT 0') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='pet_order_wsh' AND column_name='overtime_hours_wsh');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE pet_order_wsh ADD COLUMN overtime_amount_wsh DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT ''冻结的晚接补费金额''',
  'SELECT 0') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='pet_order_wsh' AND column_name='overtime_amount_wsh');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE pet_order_wsh ADD COLUMN overtime_payment_id_wsh BIGINT NULL COMMENT ''唯一一笔晚接补费支付记录ID；0费时为NULL''',
  'SELECT 0') FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='pet_order_wsh' AND column_name='overtime_payment_id_wsh');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 5. 服务计费单位映射（严格白名单 + 旧值断言；29/36 按审批保持原值不动）
--    1,2        基础/豪华寄养  day/1440/date_range -> night/interval/1440
--    7,8,20,21,25,26,35 寄养类 day/1440/date_range -> night/interval/1440
--    9,30       日间寄养      day/1440/date_range -> session/slot/540
--    3          上门遛狗      time/30/time_slot    -> session/slot/30
-- ---------------------------------------------------------------------------
SELECT '=== 5. 迁移前服务单位分布 ===' AS section;
SELECT unit_wsh, IFNULL(booking_mode_wsh,'<NULL>') AS mode, COUNT(*) AS cnt
FROM pet_service_wsh GROUP BY unit_wsh, booking_mode_wsh ORDER BY unit_wsh;

START TRANSACTION;

UPDATE pet_service_wsh
   SET unit_wsh='night', booking_mode_wsh='interval', duration_minutes_wsh=1440
 WHERE id_wsh IN (1,2,7,8,20,21,25,26,35)
   AND unit_wsh='day' AND duration_minutes_wsh=1440 AND booking_mode_wsh='date_range';
SELECT ROW_COUNT() AS affected_night;   -- 期望 9

UPDATE pet_service_wsh
   SET unit_wsh='session', booking_mode_wsh='slot', duration_minutes_wsh=540
 WHERE id_wsh IN (9,30)
   AND unit_wsh='day' AND duration_minutes_wsh=1440 AND booking_mode_wsh='date_range';
SELECT ROW_COUNT() AS affected_daycare; -- 期望 2

UPDATE pet_service_wsh
   SET unit_wsh='session', booking_mode_wsh='slot', duration_minutes_wsh=30
 WHERE id_wsh=3
   AND unit_wsh='time' AND duration_minutes_wsh=30 AND booking_mode_wsh='time_slot';
SELECT ROW_COUNT() AS affected_walk;    -- 期望 1

COMMIT;

-- ---------------------------------------------------------------------------
-- 6. 门店坐标补齐（V20261112_001：仅补 NULL 行）
-- ---------------------------------------------------------------------------
UPDATE merchant_wsh
   SET latitude_wsh=39.921900, longitude_wsh=116.443500
 WHERE id_wsh=1 AND (latitude_wsh IS NULL OR longitude_wsh IS NULL);
SELECT ROW_COUNT() AS affected_merchant_coords;

-- ---------------------------------------------------------------------------
-- 7. 迁移台账登记（可追溯）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO migration_ledger_wsh (version_wsh, applied_at_wsh, summary_wsh) VALUES
 ('V20261110_001_pricing_unit_unify', NOW(), '生产补齐：pet_order 5列 + payment.purpose_wsh；单位白名单映射'),
 ('V20261112_001_merchant_coords',    NOW(), '门店1坐标补齐'),
 ('V20261115_002_agent_session',      NOW(), '建 agent_session_wsh'),
 ('V20261115_004_mq_consume_log',     NOW(), '建 mq_consume_log_wsh');

-- ---------------------------------------------------------------------------
-- 8. 迁移后核验
-- ---------------------------------------------------------------------------
SELECT '=== 8. 核验 ===' AS section;
SELECT unit_wsh, IFNULL(booking_mode_wsh,'<NULL>') AS mode, COUNT(*) AS cnt
FROM pet_service_wsh GROUP BY unit_wsh, booking_mode_wsh ORDER BY unit_wsh;

SELECT id_wsh, unit_wsh, duration_minutes_wsh, booking_mode_wsh, price_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (1,2,3,9,29,30,36) ORDER BY id_wsh;

SELECT (SELECT COUNT(*) FROM pet_order_wsh) AS orders,
       (SELECT SUM(final_amount_wsh) FROM pet_order_wsh) AS sum_final,
       (SELECT COUNT(*) FROM payment_wsh) AS payments,
       (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()) AS tables_cnt;

SELECT table_name, COUNT(*) AS cols FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name IN ('agent_session_wsh','mq_consume_log_wsh','payment_wsh','pet_order_wsh')
GROUP BY table_name;
