-- ============================================================
-- v7_multi_unit_booking — 多单位计费与预约（day/session/hour）
--
-- 目标版本: v7（多单位计费与预约）
-- 适用环境: MySQL 8.0.29+（增量部署步骤；应用启动不自动执行本脚本，
--           pet_order_wsh 缺失列由 OrderFulfillmentSchemaMigrator 运行时兜底，
--           pet_service_wsh 缺失列依赖本脚本或全量 schema 重建）。
--
-- 幂等性: 均为 ADD COLUMN IF NOT EXISTS ，重复执行不重复加列。
--         只新增列，不删除列、不 DROP 表、不回写历史订单。
--
-- 新增列:
--   * pet_service_wsh: duration_minutes_wsh / booking_mode_wsh
--   * pet_order_wsh:   billing_unit_wsh / quantity_wsh / unit_price_wsh / duration_minutes_wsh
-- ============================================================

ALTER TABLE pet_service_wsh ADD COLUMN IF NOT EXISTS `duration_minutes_wsh` INT NULL COMMENT 'session/hour 单次时长(分钟)，day 为 1440' AFTER `unit_wsh`;
ALTER TABLE pet_service_wsh ADD COLUMN IF NOT EXISTS `booking_mode_wsh` VARCHAR(20) NOT NULL DEFAULT 'date_range' COMMENT 'date_range(day)/slot(session, hour)' AFTER `duration_minutes_wsh`;

ALTER TABLE pet_order_wsh ADD COLUMN IF NOT EXISTS `billing_unit_wsh` VARCHAR(20) NOT NULL DEFAULT 'day' COMMENT '计费单位快照 day/session/hour' AFTER `price_per_day_wsh`;
ALTER TABLE pet_order_wsh ADD COLUMN IF NOT EXISTS `quantity_wsh` INT NOT NULL DEFAULT 1 COMMENT '计费数量：day=天数，session/hour=槽位数' AFTER `billing_unit_wsh`;
ALTER TABLE pet_order_wsh ADD COLUMN IF NOT EXISTS `unit_price_wsh` DECIMAL(10, 2) NULL COMMENT '单价快照(每个计费单位)' AFTER `quantity_wsh`;
ALTER TABLE pet_order_wsh ADD COLUMN IF NOT EXISTS `duration_minutes_wsh` INT NULL COMMENT '下单时单个单位时长快照(分钟)' AFTER `unit_price_wsh`;