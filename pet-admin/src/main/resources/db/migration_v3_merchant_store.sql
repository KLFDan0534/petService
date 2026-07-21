ALTER TABLE merchant_wsh ADD COLUMN IF NOT EXISTS `store_mode_wsh` TINYINT DEFAULT 0 COMMENT '钀ヤ笟妯″紡: 0-鑷姩 1-鎵嬪姩寮€搴?2-鎵嬪姩鍏冲簵' AFTER `status_wsh`;
ALTER TABLE merchant_wsh ADD COLUMN IF NOT EXISTS `store_status_wsh` TINYINT DEFAULT 0 COMMENT '钀ヤ笟鐘舵€? 0-浼戞伅 1-钀ヤ笟' AFTER `store_mode_wsh`;
