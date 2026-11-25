-- =============================================================================
-- V20261118_002 · 回滚脚本
--
-- 对应迁移：V20261118_002__order_indexes.sql
-- 作用：删除本次新增的 7 个业务索引，恢复到只有 PRIMARY + order_no_wsh 的状态。
--
-- 安全性：DROP INDEX 只删索引不删数据，回滚无数据风险；但会导致相关查询
--         退回全表扫描，请在确认新索引确实引发问题（如写入性能明显下降）
--         后再执行。
-- 幂等：索引不存在时跳过，重复执行安全。
-- =============================================================================

DROP PROCEDURE IF EXISTS `tmp_order_drop_index`;

DELIMITER $$
CREATE PROCEDURE `tmp_order_drop_index`(
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
        SELECT CONCAT('index ', p_index, ' not exists on ', p_table, ' (skipped)') AS step_result;
    END IF;
END$$
DELIMITER ;

CALL `tmp_order_drop_index`('pet_order_wsh', 'idx_order_owner_created');
CALL `tmp_order_drop_index`('pet_order_wsh', 'idx_order_merchant_created');
CALL `tmp_order_drop_index`('pet_order_wsh', 'idx_order_keeper_created');
CALL `tmp_order_drop_index`('pet_order_wsh', 'idx_order_status_created');
CALL `tmp_order_drop_index`('pet_order_wsh', 'idx_order_status_updated');
CALL `tmp_order_drop_index`('pet_order_wsh', 'idx_order_pet_date');
CALL `tmp_order_drop_index`('pet_order_wsh', 'idx_order_keeper_date');

DROP PROCEDURE IF EXISTS `tmp_order_drop_index`;

-- =============================================================================
-- 回滚后自检：应只剩 PRIMARY 与 order_no_wsh
-- =============================================================================
SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns_in_order
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'pet_order_wsh'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY INDEX_NAME;
