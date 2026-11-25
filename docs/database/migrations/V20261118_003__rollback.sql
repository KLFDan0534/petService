-- ============================================================================
-- 回滚 V20261118_003：恢复 pet_order_wsh.days_wsh
--
-- 恢复口径（与原写入逻辑一致）：
--   night        → quantity_wsh（预订夜数）
--   session/hour → 1
--   其它历史单位 → 优先 quantity_wsh，缺失时 1
-- ============================================================================

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE pet_order_wsh ADD COLUMN days_wsh INT NOT NULL DEFAULT 1 AFTER end_date_wsh',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pet_order_wsh'
    AND COLUMN_NAME = 'days_wsh');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 恢复口径（与删除前的真实数据逐行一致）：
--   hour         → 1（原值就是 1，quantity 才是槽位数）
--   night/session/其它 → quantity_wsh（原值与 quantity 恒等，含早期 session 多数量单）
UPDATE pet_order_wsh
SET days_wsh = CASE
    WHEN billing_unit_wsh = 'hour' THEN 1
    ELSE COALESCE(quantity_wsh, 1)
  END;

SELECT COUNT(*) AS restored_rows,
       SUM(days_wsh) AS sum_days,
       SUM(CASE WHEN billing_unit_wsh = 'night' AND days_wsh <> quantity_wsh THEN 1 ELSE 0 END) AS night_mismatch
FROM pet_order_wsh;
