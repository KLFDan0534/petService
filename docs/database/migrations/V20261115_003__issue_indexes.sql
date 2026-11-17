-- =============================================================================
-- V20261115_003 · 投诉与工单查询索引
--
-- 背景（实测）：
--   complaint_wsh        只有主键，没有任何二级索引
--   ticket_wsh           只有主键，没有任何二级索引
--   complaint_message_wsh 有 idx_cm_complaint / 线上叫 idx_complaint_id
--   ticket_message_wsh   没有任何索引 → 每次消息列表都全表扫描
--
-- 设计依据（逐条对应真实查询）：
--   complaint_wsh
--     ① listByOwner          WHERE owner_id_wsh=? ORDER BY created_at_wsh DESC
--     ② listPageForStaff     WHERE merchant_id_wsh IN (...) [AND status_wsh=?] ORDER BY created_at_wsh DESC
--     ③ assertCanComplain    COUNT WHERE owner_id_wsh=? AND created_at_wsh > ?
--                            COUNT WHERE owner_id_wsh=? AND merchant_id_wsh=? AND status_wsh IN (...)
--     → (owner_id_wsh, created_at_wsh) 同时服务 ① 与 ③ 的限流计数
--     → (merchant_id_wsh, created_at_wsh) 服务 ②：等值/IN 前导列 + 排序列，避免 filesort
--       status_wsh 作为残余过滤（单商家数据量小，不再单独建 (merchant,status) 索引，
--       否则每个索引都要多一份写入成本）
--
--   ticket_wsh
--     ① listByUser           WHERE user_id_wsh=? ORDER BY created_at_wsh DESC
--     ② listPageForStaff     WHERE merchant_id_wsh IN (...) ORDER BY created_at_wsh DESC
--     ③ 工作台统计           COUNT WHERE status_wsh=? AND assignee_id_wsh=?
--     → 三个索引分别对应上面三条，互不重复
--
--   ticket_message_wsh
--     ① listMessages         WHERE ticket_id_wsh=? ORDER BY created_at_wsh ASC
--     ② selectLatestBy...    WHERE ticket_id_wsh IN (...)
--     ③ 未读聚合             WHERE is_read_wsh=0 AND from/user_id<>? AND ticket_id_wsh IN (...)
--     → (ticket_id_wsh, created_at_wsh)：前导列服务 IN/等值，第二列服务消息列表排序
--
--   complaint_message_wsh
--     **不新增索引**：已有 (complaint_id_wsh) 足以覆盖全部按投诉聚合的查询；
--     再加一个同前缀复合索引只会多占存储与写入成本（重复前缀）。
--
-- 幂等：全部通过 information_schema 判存在再建，重复执行安全。
-- 回滚：V20261115_003__rollback.sql
-- =============================================================================

DROP PROCEDURE IF EXISTS `tmp_issue_add_index`;

DELIMITER $$
CREATE PROCEDURE `tmp_issue_add_index`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('created index ', p_index, ' on ', p_table) AS step_result;
    ELSE
        SELECT CONCAT('index ', p_index, ' already exists on ', p_table) AS step_result;
    END IF;
END$$
DELIMITER ;

-- 投诉：用户端列表 / 限流计数
CALL `tmp_issue_add_index`('complaint_wsh', 'idx_complaint_owner_created', '`owner_id_wsh`,`created_at_wsh`');
-- 投诉：后台列表（商家范围 + 按创建时间倒序）
CALL `tmp_issue_add_index`('complaint_wsh', 'idx_complaint_merchant_created', '`merchant_id_wsh`,`created_at_wsh`');

-- 工单：用户端列表
CALL `tmp_issue_add_index`('ticket_wsh', 'idx_ticket_user_created', '`user_id_wsh`,`created_at_wsh`');
-- 工单：后台列表
CALL `tmp_issue_add_index`('ticket_wsh', 'idx_ticket_merchant_created', '`merchant_id_wsh`,`created_at_wsh`');
-- 工单：工作台「我处理中」统计
CALL `tmp_issue_add_index`('ticket_wsh', 'idx_ticket_assignee_status', '`assignee_id_wsh`,`status_wsh`');

-- 工单留言：消息列表 / 最新留言 / 未读聚合（原表零索引）
CALL `tmp_issue_add_index`('ticket_message_wsh', 'idx_ticket_msg_ticket', '`ticket_id_wsh`,`created_at_wsh`');

DROP PROCEDURE IF EXISTS `tmp_issue_add_index`;

-- =============================================================================
-- 执行后自检：应看到 5 个新索引
-- =============================================================================
SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns_in_order
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('complaint_wsh', 'ticket_wsh', 'complaint_message_wsh', 'ticket_message_wsh')
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
