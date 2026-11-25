-- ============================================================================
-- 迁移 V20261119_001：加宽 wallet_transaction_wsh.type_wsh（20 → 32）
-- 目标库：pet_service
--
-- 背景：
--   定时任务「接单超时自动分配」在负责人不可用时走
--   OrderServiceImpl.cancelPaidOrderForInvalidKeeper → refundPaidOrder
--   → accountingService.debit(..., reason + "_subsidy", ...)，其中 reason="keeper_invalid"，
--   拼出 type_wsh = "keeper_invalid_subsidy"（22 字符），超过原列宽 VARCHAR(20)，
--   MySQL 严格模式抛 Data too long → 整个退款事务回滚，订单停在 cancelled 但钱没退。
--
-- 全量 type_wsh 取值清点（代码中的字面量）：
--   recharge(8) / admin_adjust(12) / tip(3) / withdraw(8) / membership_payment(18)
--   payment(7) / coupon_subsidy(14) / coupon_subsidy_refund(21) / keeper_invalid(14)
--   keeper_invalid_subsidy(22) / unassigned(10) / unassigned_subsidy(19)
--   reject(6) / reject_subsidy(14) / income / expense
--   → 最长 22；其中 coupon_subsidy_refund(21) 同样是原列宽下的潜在失败点（退款回冲补贴）。
--
-- 选择 32 而不是 50：留 10 字符余量，同时避免把类型列做成无界文本（仍可加索引）。
--
-- 回滚：V20261119_001__rollback.sql（先校验无超长值再收窄）
-- ============================================================================

SELECT '=== 0. 前置基线 ===' AS section;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_transaction_wsh'
  AND COLUMN_NAME = 'type_wsh';

SELECT type_wsh, CHAR_LENGTH(type_wsh) AS len, COUNT(*) AS cnt
FROM wallet_transaction_wsh
GROUP BY type_wsh
ORDER BY len DESC;

SELECT COUNT(*) AS rows_over_20, COALESCE(MAX(CHAR_LENGTH(type_wsh)), 0) AS max_len
FROM wallet_transaction_wsh;

-- ---------------------------------------------------------------------------
-- 1. 加宽（幂等：已是 >=32 则跳过）
-- ---------------------------------------------------------------------------
SET @s := (SELECT IF(COALESCE(MAX(CHARACTER_MAXIMUM_LENGTH), 0) < 32,
  'ALTER TABLE wallet_transaction_wsh MODIFY COLUMN type_wsh VARCHAR(32) DEFAULT ''income'' COMMENT ''流水类型: income/expense/withdraw/refund/payment/recharge/tip/membership_payment/admin_adjust/coupon_subsidy*/{reason}_subsidy''',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wallet_transaction_wsh'
    AND COLUMN_NAME = 'type_wsh');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. 后置校验
-- ---------------------------------------------------------------------------
SELECT '=== 2. 后置校验 ===' AS section;
SELECT COLUMN_TYPE AS new_type
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wallet_transaction_wsh'
  AND COLUMN_NAME = 'type_wsh';                       -- 期望 varchar(32)

SELECT COUNT(*) AS tx_rows,
       SUM(amount_wsh) AS sum_amount
FROM wallet_transaction_wsh;                          -- 行数与金额必须与步骤 0 一致
