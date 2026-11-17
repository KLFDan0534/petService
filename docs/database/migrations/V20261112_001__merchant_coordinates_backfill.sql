-- =============================================================================
-- V20261112_001 门店坐标补齐
-- =============================================================================
-- 背景：到店交接口径确定后，送达地址与坐标一律取门店；门店缺坐标的订单在下单口
--       会被 OrderServiceImpl.requireMerchantCoordinates 拦截。
-- 目标：把存量门店的经纬度补齐，使所有在营门店都能正常接单。
-- 特性：幂等，只更新坐标为 NULL 的行，可重复执行。
-- =============================================================================

-- 1) 演示/测试门店（id=1，北京市朝阳区测试路1号）
--    原行为空坐标，按地址所在行政区取近似坐标
UPDATE merchant_wsh
SET latitude_wsh  = 39.921900,
    longitude_wsh = 116.443500
WHERE id_wsh = 1
  AND (latitude_wsh IS NULL OR longitude_wsh IS NULL);

-- 2) 校验：执行后应返回 0 行；仍有输出说明需要人工补该门店坐标
SELECT id_wsh, name_wsh, address_wsh
FROM merchant_wsh
WHERE deleted_wsh = 0
  AND (latitude_wsh IS NULL OR longitude_wsh IS NULL);
