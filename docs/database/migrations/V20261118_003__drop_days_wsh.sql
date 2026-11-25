-- ============================================================================
-- 迁移 V20261118_003：删除 pet_order_wsh.days_wsh
-- 目标库：pet_service
--
-- 背景：
--   days_wsh 是 quantity_wsh 的展示投影，不是独立业务事实：
--     night  → days = quantity（预订夜数）
--     session/hour → days = 1
--   多计费单位落地后它已不参与任何计价/容量计算，仅被 DTO 与 AI 报告文案读取，
--   且这两处按"计费数量"展示时对 hour 订单是错的（quantity=3 小时却显示 1）。
--   删除后统一以 quantity_wsh 为准。
--
-- 影响面（已核对）：
--   PetOrder / OrderDTO / OrderQuoteDTO / OrderServiceImpl（含 Quote.daysDisplay）
--   / OrderSnapshotServiceImpl / OrderFulfillmentServiceImpl / AiReportServiceImpl
--   / schema.sql / h2-schema.sql / seed.sql / complete_schema.sql / api 文档 / 单测
--
-- 回滚：V20261118_003__rollback.sql
--
-- ⚠️ 发布顺序（必须遵守）：
--   1) 先执行本迁移；2) 再发布新代码。
--   原列是 NOT NULL 且无默认值，先发代码（INSERT 不再写该列）会直接报
--   "Field 'days_wsh' doesn't have a default value"，下单全链路失败。
--   若已发代码但未跑迁移，临时执行：
--     ALTER TABLE pet_order_wsh MODIFY COLUMN days_wsh INT NULL DEFAULT 1;
-- ============================================================================

SELECT '=== 0. 前置基线（迁移前记录）===' AS section;
SELECT COUNT(*) AS order_rows,
       SUM(total_amount_wsh) AS sum_total,
       SUM(final_amount_wsh) AS sum_final,
       SUM(COALESCE(settlement_amount_wsh, 0)) AS sum_settlement
FROM pet_order_wsh;

-- 校验：days_wsh 是否真等于 quantity_wsh（不一致的行只允许出现在 hour 单位上）
-- 说明：night / session 的历史数据 days 恒等于 quantity；hour 的 days 恒为 1、
--       quantity 才是槽位数。首次执行时发现早期 session 演示单也是 days=quantity，
--       因此这里改为与 quantity 直接比对，不再假设 session=1。
SELECT COUNT(*) AS days_ne_quantity_rows
FROM pet_order_wsh
WHERE days_wsh IS NOT NULL
  AND days_wsh <> COALESCE(quantity_wsh, days_wsh);

-- 校验：按单位分组看差异分布（期望只有 hour 有差异，且差异值恒为 days=1）
SELECT billing_unit_wsh,
       COUNT(*) AS cnt,
       SUM(days_wsh <> COALESCE(quantity_wsh, days_wsh)) AS days_ne_quantity,
       SUM(CASE WHEN days_wsh <> COALESCE(quantity_wsh, days_wsh) AND days_wsh <> 1 THEN 1 ELSE 0 END)
           AS unexpected_diff_rows
FROM pet_order_wsh
GROUP BY billing_unit_wsh;

-- ---------------------------------------------------------------------------
-- 1. 删除列（幂等）
-- ---------------------------------------------------------------------------
SET @s := (SELECT IF(COUNT(*) > 0,
  'ALTER TABLE pet_order_wsh DROP COLUMN days_wsh',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pet_order_wsh'
    AND COLUMN_NAME = 'days_wsh');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. 后置校验
-- ---------------------------------------------------------------------------
SELECT '=== 2. 后置校验 ===' AS section;
SELECT COUNT(*) AS column_still_exists             -- 期望 0
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'pet_order_wsh'
  AND COLUMN_NAME = 'days_wsh';

SELECT COUNT(*) AS order_rows,
       SUM(total_amount_wsh) AS sum_total,
       SUM(final_amount_wsh) AS sum_final,
       SUM(COALESCE(settlement_amount_wsh, 0)) AS sum_settlement
FROM pet_order_wsh;                                -- 必须与步骤 0 完全一致

SELECT COUNT(*) AS quantity_null_rows              -- 期望 0：展示口径仍可从 quantity 还原
FROM pet_order_wsh WHERE quantity_wsh IS NULL;
