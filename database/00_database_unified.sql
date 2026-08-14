-- ===================================================================
-- Pet Service — 数据库统一 DDL（单一权威来源）
-- ===================================================================
-- 本文件合并了以下文件中的所有 DDL + 种子数据：
--   database/01_database_baseline.sql
--   pet-admin/src/main/resources/db/schema.sql
--   pet-admin/src/main/resources/h2-schema.sql
--   pet-admin/src/main/resources/db/migration.sql
--   pet-admin/src/main/resources/db/migration_v2_category_id_wsh.sql
--   pet-admin/src/main/resources/db/seed.sql
--   pet-admin/src/main/resources/db/restore_dml_only.sql
--
-- ✓ 所有 38 张表
-- ✓ 角色种子数据（5 个内置角色）
-- ✓ 知识库文档种子数据（7 篇内置文档）
-- ✓ 服务分类种子数据（18 个内置分类）
-- ===================================================================

CREATE DATABASE IF NOT EXISTS `pet_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `pet_service`;

SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================================
-- 1. user_wsh — 用户表
-- ===================================================================
DROP TABLE IF EXISTS `user_wsh`;
CREATE TABLE `user_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username_wsh` varchar(50) NOT NULL COMMENT '用户名',
  `password_wsh` varchar(255) NOT NULL COMMENT '密码',
  `payment_password_wsh` varchar(255) DEFAULT NULL COMMENT '支付密码哈希',
  `nickname_wsh` varchar(50) DEFAULT NULL COMMENT '昵称',
  `phone_wsh` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `email_wsh` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `gender_wsh` tinyint DEFAULT '0' COMMENT '性别: 0-未知 1-男 2-女',
  `real_name_wsh` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `id_card_no_wsh` varchar(32) DEFAULT NULL COMMENT '身份证号',
  `real_name_status_wsh` tinyint DEFAULT '0' COMMENT '实名状态: 0-未认证 1-待审核 2-已认证 3-已驳回',
  `reject_reason_wsh` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `address_wsh` varchar(255) DEFAULT NULL COMMENT '地址',
  `latitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `username_wsh` (`username_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ===================================================================
-- 2. role_wsh — 角色表
-- ===================================================================
DROP TABLE IF EXISTS `role_wsh`;
CREATE TABLE `role_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '角色名称',
  `code_wsh` varchar(50) NOT NULL COMMENT '角色编码',
  `description_wsh` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `name_wsh` (`name_wsh`),
  UNIQUE KEY `code_wsh` (`code_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ===================================================================
-- 3. user_role_wsh — 用户角色关联表
-- ===================================================================
DROP TABLE IF EXISTS `user_role_wsh`;
CREATE TABLE `user_role_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `role_id_wsh` bigint NOT NULL COMMENT '角色ID',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted_wsh` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_user_role` (`user_id_wsh`,`role_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

-- ===================================================================
-- 4. category_wsh — 宠物分类表
-- ===================================================================
DROP TABLE IF EXISTS `category_wsh`;
CREATE TABLE `category_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '分类名称',
  `parent_id_wsh` bigint DEFAULT '0' COMMENT '父分类ID',
  `sort_order_wsh` int DEFAULT '0' COMMENT '排序',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物分类表';

-- ===================================================================
-- 5. pet_wsh — 宠物表
-- ===================================================================
DROP TABLE IF EXISTS `pet_wsh`;
CREATE TABLE `pet_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '宠物ID',
  `owner_id_wsh` bigint NOT NULL COMMENT '主人ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '宠物名称',
  `type_wsh` varchar(50) NOT NULL COMMENT '类型: dog/cat/other',
  `breed_wsh` varchar(100) DEFAULT NULL COMMENT '品种',
  `age_wsh` int DEFAULT NULL COMMENT '年龄(月)',
  `weight_wsh` decimal(10,2) DEFAULT NULL COMMENT '体重(kg)',
  `gender_wsh` tinyint DEFAULT NULL COMMENT '性别: 0-母 1-公',
  `sterilized_wsh` tinyint DEFAULT '0' COMMENT '是否绝育: 0-否 1-是',
  `vaccinated_wsh` tinyint DEFAULT '0' COMMENT '是否接种疫苗: 0-否 1-是',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `description_wsh` text COMMENT '描述',
  `allergies_wsh` text COMMENT '过敏信息',
  `habits_wsh` text COMMENT '生活习惯',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物表';

-- ===================================================================
-- 6. care_record_wsh — 照护记录表
-- ===================================================================
DROP TABLE IF EXISTS `care_record_wsh`;
CREATE TABLE `care_record_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `pet_id_wsh` bigint DEFAULT NULL COMMENT '宠物ID',
  `keeper_id_wsh` bigint DEFAULT NULL COMMENT '寄养员ID',
  `type_wsh` varchar(20) DEFAULT 'feed' COMMENT '类型: feed-喂食 activity-活动 medication-用药 health-健康',
  `content_wsh` text COMMENT '记录内容',
  `images_wsh` varchar(2000) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `record_time_wsh` datetime DEFAULT NULL COMMENT '记录时间',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='照护记录表';

-- ===================================================================
-- 7. merchant_wsh — 商家表
-- ===================================================================
DROP TABLE IF EXISTS `merchant_wsh`;
CREATE TABLE `merchant_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '商家ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `name_wsh` varchar(100) NOT NULL COMMENT '商家名称',
  `phone_wsh` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address_wsh` varchar(255) DEFAULT NULL COMMENT '地址',
  `latitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `description_wsh` text COMMENT '描述',
  `business_license_wsh` varchar(500) DEFAULT NULL COMMENT '营业执照URL',
  `rating_wsh` decimal(3,2) DEFAULT '5.00' COMMENT '评分',
  `status_wsh` tinyint DEFAULT '0' COMMENT '状态: 0-待审核 1-已通过 2-已驳回',
  `future_booking_enabled_wsh` tinyint NOT NULL DEFAULT '1' COMMENT '是否接受未来预约: 0-关闭 1-开启(默认开启)',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '商家头像',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家表';

-- ===================================================================
-- 8. keeper_wsh — 看护人表
-- ===================================================================
DROP TABLE IF EXISTS `keeper_wsh`;
CREATE TABLE `keeper_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '看护人ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '姓名',
  `phone_wsh` varchar(20) DEFAULT NULL COMMENT '电话',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '头像',
  `experience_years_wsh` int DEFAULT '0' COMMENT '经验(年)',
  `rating_wsh` decimal(3,2) DEFAULT '5.00' COMMENT '评分',
  `completion_rate_wsh` decimal(5,2) DEFAULT '100.00' COMMENT '完成率',
  `complaint_rate_wsh` decimal(5,2) DEFAULT '0.00' COMMENT '投诉率',
  `price_per_day_wsh` decimal(10,2) NOT NULL COMMENT '每天价格',
  `max_pets_wsh` int DEFAULT '5' COMMENT '最大宠物数',
  `current_pets_wsh` int DEFAULT '0' COMMENT '当前宠物数',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-离线 1-在线',
  `offline_source_wsh` tinyint DEFAULT '0' COMMENT '离线来源: 0-店铺同步/系统 1-看护员主动离线',
  `bio_wsh` text COMMENT '简介',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='看护人表';

-- ===================================================================
-- 9. service_category_wsh — 服务分类表（V2 层级分类）
-- ===================================================================
DROP TABLE IF EXISTS `service_category_wsh`;
CREATE TABLE `service_category_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id_wsh` bigint DEFAULT NULL COMMENT '父级分类ID，NULL为顶级分类',
  `name_wsh` varchar(50) NOT NULL COMMENT '分类名称（中文）',
  `code_wsh` varchar(50) NOT NULL COMMENT '分类编码，全大写下划线风格',
  `sort_wsh` int DEFAULT '0' COMMENT '排序号',
  `status_wsh` tinyint DEFAULT '1' COMMENT '0-禁用 1-启用',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `code_wsh` (`code_wsh`),
  KEY `idx_parent_id` (`parent_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务分类表';

-- ===================================================================
-- 10. pet_service_wsh — 服务项目表
-- ===================================================================
DROP TABLE IF EXISTS `pet_service_wsh`;
CREATE TABLE `pet_service_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `name_wsh` varchar(100) NOT NULL COMMENT '服务名称',
  `type_wsh` varchar(50) DEFAULT NULL COMMENT '兼容旧字段，新数据从 service_category_wsh.code_wsh 派生',
  `category_id_wsh` bigint DEFAULT NULL COMMENT '关联 service_category_wsh.id_wsh',
  `description_wsh` text COMMENT '描述',
  `price_wsh` decimal(10,2) NOT NULL COMMENT '价格',
  `unit_wsh` varchar(20) DEFAULT 'day' COMMENT '单位',
  `images_wsh` text COMMENT '图片URL',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-下架 1-上架',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_category_id` (`category_id_wsh`),
  KEY `idx_merchant_id` (`merchant_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务项目表';

-- ===================================================================
-- 11. address_wsh — 收货地址表
-- ===================================================================
DROP TABLE IF EXISTS `address_wsh`;
CREATE TABLE `address_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `label_wsh` varchar(50) DEFAULT '' COMMENT '标签(家/公司/学校)',
  `name_wsh` varchar(50) NOT NULL COMMENT '收货人姓名',
  `phone_wsh` varchar(20) NOT NULL COMMENT '收货人电话',
  `address_wsh` varchar(500) NOT NULL COMMENT '详细地址',
  `detail_wsh` varchar(500) DEFAULT '' COMMENT '门牌号等补充信息',
  `latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `is_default_wsh` tinyint DEFAULT '0' COMMENT '是否默认: 0-否 1-是',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收货地址表';

-- ===================================================================
-- 12. business_hours_wsh — 营业时间表
-- ===================================================================
DROP TABLE IF EXISTS `business_hours_wsh`;
CREATE TABLE `business_hours_wsh` (
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
-- 13. pet_order_wsh — 订单表
-- ===================================================================
DROP TABLE IF EXISTS `pet_order_wsh`;
CREATE TABLE `pet_order_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no_wsh` varchar(50) NOT NULL COMMENT '订单号',
  `owner_id_wsh` bigint NOT NULL COMMENT '宠物主ID',
  `pet_id_wsh` bigint NOT NULL COMMENT '宠物ID',
  `keeper_id_wsh` bigint NOT NULL COMMENT '看护人ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `service_id_wsh` bigint DEFAULT NULL COMMENT '服务项目ID',
  `start_date_wsh` date NOT NULL COMMENT '开始日期',
  `end_date_wsh` date NOT NULL COMMENT '结束日期',
  `days_wsh` int NOT NULL COMMENT '天数',
  `price_per_day_wsh` decimal(10,2) NOT NULL COMMENT '每天价格',
  `total_amount_wsh` decimal(10,2) NOT NULL COMMENT '总金额',
  `discount_wsh` decimal(10,2) DEFAULT '0.00' COMMENT '折扣',
  `final_amount_wsh` decimal(10,2) NOT NULL COMMENT '实付金额',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/paid/confirmed/delivered/received/in_progress/completed/cancelled/refunding/refunded',
  `remark_wsh` text COMMENT '备注',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delivery_address_wsh` varchar(500) DEFAULT NULL COMMENT '送养地址',
  `delivery_time_wsh` datetime DEFAULT NULL COMMENT '送养时间',
  `receiver_available_start_wsh` datetime DEFAULT NULL COMMENT '接收人可接收开始时间',
  `receiver_available_end_wsh` datetime DEFAULT NULL COMMENT '接收人可接收结束时间',
  `pickup_address_wsh` varchar(500) DEFAULT NULL COMMENT '接回地址',
  `pickup_time_wsh` datetime DEFAULT NULL COMMENT '接回时间',
  `delivered_at_wsh` datetime DEFAULT NULL COMMENT '交付时间',
  `received_at_wsh` datetime DEFAULT NULL COMMENT '接收时间',
  `started_at_wsh` datetime DEFAULT NULL COMMENT '服务开始时间',
  `completed_at_wsh` datetime DEFAULT NULL COMMENT '服务完成时间',
  `final_report_generated_wsh` tinyint DEFAULT '0' COMMENT '是否生成最终AI报告',
  `handover_code_wsh` varchar(4) DEFAULT NULL COMMENT '交接验证码',
  `start_photo_wsh` varchar(1000) DEFAULT NULL COMMENT '开始服务照片URL',
  `delivery_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '送养纬度',
  `delivery_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '送养经度',
  `delivery_location_source_wsh` varchar(50) DEFAULT NULL COMMENT '送养位置来源',
  `pickup_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '接回纬度',
  `pickup_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '接回经度',
  `pickup_location_source_wsh` varchar(50) DEFAULT NULL COMMENT '接回位置来源',
  `delivered_address_wsh` varchar(500) DEFAULT NULL COMMENT '实际交付地址',
  `delivered_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '交付纬度',
  `delivered_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '交付经度',
  `delivered_accuracy_wsh` decimal(10,2) DEFAULT NULL COMMENT '交付定位精度(米)',
  `received_address_wsh` varchar(500) DEFAULT NULL COMMENT '实际接收地址',
  `received_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '接收纬度',
  `received_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '接收经度',
  `received_accuracy_wsh` decimal(10,2) DEFAULT NULL COMMENT '接收定位精度(米)',
  `received_distance_m_wsh` decimal(10,1) DEFAULT NULL COMMENT '交接距离(米)',
  `emergency_contact_name_wsh` varchar(50) DEFAULT NULL COMMENT '紧急联系人姓名',
  `emergency_contact_phone_wsh` varchar(20) DEFAULT NULL COMMENT '紧急联系人电话',
  `emergency_contact_relation_wsh` varchar(30) DEFAULT NULL COMMENT '紧急联系人与宠物关系',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `order_no_wsh` (`order_no_wsh`) COMMENT '订单号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

-- ===================================================================
-- 14. order_snapshot_wsh — 订单不可变快照表
-- ===================================================================
DROP TABLE IF EXISTS `order_snapshot_wsh`;
CREATE TABLE `order_snapshot_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `order_no_wsh` varchar(50) NOT NULL COMMENT '订单号',
  `owner_snapshot_wsh` text COMMENT '下单用户快照JSON',
  `pet_snapshot_wsh` text COMMENT '宠物快照JSON',
  `merchant_snapshot_wsh` text COMMENT '商户快照JSON',
  `keeper_snapshot_wsh` text COMMENT '寄养员快照JSON',
  `service_snapshot_wsh` text COMMENT '服务快照JSON',
  `address_snapshot_wsh` text COMMENT '地址和履约信息快照JSON',
  `price_snapshot_wsh` text COMMENT '价格快照JSON',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_order_snapshot_order` (`order_id_wsh`),
  KEY `idx_order_snapshot_no` (`order_no_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单不可变快照表';

-- ===================================================================
-- 15. payment_wsh — 支付表
-- ===================================================================
DROP TABLE IF EXISTS `payment_wsh`;
CREATE TABLE `payment_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '支付ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `order_no_wsh` varchar(50) NOT NULL COMMENT '订单号',
  `pay_no_wsh` varchar(100) DEFAULT NULL COMMENT '支付流水号',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '支付金额',
  `method_wsh` varchar(20) DEFAULT 'wechat' COMMENT '支付方式: wechat/alipay/balance',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/success/failed',
  `paid_at_wsh` datetime DEFAULT NULL COMMENT '支付时间',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付表';

-- ===================================================================
-- 16. refund_wsh — 退款表
-- ===================================================================
DROP TABLE IF EXISTS `refund_wsh`;
CREATE TABLE `refund_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '退款ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `order_no_wsh` varchar(50) NOT NULL COMMENT '订单号',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '退款金额',
  `reason_wsh` text COMMENT '退款原因',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/approved/rejected/completed',
  `order_status_before_refund_wsh` varchar(30) DEFAULT NULL COMMENT '退款前订单状态',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款表';

-- ===================================================================
-- 17. tip_wsh — 打赏表
-- ===================================================================
DROP TABLE IF EXISTS `tip_wsh`;
CREATE TABLE `tip_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '打赏ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `from_user_id_wsh` bigint NOT NULL COMMENT '打赏用户',
  `to_user_id_wsh` bigint NOT NULL COMMENT '接收用户',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '打赏金额',
  `message_wsh` varchar(200) DEFAULT NULL COMMENT '打赏留言',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏记录表';

-- ===================================================================
-- 18. chat_message_wsh — 聊天消息表
-- ===================================================================
DROP TABLE IF EXISTS `chat_message_wsh`;
CREATE TABLE `chat_message_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `from_user_id_wsh` bigint NOT NULL COMMENT '发送人ID',
  `to_user_id_wsh` bigint NOT NULL COMMENT '接收人ID',
  `order_id_wsh` bigint DEFAULT NULL COMMENT '关联订单ID',
  `content_wsh` text COMMENT '消息内容',
  `type_wsh` varchar(20) DEFAULT 'text' COMMENT '类型: text/image/video/file',
  `file_url_wsh` varchar(500) DEFAULT NULL COMMENT '文件URL',
  `read_wsh` tinyint DEFAULT '0' COMMENT '是否已读: 0-未读 1-已读',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';

-- ===================================================================
-- 19. complaint_wsh — 投诉表
-- ===================================================================
DROP TABLE IF EXISTS `complaint_wsh`;
CREATE TABLE `complaint_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '投诉ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `owner_id_wsh` bigint NOT NULL COMMENT '投诉人ID',
  `target_id_wsh` bigint DEFAULT NULL COMMENT '被投诉对象ID',
  `target_type_wsh` varchar(20) DEFAULT NULL COMMENT '被投诉类型: merchant/keeper',
  `title_wsh` varchar(200) DEFAULT NULL COMMENT '投诉标题',
  `content_wsh` text COMMENT '投诉内容',
  `images_wsh` text COMMENT '投诉图片URL',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/processing/resolved/rejected',
  `result_wsh` text COMMENT '处理结果',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投诉表';

-- ===================================================================
-- 20. rating_wsh — 评价表
-- ===================================================================
DROP TABLE IF EXISTS `rating_wsh`;
CREATE TABLE `rating_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id_wsh` bigint DEFAULT NULL COMMENT '订单ID(服务评价可为空)',
  `user_id_wsh` bigint NOT NULL COMMENT '评价人ID',
  `target_id_wsh` bigint NOT NULL COMMENT '被评价对象ID',
  `target_type_wsh` varchar(20) NOT NULL COMMENT '被评价类型: merchant/keeper/service',
  `score_wsh` tinyint NOT NULL COMMENT '评分(1-5)',
  `content_wsh` text COMMENT '评价内容',
  `images_wsh` text COMMENT '评价图片URL',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `reply_wsh` text COMMENT '商家回复',
  `reply_at_wsh` datetime DEFAULT NULL COMMENT '回复时间',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_rating_order_user_type` (`order_id_wsh`, `user_id_wsh`, `target_type_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价表';

-- ===================================================================
-- 21. ticket_wsh — 工单表
-- ===================================================================
DROP TABLE IF EXISTS `ticket_wsh`;
CREATE TABLE `ticket_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `user_id_wsh` bigint NOT NULL COMMENT '创建用户ID',
  `title_wsh` varchar(200) NOT NULL COMMENT '工单标题',
  `content_wsh` text COMMENT '工单内容',
  `category_wsh` varchar(50) DEFAULT NULL COMMENT '分类: complaint-投诉 question-咨询 suggestion-建议 other-其他',
  `priority_wsh` varchar(20) DEFAULT 'medium' COMMENT '优先级: low/medium/high/urgent',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending-待处理 processing-处理中 resolved-已解决 closed-已关闭',
  `assignee_id_wsh` bigint DEFAULT NULL COMMENT '处理人ID',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服工单表';

-- ===================================================================
-- 22. ticket_message_wsh — 工单消息表
-- ===================================================================
DROP TABLE IF EXISTS `ticket_message_wsh`;
CREATE TABLE `ticket_message_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `ticket_id_wsh` bigint NOT NULL COMMENT '工单ID',
  `user_id_wsh` bigint NOT NULL COMMENT '发送人ID',
  `content_wsh` text NOT NULL COMMENT '消息内容',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单消息表';

-- ===================================================================
-- 23. wallet_wsh — 钱包表
-- ===================================================================
DROP TABLE IF EXISTS `wallet_wsh`;
CREATE TABLE `wallet_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `balance_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '余额',
  `frozen_amount_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '冻结金额',
  `version_wsh` int DEFAULT '0' COMMENT '乐观锁',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_wallet_user` (`user_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包表';

-- ===================================================================
-- 24. wallet_transaction_wsh — 交易流水表
-- ===================================================================
DROP TABLE IF EXISTS `wallet_transaction_wsh`;
CREATE TABLE `wallet_transaction_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `wallet_id_wsh` bigint DEFAULT NULL COMMENT '钱包ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `type_wsh` varchar(20) DEFAULT 'income' COMMENT '类型: income-收入 expense-支出 withdraw-提现 refund-退款',
  `amount_wsh` decimal(12,2) NOT NULL COMMENT '金额',
  `balance_before_wsh` decimal(12,2) DEFAULT NULL COMMENT '变动前余额',
  `balance_after_wsh` decimal(12,2) DEFAULT NULL COMMENT '变动后余额',
  `frozen_before_wsh` decimal(12,2) DEFAULT NULL COMMENT '冻结前金额',
  `frozen_after_wsh` decimal(12,2) DEFAULT NULL COMMENT '冻结后金额',
  `direction_wsh` varchar(20) DEFAULT NULL COMMENT '方向: in-out-freeze-unfreeze-set',
  `status_wsh` varchar(20) DEFAULT 'success' COMMENT '状态: pending-success-failed',
  `business_type_wsh` varchar(50) DEFAULT NULL COMMENT '业务类型: payment-refund-settlement-withdraw-tip-admin_adjust',
  `business_id_wsh` varchar(100) DEFAULT NULL COMMENT '业务ID',
  `request_id_wsh` varchar(120) DEFAULT NULL COMMENT '幂等请求ID',
  `order_id_wsh` bigint DEFAULT NULL COMMENT '关联订单',
  `description_wsh` varchar(500) DEFAULT NULL COMMENT '描述',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_wallet_tx_request` (`request_id_wsh`),
  KEY `idx_wallet_tx_user` (`user_id_wsh`),
  KEY `idx_wallet_tx_business` (`business_type_wsh`, `business_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易流水表';

-- ===================================================================
-- 25. withdrawal_wsh — 提现记录表
-- ===================================================================
DROP TABLE IF EXISTS `withdrawal_wsh`;
CREATE TABLE `withdrawal_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '提现ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `amount_wsh` decimal(12,2) NOT NULL COMMENT '提现金额',
  `fee_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '手续费',
  `actual_amount_wsh` decimal(12,2) DEFAULT NULL COMMENT '实际到账',
  `bank_name_wsh` varchar(100) DEFAULT NULL COMMENT '银行名称',
  `bank_card_wsh` varchar(100) DEFAULT NULL COMMENT '银行卡号',
  `account_name_wsh` varchar(100) DEFAULT NULL COMMENT '持卡人',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending-待审核 approved-已通过 rejected-已驳回 completed-已完成',
  `remark_wsh` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提现记录表';

-- ===================================================================
-- 26. notice_wsh — 公告表
-- ===================================================================
DROP TABLE IF EXISTS `notice_wsh`;
CREATE TABLE `notice_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title_wsh` varchar(200) NOT NULL COMMENT '标题',
  `content_wsh` text COMMENT '内容',
  `type_wsh` varchar(20) DEFAULT 'notice' COMMENT '类型: notice-公告 banner-Banner',
  `image_url_wsh` varchar(500) DEFAULT NULL COMMENT '图片URL',
  `link_url_wsh` varchar(500) DEFAULT NULL COMMENT '跳转链接',
  `sort_order_wsh` int DEFAULT '0' COMMENT '排序',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 1-显示 0-隐藏',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告与Banner表';

-- ===================================================================
-- 27. notice_read_wsh — 公告已读表
-- ===================================================================
DROP TABLE IF EXISTS `notice_read_wsh`;
CREATE TABLE `notice_read_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '已读记录ID',
  `notice_id_wsh` bigint NOT NULL COMMENT '公告ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `read_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_notice_read_notice_user` (`notice_id_wsh`,`user_id_wsh`) COMMENT '公告+用户唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';

-- ===================================================================
-- 28. notification_wsh — 通知表
-- ===================================================================
DROP TABLE IF EXISTS `notification_wsh`;
CREATE TABLE `notification_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id_wsh` bigint NOT NULL COMMENT '接收用户ID',
  `title_wsh` varchar(200) NOT NULL COMMENT '通知标题',
  `content_wsh` text COMMENT '通知内容',
  `type_wsh` varchar(30) DEFAULT NULL COMMENT '类型: order/payment/ticket/system',
  `is_read_wsh` tinyint DEFAULT '0' COMMENT '是否已读: 1-已读',
  `related_id_wsh` bigint DEFAULT NULL COMMENT '关联业务ID',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user` (`user_id_wsh`) COMMENT '用户索引',
  KEY `idx_read` (`is_read_wsh`) COMMENT '已读状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';

-- ===================================================================
-- 29. operation_log_wsh — 操作日志表
-- ===================================================================
DROP TABLE IF EXISTS `operation_log_wsh`;
CREATE TABLE `operation_log_wsh` (
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
-- 30. file_record_wsh — 文件记录表
-- ===================================================================
DROP TABLE IF EXISTS `file_record_wsh`;
CREATE TABLE `file_record_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name_wsh` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `object_name_wsh` varchar(500) NOT NULL COMMENT '存储对象名(MiniO)',
  `size_wsh` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `content_type_wsh` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `purpose_wsh` varchar(30) DEFAULT NULL COMMENT '文件用途: product-产品图片 avatar-头像 evidence-资质证明 其他',
  `merchant_id_wsh` bigint DEFAULT NULL COMMENT '服务端归属商家ID(产品图片等受管文件)',
  `user_id_wsh` bigint DEFAULT NULL COMMENT '上传用户ID',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user` (`user_id_wsh`) COMMENT '用户索引',
  KEY `idx_purpose_merchant` (`purpose_wsh`, `merchant_id_wsh`) COMMENT '产品图片用途+商家索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件上传记录表';

-- ===================================================================
-- 30.1 pet_service_media_wsh — 服务产品图片表
-- ===================================================================
DROP TABLE IF EXISTS `pet_service_media_wsh`;
CREATE TABLE `pet_service_media_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `service_id_wsh` bigint NOT NULL COMMENT '服务产品ID(pet_service_wsh.id_wsh)',
  `file_id_wsh` bigint NOT NULL COMMENT '文件记录ID(file_record_wsh.id_wsh)',
  `sort_order_wsh` int NOT NULL DEFAULT '0' COMMENT '排序序号(0..N 连续)',
  `is_cover_wsh` tinyint NOT NULL DEFAULT '0' COMMENT '是否封面: 0-否 1-是',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_service_file` (`service_id_wsh`, `file_id_wsh`) COMMENT '同一服务不重复引用同一文件',
  UNIQUE KEY `uk_service_sort` (`service_id_wsh`, `sort_order_wsh`) COMMENT '同一服务内排序唯一',
  KEY `idx_media_file` (`file_id_wsh`) COMMENT '文件索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务产品图片表';

-- ===================================================================
-- 31. qualification_wsh — 资质证明表
-- ===================================================================
DROP TABLE IF EXISTS `qualification_wsh`;
CREATE TABLE `qualification_wsh` (
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
-- 32. favorite_wsh — 收藏表
-- ===================================================================
DROP TABLE IF EXISTS `favorite_wsh`;
CREATE TABLE `favorite_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `target_id_wsh` bigint NOT NULL COMMENT '目标ID',
  `target_type_wsh` varchar(20) NOT NULL COMMENT '目标类型: merchant/keeper',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_user_target` (`user_id_wsh`,`target_id_wsh`,`target_type_wsh`) COMMENT '用户+目标唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';

-- ===================================================================
-- 33. content_review_wsh — 内容审核表
-- ===================================================================
DROP TABLE IF EXISTS `content_review_wsh`;
CREATE TABLE `content_review_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '审核ID',
  `target_type_wsh` varchar(50) NOT NULL COMMENT '目标类型: rating/complaint/chat/等',
  `target_id_wsh` bigint NOT NULL COMMENT '目标ID',
  `reporter_id_wsh` bigint DEFAULT NULL COMMENT '举报人',
  `reason_wsh` varchar(500) DEFAULT NULL COMMENT '举报原因',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending-待审核 approved-已通过 rejected-已驳回',
  `reviewer_id_wsh` bigint DEFAULT NULL COMMENT '审核人',
  `review_remark_wsh` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容审核表';

-- ===================================================================
-- 34. adoption_pet_wsh — 领养宠物表
-- ===================================================================
DROP TABLE IF EXISTS `adoption_pet_wsh`;
CREATE TABLE `adoption_pet_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '领养宠物ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '宠物名称',
  `type_wsh` varchar(30) DEFAULT NULL COMMENT '宠物类型',
  `breed_wsh` varchar(100) DEFAULT NULL COMMENT '品种',
  `age_wsh` int DEFAULT NULL COMMENT '年龄(月)',
  `gender_wsh` varchar(10) DEFAULT NULL COMMENT '性别',
  `weight_wsh` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `color_wsh` varchar(50) DEFAULT NULL COMMENT '颜色',
  `health_status_wsh` varchar(500) DEFAULT NULL COMMENT '健康状况',
  `vaccinated_wsh` tinyint DEFAULT '0' COMMENT '是否接种疫苗',
  `sterilized_wsh` tinyint DEFAULT '0' COMMENT '是否绝育',
  `personality_wsh` varchar(500) DEFAULT NULL COMMENT '性格',
  `story_wsh` text COMMENT '救助故事',
  `adoption_requirements_wsh` text COMMENT '领养要求',
  `adoption_fee_wsh` decimal(10,2) DEFAULT '0.00' COMMENT '领养费',
  `cover_image_wsh` varchar(500) DEFAULT NULL COMMENT '封面图片',
  `images_wsh` varchar(2000) DEFAULT NULL COMMENT '图片(逗号分隔)',
  `status_wsh` varchar(20) DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE/APPLIED/ADOPTED',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_merchant` (`merchant_id_wsh`) COMMENT '商家索引',
  KEY `idx_status` (`status_wsh`) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领养宠物表';

-- ===================================================================
-- 35. adoption_application_wsh — 领养申请表
-- ===================================================================
DROP TABLE IF EXISTS `adoption_application_wsh`;
CREATE TABLE `adoption_application_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id_wsh` bigint NOT NULL COMMENT '申请人用户ID',
  `pet_id_wsh` bigint NOT NULL COMMENT '领养宠物ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `applicant_name_wsh` varchar(50) NOT NULL COMMENT '申请人姓名',
  `applicant_phone_wsh` varchar(20) NOT NULL COMMENT '申请人电话',
  `applicant_address_wsh` varchar(500) NOT NULL COMMENT '家庭地址',
  `housing_type_wsh` varchar(50) DEFAULT NULL COMMENT '住房类型',
  `has_yard_wsh` tinyint DEFAULT '0' COMMENT '是否有院子',
  `family_members_wsh` varchar(200) DEFAULT NULL COMMENT '家庭成员',
  `pet_experience_wsh` text COMMENT '养宠经验',
  `reason_wsh` text NOT NULL COMMENT '领养原因',
  `economic_condition_wsh` varchar(200) DEFAULT NULL COMMENT '经济状况',
  `agree_visit_wsh` tinyint DEFAULT '0' COMMENT '同意回访',
  `merchant_status_wsh` varchar(20) DEFAULT NULL COMMENT '商家审核状态',
  `merchant_remark_wsh` varchar(500) DEFAULT NULL COMMENT '商家审核备注',
  `admin_status_wsh` varchar(20) DEFAULT NULL COMMENT '管理员审核状态',
  `admin_remark_wsh` varchar(500) DEFAULT NULL COMMENT '管理员审核备注',
  `status_wsh` varchar(20) DEFAULT 'PENDING' COMMENT '总体状态',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user` (`user_id_wsh`) COMMENT '用户索引',
  KEY `idx_pet` (`pet_id_wsh`) COMMENT '宠物索引',
  KEY `idx_status` (`status_wsh`) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领养申请表';

-- ===================================================================
-- 36. ai_report_wsh — AI报告表
-- ===================================================================
DROP TABLE IF EXISTS `ai_report_wsh`;
CREATE TABLE `ai_report_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `pet_id_wsh` bigint NOT NULL COMMENT '宠物ID',
  `keeper_id_wsh` bigint NOT NULL COMMENT '看护人ID',
  `content_wsh` text COMMENT '报告内容',
  `type_wsh` varchar(50) DEFAULT NULL COMMENT '类型: daily/final/health',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI报告表';

-- ===================================================================
-- 37. knowledge_document_wsh — 知识库文档表
-- ===================================================================
DROP TABLE IF EXISTS `knowledge_document_wsh`;
CREATE TABLE `knowledge_document_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `title_wsh` varchar(200) NOT NULL COMMENT '文档标题',
  `content_wsh` text NOT NULL COMMENT '文档内容',
  `category_wsh` varchar(50) DEFAULT NULL COMMENT '分类: boarding/refund/complaint/care/vaccine/agreement/rule',
  `source_type_wsh` varchar(20) DEFAULT NULL COMMENT '来源类型: pdf/word/markdown/txt',
  `source_path_wsh` varchar(500) DEFAULT NULL COMMENT '来源路径',
  `word_count_wsh` int DEFAULT '0' COMMENT '字数',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档表';

-- ===================================================================
-- 38. document_embedding_wsh — 文档嵌入向量表
-- ===================================================================
DROP TABLE IF EXISTS `document_embedding_wsh`;
CREATE TABLE `document_embedding_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '嵌入ID',
  `document_id_wsh` bigint NOT NULL COMMENT '文档ID',
  `embedding_wsh` longtext COMMENT '嵌入向量(JSON数组)',
  `dimension_wsh` int DEFAULT '0' COMMENT '向量维度',
  `chunk_index_wsh` int DEFAULT '0' COMMENT '分块索引',
  `chunk_text_wsh` text COMMENT '分块文本',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  KEY `document_id_wsh` (`document_id_wsh`) COMMENT '文档索引',
  CONSTRAINT `document_embedding_wsh_ibfk_1` FOREIGN KEY (`document_id_wsh`) REFERENCES `knowledge_document_wsh` (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档嵌入向量表';

SET FOREIGN_KEY_CHECKS = 1;

-- ===================================================================
-- 种子数据 — 角色
-- ===================================================================
INSERT IGNORE INTO `role_wsh` (`id_wsh`, `name_wsh`, `code_wsh`, `description_wsh`) VALUES
(1, '系统管理员', 'ADMIN', '系统管理员'),
(2, '宠物主人', 'OWNER', '宠物主人'),
(3, '寄养员', 'KEEPER', '宠物寄养员'),
(4, '商家', 'MERCHANT', '商家'),
(5, '客服', 'CUSTOMER_SERVICE', '客服人员');

-- ===================================================================
-- 种子数据 — 知识库文档
-- ===================================================================
INSERT IGNORE INTO `knowledge_document_wsh` (`id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `source_type_wsh`) VALUES
(1, '宠物寄养规则', '1. 寄养前需确保宠物已完成疫苗接种\n2. 寄养期间宠物主需提供宠物食物\n3. 寄养员需每日提供宠物活动时间不少于2小时\n4. 寄养期间如宠物生病需及时通知宠物主\n5. 寄养期间宠物意外伤害由寄养员负责\n6. 寄养期间宠物死亡由寄养员全责赔偿\n7. 寄养前需签订寄养协议\n8. 寄养费用按天计算，不足一天按一天计算', 'boarding', 'txt'),
(2, '退款规则', '1. 订单支付后24小时内可全额退款\n2. 寄养开始前3天退款扣除10%手续费\n3. 寄养开始前1天退款扣除30%手续费\n4. 寄养开始后不接受退款\n5. 因平台原因导致无法服务的，全额退款\n6. 因商家原因取消订单的，全额退款并赔偿20%\n7. 退款到账时间：1-3个工作日\n8. 部分退款按实际未服务天数计算', 'refund', 'txt'),
(3, '投诉规则', '1. 投诉需在服务结束后7天内提出\n2. 投诉需要提供相关证据（照片/视频）\n3. 平台客服将在24小时内受理投诉\n4. 投诉处理周期为3-5个工作日\n5. 严重投诉升级至平台管理员处理\n6. 恶意投诉将影响用户信用分\n7. 投诉处理结果将以站内信形式通知\n8. 对处理结果不满可再次申诉', 'complaint', 'txt'),
(4, '宠物护理指南', '1. 每日定时喂食，保持饮食规律\n2. 提供充足的饮用水\n3. 每日遛狗不少于2次\n4. 保持居住环境清洁卫生\n5. 定期梳毛，防止打结\n6. 注意观察宠物精神状态\n7. 发现异常及时记录并通知主人\n8. 按主人要求给药（如有）', 'care', 'txt'),
(5, '疫苗要求', '1. 寄养宠物必须完成核心疫苗接种\n2. 犬类：狂犬病疫苗、犬瘟热疫苗、细小病毒疫苗\n3. 猫类：狂犬病疫苗、猫三联疫苗\n4. 疫苗需在有效期内\n5. 需提供疫苗接种证明\n6. 未接种疫苗的宠物需加收健康管理费\n7. 建议接种流感疫苗（季节性）\n8. 老年宠物建议做健康检查', 'vaccine', 'txt');

-- ===================================================================
-- 种子数据 — 服务分类（V2 层级）
-- ===================================================================
INSERT IGNORE INTO `service_category_wsh` (`id_wsh`, `parent_id_wsh`, `name_wsh`, `code_wsh`, `sort_wsh`, `status_wsh`) VALUES
-- Level 1
(1, NULL, '寄养服务', 'BOARDING', 1, 1),
(2, NULL, '美容护理', 'GROOMING', 2, 1),
(3, NULL, '训练服务', 'TRAINING', 3, 1),
(4, NULL, '遛宠服务', 'WALK', 4, 1),
(5, NULL, '医疗保健', 'MEDICAL', 5, 1),
-- Level 2 - 寄养服务
(11, 1, '标准寄养', 'BOARDING_STANDARD', 1, 1),
(12, 1, '豪华寄养', 'BOARDING_VIP', 2, 1),
(13, 1, '日间寄养', 'BOARDING_DAYCARE', 3, 1),
-- Level 2 - 美容护理
(21, 2, '基础美容', 'GROOMING_BASIC', 1, 1),
(22, 2, '全效美容', 'GROOMING_FULL', 2, 1),
(23, 2, 'SPA护理', 'GROOMING_SPA', 3, 1),
-- Level 2 - 训练服务
(31, 3, '基础训练', 'TRAINING_BASIC', 1, 1),
(32, 3, '进阶训练', 'TRAINING_ADVANCED', 2, 1),
(33, 3, '行为矫正', 'TRAINING_BEHAVIOR', 3, 1),
-- Level 2 - 遛宠服务
(41, 4, '标准遛弯', 'WALK_STANDARD', 1, 1),
(42, 4, '长时间遛弯', 'WALK_EXTENDED', 2, 1),
-- Level 2 - 医疗保健
(51, 5, '常规体检', 'MEDICAL_CHECKUP', 1, 1),
(52, 5, '疫苗接种', 'MEDICAL_VACCINE', 2, 1),
(53, 5, '驱虫护理', 'MEDICAL_DEWORM', 3, 1),
(54, 5, '术后护理', 'MEDICAL_POSTOP', 4, 1);
