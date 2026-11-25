-- ============================================================================
-- 迁移 V20261118_001：删除 pet_order_wsh.price_per_day_wsh
-- 目标库：pet_service
--
-- 背景：
--   该列是"每日价格"时代的兼容投影，值恒等于 unit_price_wsh（下单时由服务端
--   直接赋同一个变量），不参与任何计价、结算、退款或容量计算。
--   多计费单位（night/session/hour）落地后，单价语义统一由
--   billing_unit_wsh + quantity_wsh + unit_price_wsh + duration_minutes_wsh 表达，
--   该列已无读取方（前端零引用，后端仅被拷进 DTO 与 price_snapshot JSON）。
--
-- 影响面（已核对）：
--   PetOrder / OrderDTO / OrderServiceImpl.doCreateOrder / OrderSnapshotServiceImpl
--   / schema.sql / h2-schema.sql / seed.sql / complete_schema.sql / api 文档
--   ⚠️ keeper_wsh.price_per_day_wsh 是另一个业务字段（看护者日薪），本迁移不涉及。
--
-- 回滚：V20261118_001__rollback.sql
--
-- ⚠️ 发布顺序（必须遵守）：
--   1) 先在目标库执行本迁移（删列）；
--   2) 再发布不包含该字段的新代码。
--   原列是 NOT NULL 且无默认值，若先发代码（INSERT 不再写该列）会直接报
--   "Field 'price_per_day_wsh' doesn't have a default value"，下单全链路失败。
--   若已发代码但未跑迁移，请立刻执行本文件，或临时执行：
--     ALTER TABLE pet_order_wsh MODIFY COLUMN price_per_day_wsh DECIMAL(10,2) NULL DEFAULT NULL;
-- ============================================================================

SELECT '=== 0. 前置基线（迁移前必须记录）===' AS section;
SELECT COUNT(*) AS order_rows,
       SUM(total_amount_wsh) AS sum_total,
       SUM(final_amount_wsh) AS sum_final,
       SUM(COALESCE(settlement_amount_wsh, 0)) AS sum_settlement
FROM pet_order_wsh;

-- 校验：该列与 unit_price_wsh 是否真的一致（不一致说明有历史脏数据，需人工确认）
SELECT COUNT(*) AS mismatch_rows
FROM pet_order_wsh
WHERE price_per_day_wsh IS NOT NULL
  AND (unit_price_wsh IS NULL OR price_per_day_wsh <> unit_price_wsh);

-- 校验：是否还有业务方写入该列（期望为 0 行）
SELECT COUNT(*) AS non_equal_projection_rows
FROM pet_order_wsh
WHERE price_per_day_wsh IS NOT NULL AND price_per_day_wsh = 0;

-- ---------------------------------------------------------------------------
-- 1. 删除列（幂等：列不存在时跳过）
-- ---------------------------------------------------------------------------
SET @s := (SELECT IF(COUNT(*) > 0,
  'ALTER TABLE pet_order_wsh DROP COLUMN price_per_day_wsh',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pet_order_wsh'
    AND COLUMN_NAME = 'price_per_day_wsh');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. 后置校验
-- ---------------------------------------------------------------------------
SELECT '=== 2. 后置校验 ===' AS section;
SELECT COUNT(*) AS column_still_exists            -- 期望 0
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'pet_order_wsh'
  AND COLUMN_NAME = 'price_per_day_wsh';

SELECT COUNT(*) AS order_rows,
       SUM(total_amount_wsh) AS sum_total,
       SUM(final_amount_wsh) AS sum_final,
       SUM(COALESCE(settlement_amount_wsh, 0)) AS sum_settlement
FROM pet_order_wsh;                               -- 必须与步骤 0 完全一致

SELECT COUNT(*) AS unit_price_null_rows           -- 期望 0：删除投影后单价仍完整
FROM pet_order_wsh WHERE unit_price_wsh IS NULL;
