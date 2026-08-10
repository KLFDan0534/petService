-- 看护员离线来源：用于关店同步状态时禁止覆盖看护员主动离线
ALTER TABLE keeper_wsh ADD COLUMN IF NOT EXISTS `offline_source_wsh` TINYINT NOT NULL DEFAULT 0 COMMENT '离线来源: 0-店铺同步/系统 1-看护员主动离线' AFTER `status_wsh`;