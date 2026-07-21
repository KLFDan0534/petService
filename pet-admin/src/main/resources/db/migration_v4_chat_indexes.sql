SET @dbname = DATABASE();

SET @exists_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = @dbname
                     AND TABLE_NAME = 'chat_message_wsh'
                     AND INDEX_NAME = 'idx_chat_order_created');
SET @sql_idx = IF(@exists_idx = 0,
    'ALTER TABLE `chat_message_wsh` ADD INDEX `idx_chat_order_created` (`order_id_wsh`, `created_at_wsh`)',
    'SELECT ''index idx_chat_order_created already exists''');
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

SET @exists_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = @dbname
                     AND TABLE_NAME = 'chat_message_wsh'
                     AND INDEX_NAME = 'idx_chat_order_id');
SET @sql_idx = IF(@exists_idx = 0,
    'ALTER TABLE `chat_message_wsh` ADD INDEX `idx_chat_order_id` (`order_id_wsh`, `id_wsh`)',
    'SELECT ''index idx_chat_order_id already exists''');
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

SET @exists_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = @dbname
                     AND TABLE_NAME = 'chat_message_wsh'
                     AND INDEX_NAME = 'idx_chat_to_read');
SET @sql_idx = IF(@exists_idx = 0,
    'ALTER TABLE `chat_message_wsh` ADD INDEX `idx_chat_to_read` (`to_user_id_wsh`, `read_wsh`)',
    'SELECT ''index idx_chat_to_read already exists''');
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

SET @exists_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = @dbname
                     AND TABLE_NAME = 'chat_message_wsh'
                     AND INDEX_NAME = 'idx_chat_order_pair');
SET @sql_idx = IF(@exists_idx = 0,
    'ALTER TABLE `chat_message_wsh` ADD INDEX `idx_chat_order_pair` (`order_id_wsh`, `from_user_id_wsh`, `to_user_id_wsh`)',
    'SELECT ''index idx_chat_order_pair already exists''');
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;
