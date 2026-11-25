-- ============================================================================
-- 回滚 V20261118_001：恢复 pet_order_wsh.price_per_day_wsh
--
-- 恢复值来源：unit_price_wsh（原列的写入逻辑就是 `price_per_day_wsh = unit_price`）。
-- 若 unit_price_wsh 为 NULL，回填 0.00 以满足原列 NOT NULL 约束。
-- 注意：回滚只能恢复"可推导"的值；若期间有历史行的 unit_price 被修正过，
--       恢复后的投影与原始值可能不同，但对账口径以 unit_price_wsh 为准。
-- ============================================================================

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE pet_order_wsh ADD COLUMN price_per_day_wsh DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''兼容投影，= unit_price_wsh'' AFTER days_wsh',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pet_order_wsh'
    AND COLUMN_NAME = 'price_per_day_wsh');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE pet_order_wsh
SET price_per_day_wsh = COALESCE(unit_price_wsh, 0.00)
WHERE price_per_day_wsh = 0.00;

SELECT COUNT(*) AS restored_rows,
       SUM(price_per_day_wsh) AS sum_price_per_day
FROM pet_order_wsh;
