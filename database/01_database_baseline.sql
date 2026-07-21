-- ===================================================================
-- Pet Service Database Baseline
-- 生成日期: 2026-06-28
-- 数据库: pet_service
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_0900_ai_ci
-- 引擎: InnoDB
--
-- 可信来源: 生产数据库 (MySQL 8.0.46)
-- 表数量: 35
-- 字段总数: 368
-- ===================================================================

CREATE DATABASE IF NOT EXISTS `pet_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `pet_service`;

SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================================
-- 1. 系统模块 - user_wsh (用户表)
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
  `address_wsh` varchar(255) DEFAULT NULL COMMENT '地址',
  `latitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `gender_wsh` tinyint DEFAULT '0' COMMENT '性别: 0-未知 1-男 2-女',
  `real_name_wsh` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `id_card_no_wsh` varchar(32) DEFAULT NULL COMMENT '身份证号',
  `real_name_status_wsh` tinyint DEFAULT '0' COMMENT '实名状态: 0-未认证 1-待审核 2-已认证 3-已驳回',
  `reject_reason_wsh` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `username_wsh` (`username_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ===================================================================
-- 2. 系统模块 - role_wsh (角色表)
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
-- 3. 系统模块 - user_role_wsh (用户角色关联表)
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
-- 4. 宠物模块 - pet_wsh (宠物表)
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
-- 5. 宠物模块 - category_wsh (分类表)
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类表';

-- ===================================================================
-- 6. 宠物模块 - care_record_wsh (照护记录表)
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
-- 7. 寄养模块 - merchant_wsh (商家表)
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
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '商家头像',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家表';

-- ===================================================================
-- 8. 寄养模块 - keeper_wsh (看护人表)
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
  `bio_wsh` text COMMENT '简介',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='看护人表';

-- ===================================================================
-- 9. 寄养模块 - pet_service_wsh (服务项目表)
-- ===================================================================
DROP TABLE IF EXISTS `pet_service_wsh`;
CREATE TABLE `pet_service_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `name_wsh` varchar(100) NOT NULL COMMENT '服务名称',
  `type_wsh` varchar(50) DEFAULT NULL COMMENT '类型: boarding/grooming/training/walk',
  `description_wsh` text COMMENT '描述',
  `price_wsh` decimal(10,2) NOT NULL COMMENT '价格',
  `unit_wsh` varchar(20) DEFAULT 'day' COMMENT '单位',
  `images_wsh` text COMMENT '图片URL',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-下架 1-上架',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务项目表';

-- ===================================================================
-- 10. 寄养模块 - address_wsh (地址表)
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
-- 11. 寄养模块 - business_hours_wsh (营业时间表)
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
-- 12. 订单模块 - pet_order_wsh (订单表)
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
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/paid/in_progress/completed/cancelled/refunding/refunded',
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
-- 13. 订单模块 - payment_wsh (支付表)
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
-- 14. 订单模块 - refund_wsh (退款表)
-- ===================================================================
DROP TABLE IF EXISTS `refund_wsh`;
CREATE TABLE `refund_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '退款ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `order_no_wsh` varchar(50) NOT NULL COMMENT '订单号',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '退款金额',
  `reason_wsh` text COMMENT '退款原因',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/approved/rejected/completed',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款表';

-- ===================================================================
-- 15. 订单模块 - tip_wsh (打赏表)
-- ===================================================================
DROP TABLE IF EXISTS `tip_wsh`;
CREATE TABLE `tip_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '打赏ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `from_user_id_wsh` bigint NOT NULL COMMENT '打赏用户',
  `to_user_id_wsh` bigint NOT NULL COMMENT '接收用户',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '打赏金额',
  `message_wsh` varchar(200) DEFAULT NULL COMMENT '打赏留言',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏记录表';

-- ===================================================================
-- 16. 客服模块 - chat_message_wsh (聊天消息表)
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
-- 17. 客服模块 - complaint_wsh (投诉表)
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
-- 18. 客服模块 - rating_wsh (评价表)
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
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价表';

-- ===================================================================
-- 19. 客服模块 - ticket_wsh (工单表)
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
  `result_wsh` varchar(2000) DEFAULT NULL COMMENT '处理结果',
  `assignee_id_wsh` bigint DEFAULT NULL COMMENT '处理人ID',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服工单表';

-- ===================================================================
-- 20. 客服模块 - ticket_message_wsh (工单消息表)
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
-- 21. 财务模块 - wallet_wsh (钱包表)
-- ===================================================================
DROP TABLE IF EXISTS `wallet_wsh`;
CREATE TABLE `wallet_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `balance_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '余额',
  `frozen_amount_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '冻结金额',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包表';

-- ===================================================================
-- 22. 财务模块 - wallet_transaction_wsh (交易流水表)
-- ===================================================================
DROP TABLE IF EXISTS `wallet_transaction_wsh`;
CREATE TABLE `wallet_transaction_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `wallet_id_wsh` bigint DEFAULT NULL COMMENT '钱包ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `type_wsh` varchar(20) DEFAULT 'income' COMMENT '类型: income-收入 expense-支出 withdraw-提现 refund-退款',
  `amount_wsh` decimal(12,2) NOT NULL COMMENT '金额',
  `balance_after_wsh` decimal(12,2) DEFAULT NULL COMMENT '变动后余额',
  `order_id_wsh` bigint DEFAULT NULL COMMENT '关联订单',
  `description_wsh` varchar(500) DEFAULT NULL COMMENT '描述',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易流水表';

-- ===================================================================
-- 23. 财务模块 - withdrawal_wsh (提现记录表)
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
-- 24. 运营模块 - notice_wsh (公告表)
-- ===================================================================
DROP TABLE IF EXISTS `notice_wsh`;
CREATE TABLE `notice_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title_wsh` varchar(200) NOT NULL COMMENT '标题',
  `content_wsh` text COMMENT '内容',
  `type_wsh` varchar(20) DEFAULT 'notice' COMMENT '类型: notice-公告 banner-Banner',
  `delivery_type_wsh` varchar(32) DEFAULT 'notice' COMMENT '投递方式: popup-弹窗通知 notification-消息通知(可组合,逗号分隔)',
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
-- 25. 运营模块 - notice_read_wsh (公告已读表)
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
-- 26. 运营模块 - notification_wsh (通知表)
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
-- 27. 运营模块 - operation_log_wsh (操作日志表)
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
-- 28. 运营模块 - file_record_wsh (文件记录表)
-- ===================================================================
DROP TABLE IF EXISTS `file_record_wsh`;
CREATE TABLE `file_record_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name_wsh` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `object_name_wsh` varchar(255) DEFAULT NULL COMMENT '存储对象名',
  `size_wsh` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `content_type_wsh` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `user_id_wsh` bigint DEFAULT NULL COMMENT '上传用户ID',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user` (`user_id_wsh`) COMMENT '用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件上传记录表';

-- ===================================================================
-- 29. 运营模块 - favorite_wsh (收藏表)
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
-- 30. 运营模块 - content_review_wsh (内容审核表)
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
-- 31. 运营模块 - recycle_bin_wsh (回收站表) - 需要建但数据库可能不存在
-- ===================================================================
-- 注意: recycle_bin_wsh 在数据库中不存在，RecycleBinMapper使用动态表名直接操作各表的deleted_wsh字段
-- 这是一个虚拟概念，不需要独立的表

-- ===================================================================
-- 32. 领养模块 - adoption_pet_wsh (领养宠物表)
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
-- 33. 领养模块 - adoption_application_wsh (领养申请表)
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
-- 34. AI模块 - ai_report_wsh (AI报告表)
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
-- 35. AI模块 - knowledge_document_wsh (知识库文档表)
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
-- 36. AI模块 - document_embedding_wsh (文档嵌入表)
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
