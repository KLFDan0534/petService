-- =============================================================================
-- V20261115_003 · 投诉与工单查询索引 · 回滚
--
-- 只删除本次新增的 5 个索引，不动任何既有索引（idx_cm_complaint / idx_complaint_id）。
-- 幂等：索引不存在时静默跳过。
-- 注意：DROP INDEX 只影响查询计划，不影响任何数据。
-- =============================================================================

DROP PROCEDURE IF EXISTS `tmp_issue_drop_index`;

DELIMITER $$
CREATE PROCEDURE `tmp_issue_drop_index`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('dropped index ', p_index, ' on ', p_table) AS step_result;
    ELSE
        SELECT CONCAT('index ', p_index, ' not present on ', p_table) AS step_result;
    END IF;
END$$
DELIMITER ;

CALL `tmp_issue_drop_index`('complaint_wsh', 'idx_complaint_owner_created');
CALL `tmp_issue_drop_index`('complaint_wsh', 'idx_complaint_merchant_created');
CALL `tmp_issue_drop_index`('ticket_wsh', 'idx_ticket_user_created');
CALL `tmp_issue_drop_index`('ticket_wsh', 'idx_ticket_merchant_created');
CALL `tmp_issue_drop_index`('ticket_wsh', 'idx_ticket_assignee_status');
CALL `tmp_issue_drop_index`('ticket_message_wsh', 'idx_ticket_msg_ticket');

DROP PROCEDURE IF EXISTS `tmp_issue_drop_index`;

-- 回滚后自检：这四张表上不应再出现本次新增的索引名
SELECT TABLE_NAME, INDEX_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN ('idx_complaint_owner_created', 'idx_complaint_merchant_created',
                     'idx_ticket_user_created', 'idx_ticket_merchant_created',
                     'idx_ticket_assignee_status', 'idx_ticket_msg_ticket');
