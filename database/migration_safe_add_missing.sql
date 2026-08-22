-- ===================================================================
-- Migration: 补齐代码所需的缺失数据库对象
-- 安全：CREATE TABLE IF NOT EXISTS + information_schema 列存在检查
-- 不影响任何已有数据
-- ===================================================================

USE `pet_service`;

-- ===================================================================
-- 1. user_wsh — 补充 reject_reason_wsh 列
-- ===================================================================
SET @db = (SELECT DATABASE());
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_wsh' AND COLUMN_NAME = 'reject_reason_wsh');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `user_wsh` ADD COLUMN `reject_reason_wsh` VARCHAR(500) DEFAULT NULL COMMENT ''驳回原因'' AFTER `real_name_status_wsh`',
    'SELECT ''user_wsh.reject_reason_wsh already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 2. keeper_wsh — 补充 bio_wsh 列（schema.sql 有，但现有 MySQL 可能缺）
-- ===================================================================
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'keeper_wsh' AND COLUMN_NAME = 'bio_wsh');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `keeper_wsh` ADD COLUMN `bio_wsh` TEXT COMMENT ''简介'' AFTER `current_pets_wsh`',
    'SELECT ''keeper_wsh.bio_wsh already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 3. qualification_wsh — 资质证明表
-- ===================================================================
CREATE TABLE IF NOT EXISTS `qualification_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '资质ID',
  `owner_type_wsh` varchar(30) NOT NULL COMMENT '所属主体类型: merchant/keeper/adopter',
  `owner_id_wsh` bigint NOT NULL COMMENT '所属业务主体ID',
  `user_id_wsh` bigint DEFAULT NULL COMMENT '提交用户ID',
  `qual_type_wsh` varchar(50) NOT NULL COMMENT '资质类型',
  `title_wsh` varchar(100) DEFAULT NULL COMMENT '展示标题',
  `file_url_wsh` varchar(1000) DEFAULT NULL COMMENT '资质图片URL',
  `summary_wsh` varchar(500) DEFAULT NULL COMMENT '摘要',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
  `visibility_wsh` varchar(20) DEFAULT 'masked_public' COMMENT '可见性: private/masked_public/public',
  `reviewer_id_wsh` bigint DEFAULT NULL COMMENT '审核人',
  `review_remark_wsh` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_qualification_owner` (`owner_type_wsh`,`owner_id_wsh`) COMMENT '所属主体索引',
  KEY `idx_qualification_user` (`user_id_wsh`) COMMENT '提交用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资质证明表';

-- ===================================================================
-- 4. operation_log_wsh — 操作日志表
-- ===================================================================
CREATE TABLE IF NOT EXISTS `operation_log_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id_wsh` bigint DEFAULT NULL COMMENT '用户ID',
  `username_wsh` varchar(50) DEFAULT NULL COMMENT '用户名',
  `module_wsh` varchar(50) DEFAULT NULL COMMENT '模块',
  `operation_wsh` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `description_wsh` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `method_wsh` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_url_wsh` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_params_wsh` text COMMENT '请求参数',
  `request_body_wsh` text COMMENT '请求体',
  `response_body_wsh` text COMMENT '响应体',
  `ip_address_wsh` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `duration_wsh` bigint DEFAULT NULL COMMENT '耗时(ms)',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 1-成功 0-失败',
  `error_msg_wsh` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user_id` (`user_id_wsh`) COMMENT '用户索引',
  KEY `idx_module` (`module_wsh`) COMMENT '模块索引',
  KEY `idx_created_at` (`created_at_wsh`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志表';

-- ===================================================================
-- 5. business_hours_wsh — 营业时间表
-- ===================================================================
CREATE TABLE IF NOT EXISTS `business_hours_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '营业时间ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `day_of_week_wsh` tinyint NOT NULL COMMENT '星期(1-7)',
  `open_time_wsh` time NOT NULL COMMENT '开门时间',
  `close_time_wsh` time NOT NULL COMMENT '关门时间',
  `is_closed_wsh` tinyint DEFAULT '0' COMMENT '是否休息: 1-休息',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_merchant_day` (`merchant_id_wsh`,`day_of_week_wsh`) COMMENT '商家+星期唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营业时间表';

-- ===================================================================
-- 8. order_snapshot_wsh — 订单不可变快照表（schema.sql 已有定义但 MySQL 缺失）
-- ===================================================================
CREATE TABLE IF NOT EXISTS `order_snapshot_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL COMMENT '订单ID',
    `order_no_wsh` VARCHAR(50) NOT NULL COMMENT '订单号',
    `owner_snapshot_wsh` TEXT COMMENT '下单用户快照JSON',
    `pet_snapshot_wsh` TEXT COMMENT '宠物快照JSON',
    `merchant_snapshot_wsh` TEXT COMMENT '商户快照JSON',
    `keeper_snapshot_wsh` TEXT COMMENT '寄养员快照JSON',
    `service_snapshot_wsh` TEXT COMMENT '服务快照JSON',
    `address_snapshot_wsh` TEXT COMMENT '地址和履约信息快照JSON',
    `price_snapshot_wsh` TEXT COMMENT '价格快照JSON',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_order_snapshot_order` (`order_id_wsh`),
    INDEX `idx_order_snapshot_no` (`order_no_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单不可变快照';
