-- ============================================================================
-- 回滚 V20261119_001：把 wallet_transaction_wsh.type_wsh 收窄回 VARCHAR(20)
--
-- ⚠️ 收窄是有损操作：若已存在长度 > 20 的 type_wsh（例如 keeper_invalid_subsidy、
--    coupon_subsidy_refund），MySQL 严格模式下 ALTER 会直接失败，非严格模式下会截断。
--    因此本脚本先做长度检查，只有 MAX(CHAR_LENGTH) <= 20 时才执行收窄。
-- ============================================================================

SELECT COALESCE(MAX(CHAR_LENGTH(type_wsh)), 0) AS max_len,
       SUM(CHAR_LENGTH(type_wsh) > 20) AS rows_over_20
FROM wallet_transaction_wsh;

SET @s := (SELECT IF(COALESCE(MAX(CHARACTER_MAXIMUM_LENGTH), 0) > 20
                     AND (SELECT COALESCE(MAX(CHAR_LENGTH(type_wsh)), 0) FROM wallet_transaction_wsh) <= 20,
  'ALTER TABLE wallet_transaction_wsh MODIFY COLUMN type_wsh VARCHAR(20) DEFAULT ''income'' COMMENT ''类型: income-收入 expense-支出 withdraw-提现 refund-退款''',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wallet_transaction_wsh'
    AND COLUMN_NAME = 'type_wsh');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COLUMN_TYPE AS current_type
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_transaction_wsh'
  AND COLUMN_NAME = 'type_wsh';
