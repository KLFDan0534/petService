CREATE TABLE IF NOT EXISTS `user_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username_wsh` VARCHAR(50) NOT NULL UNIQUE,
    `password_wsh` VARCHAR(255) NOT NULL,
    `nickname_wsh` VARCHAR(50),
    `phone_wsh` VARCHAR(20),
    `avatar_wsh` VARCHAR(500),
    `email_wsh` VARCHAR(100),
    `address_wsh` VARCHAR(255),
    `latitude_wsh` DECIMAL(10, 6),
    `longitude_wsh` DECIMAL(10, 6),
    `status_wsh` TINYINT DEFAULT 1,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `role_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name_wsh` VARCHAR(50) NOT NULL UNIQUE,
    `code_wsh` VARCHAR(50) NOT NULL UNIQUE,
    `description_wsh` VARCHAR(255),
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `user_role_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `role_id_wsh` BIGINT NOT NULL,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_wsh` INT DEFAULT 0,
    UNIQUE KEY `uk_user_role` (`user_id_wsh`, `role_id_wsh`)
);

CREATE TABLE IF NOT EXISTS `category_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name_wsh` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id_wsh` BIGINT DEFAULT 0 COMMENT '父分类ID,0表示一级分类',
    `sort_order_wsh` INT DEFAULT 0 COMMENT '排序',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `pet_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `owner_id_wsh` BIGINT NOT NULL,
    `name_wsh` VARCHAR(50) NOT NULL,
    `type_wsh` VARCHAR(50) NOT NULL,
    `breed_wsh` VARCHAR(100),
    `age_wsh` INT,
    `weight_wsh` DECIMAL(10, 2),
    `gender_wsh` TINYINT,
    `sterilized_wsh` TINYINT DEFAULT 0,
    `vaccinated_wsh` TINYINT DEFAULT 0,
    `avatar_wsh` VARCHAR(500),
    `description_wsh` TEXT,
    `allergies_wsh` TEXT,
    `habits_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `merchant_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `name_wsh` VARCHAR(100) NOT NULL,
    `phone_wsh` VARCHAR(20),
    `address_wsh` VARCHAR(255),
    `latitude_wsh` DECIMAL(10, 6),
    `longitude_wsh` DECIMAL(10, 6),
    `description_wsh` TEXT,
    `business_license_wsh` VARCHAR(500),
    `rating_wsh` DECIMAL(3, 2) DEFAULT 5.00,
    `status_wsh` TINYINT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `keeper_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id_wsh` BIGINT NOT NULL,
    `user_id_wsh` BIGINT NOT NULL,
    `name_wsh` VARCHAR(50) NOT NULL,
    `phone_wsh` VARCHAR(20),
    `avatar_wsh` VARCHAR(500),
    `experience_years_wsh` INT DEFAULT 0,
    `rating_wsh` DECIMAL(3, 2) DEFAULT 5.00,
    `completion_rate_wsh` DECIMAL(5, 2) DEFAULT 100.00,
    `complaint_rate_wsh` DECIMAL(5, 2) DEFAULT 0.00,
    `price_per_day_wsh` DECIMAL(10, 2) NOT NULL,
    `max_pets_wsh` INT DEFAULT 5,
    `current_pets_wsh` INT DEFAULT 0,
    `status_wsh` TINYINT DEFAULT 1,
    `bio_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `pet_service_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id_wsh` BIGINT NOT NULL,
    `name_wsh` VARCHAR(100) NOT NULL,
    `type_wsh` VARCHAR(50),
    `description_wsh` TEXT,
    `price_wsh` DECIMAL(10, 2) NOT NULL,
    `unit_wsh` VARCHAR(20) DEFAULT 'day',
    `images_wsh` TEXT,
    `status_wsh` TINYINT DEFAULT 1,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `pet_order_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no_wsh` VARCHAR(50) NOT NULL UNIQUE,
    `owner_id_wsh` BIGINT NOT NULL,
    `pet_id_wsh` BIGINT NOT NULL,
    `keeper_id_wsh` BIGINT NOT NULL,
    `merchant_id_wsh` BIGINT NOT NULL,
    `service_id_wsh` BIGINT,
    `start_date_wsh` DATE NOT NULL,
    `end_date_wsh` DATE NOT NULL,
    `days_wsh` INT NOT NULL,
    `price_per_day_wsh` DECIMAL(10, 2) NOT NULL,
    `total_amount_wsh` DECIMAL(10, 2) NOT NULL,
    `discount_wsh` DECIMAL(10, 2) DEFAULT 0,
    `final_amount_wsh` DECIMAL(10, 2) NOT NULL,
    `status_wsh` VARCHAR(20) DEFAULT 'pending',
    `handover_code_wsh` VARCHAR(4),
    `delivery_address_wsh` VARCHAR(500),
    `delivery_time_wsh` DATETIME,
    `receiver_available_start_wsh` DATETIME,
    `receiver_available_end_wsh` DATETIME,
    `pickup_address_wsh` VARCHAR(500),
    `pickup_time_wsh` DATETIME,
    `delivered_at_wsh` DATETIME,
    `received_at_wsh` DATETIME,
    `started_at_wsh` DATETIME,
    `start_photo_wsh` VARCHAR(1000),
    `completed_at_wsh` DATETIME,
    `final_report_generated_wsh` TINYINT DEFAULT 0,
    `remark_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `payment_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `order_no_wsh` VARCHAR(50) NOT NULL,
    `pay_no_wsh` VARCHAR(100),
    `amount_wsh` DECIMAL(10, 2) NOT NULL,
    `method_wsh` VARCHAR(20) DEFAULT 'wechat',
    `status_wsh` VARCHAR(20) DEFAULT 'pending',
    `paid_at_wsh` DATETIME,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `refund_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `order_no_wsh` VARCHAR(50) NOT NULL,
    `amount_wsh` DECIMAL(10, 2) NOT NULL,
    `reason_wsh` TEXT,
    `status_wsh` VARCHAR(20) DEFAULT 'pending',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `complaint_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `owner_id_wsh` BIGINT NOT NULL,
    `target_id_wsh` BIGINT,
    `target_type_wsh` VARCHAR(20),
    `title_wsh` VARCHAR(200),
    `content_wsh` TEXT,
    `images_wsh` TEXT,
    `status_wsh` VARCHAR(20) DEFAULT 'pending',
    `result_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `chat_message_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `from_user_id_wsh` BIGINT NOT NULL,
    `to_user_id_wsh` BIGINT NOT NULL,
    `order_id_wsh` BIGINT,
    `content_wsh` TEXT,
    `type_wsh` VARCHAR(20) DEFAULT 'text',
    `file_url_wsh` VARCHAR(500),
    `read_wsh` TINYINT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `favorite_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `target_id_wsh` BIGINT NOT NULL,
    `target_type_wsh` VARCHAR(20) NOT NULL,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_target` (`user_id_wsh`, `target_id_wsh`, `target_type_wsh`)
);

CREATE TABLE IF NOT EXISTS `rating_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT,
    `user_id_wsh` BIGINT NOT NULL,
    `target_id_wsh` BIGINT NOT NULL,
    `target_type_wsh` VARCHAR(20) NOT NULL,
    `score_wsh` TINYINT NOT NULL,
    `content_wsh` TEXT,
    `images_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `reply_wsh` TEXT,
    `reply_at_wsh` DATETIME,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `ai_report_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `pet_id_wsh` BIGINT NOT NULL,
    `keeper_id_wsh` BIGINT NOT NULL,
    `content_wsh` TEXT,
    `type_wsh` VARCHAR(50),
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `knowledge_document_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title_wsh` VARCHAR(200) NOT NULL,
    `content_wsh` TEXT NOT NULL,
    `category_wsh` VARCHAR(50),
    `source_type_wsh` VARCHAR(20),
    `source_path_wsh` VARCHAR(500),
    `word_count_wsh` INT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO `role_wsh` (`name_wsh`, `code_wsh`, `description_wsh`) VALUES
('Admin', 'ADMIN', 'System Administrator'),
('Pet Owner', 'OWNER', 'Pet Owner'),
('Pet Keeper', 'KEEPER', 'Pet Boarding Keeper'),
('Merchant', 'MERCHANT', 'Merchant'),
('CS', 'CUSTOMER_SERVICE', 'Customer Service');

CREATE TABLE IF NOT EXISTS `document_embedding_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `document_id_wsh` BIGINT NOT NULL,
    `embedding_wsh` TEXT,
    `dimension_wsh` INT DEFAULT 0,
    `chunk_index_wsh` INT DEFAULT 0,
    `chunk_text_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO `knowledge_document_wsh` (`title_wsh`, `content_wsh`, `category_wsh`, `source_type_wsh`) VALUES
('宠物寄养规则', '1. 寄养前需确保宠物已完成疫苗接种\n2. 寄养期间宠物主需提供宠物食物\n3. 寄养员需每日提供宠物活动时间不少于2小时\n4. 寄养期间如宠物生病需及时通知宠物主\n5. 寄养期间宠物意外伤害由寄养员负责\n6. 寄养期间宠物死亡由寄养员全责赔偿\n7. 寄养前需签订寄养协议\n8. 寄养费用按天计算，不足一天按一天计算', 'boarding', 'txt'),
('退款规则', '1. 订单支付后24小时内可全额退款\n2. 寄养开始前3天退款扣除10%手续费\n3. 寄养开始前1天退款扣除30%手续费\n4. 寄养开始后不接受退款\n5. 因平台原因导致无法服务的，全额退款\n6. 因商家原因取消订单的，全额退款并赔偿20%\n7. 退款到账时间：1-3个工作日\n8. 部分退款按实际未服务天数计算', 'refund', 'txt'),
('投诉规则', '1. 投诉需在服务结束后7天内提出\n2. 投诉需要提供相关证据（照片/视频）\n3. 平台客服将在24小时内受理投诉\n4. 投诉处理周期为3-5个工作日\n5. 严重投诉升级至平台管理员处理\n6. 恶意投诉将影响用户信用分\n7. 投诉处理结果将以站内信形式通知\n8. 对处理结果不满可再次申诉', 'complaint', 'txt'),
('宠物护理指南', '1. 每日定时喂食，保持饮食规律\n2. 提供充足的饮用水\n3. 每日遛狗不少于2次\n4. 保持居住环境清洁卫生\n5. 定期梳毛，防止打结\n6. 注意观察宠物精神状态\n7. 发现异常及时记录并通知主人\n8. 按主人要求给药（如有）', 'care', 'txt'),
('疫苗要求', '1. 寄养宠物必须完成核心疫苗接种\n2. 犬类：狂犬病疫苗、犬瘟热疫苗、细小病毒疫苗\n3. 猫类：狂犬病疫苗、猫三联疫苗\n4. 疫苗需在有效期内\n5. 需提供疫苗接种证明\n6. 未接种疫苗的宠物需加收健康管理费\n7. 建议接种流感疫苗（季节性）\n8. 老年宠物建议做健康检查', 'vaccine', 'txt');

CREATE TABLE IF NOT EXISTS `address_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `label_wsh` VARCHAR(50) DEFAULT '',
    `name_wsh` VARCHAR(50) NOT NULL,
    `phone_wsh` VARCHAR(20) NOT NULL,
    `address_wsh` VARCHAR(500) NOT NULL,
    `detail_wsh` VARCHAR(500) DEFAULT '',
    `latitude_wsh` DECIMAL(10, 7),
    `longitude_wsh` DECIMAL(10, 7),
    `is_default_wsh` TINYINT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Care Record Table
CREATE TABLE IF NOT EXISTS `care_record_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `pet_id_wsh` BIGINT,
    `keeper_id_wsh` BIGINT,
    `type_wsh` VARCHAR(20) DEFAULT 'feed',
    `content_wsh` TEXT,
    `images_wsh` VARCHAR(2000),
    `record_time_wsh` DATETIME,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Finance: Wallet
CREATE TABLE IF NOT EXISTS `wallet_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `balance_wsh` DECIMAL(12,2) DEFAULT 0.00,
    `frozen_amount_wsh` DECIMAL(12,2) DEFAULT 0.00,
    `version_wsh` INT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Finance: Wallet Transaction
CREATE TABLE IF NOT EXISTS `wallet_transaction_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `wallet_id_wsh` BIGINT,
    `user_id_wsh` BIGINT NOT NULL,
    `type_wsh` VARCHAR(20) DEFAULT 'income',
    `amount_wsh` DECIMAL(12,2) NOT NULL,
    `balance_after_wsh` DECIMAL(12,2),
    `order_id_wsh` BIGINT,
    `description_wsh` VARCHAR(500),
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Finance: Withdrawal
CREATE TABLE IF NOT EXISTS `withdrawal_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `amount_wsh` DECIMAL(12,2) NOT NULL,
    `fee_wsh` DECIMAL(12,2) DEFAULT 0.00,
    `actual_amount_wsh` DECIMAL(12,2),
    `bank_name_wsh` VARCHAR(100),
    `bank_card_wsh` VARCHAR(100),
    `account_name_wsh` VARCHAR(100),
    `status_wsh` VARCHAR(20) DEFAULT 'pending',
    `remark_wsh` VARCHAR(500),
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Notice / Banner Table
CREATE TABLE IF NOT EXISTS `notice_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title_wsh` VARCHAR(200) NOT NULL,
    `content_wsh` TEXT,
    `type_wsh` VARCHAR(20) DEFAULT 'notice',
    `image_url_wsh` VARCHAR(500),
    `link_url_wsh` VARCHAR(500),
    `sort_order_wsh` INT DEFAULT 0,
    `status_wsh` TINYINT DEFAULT 1,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `notice_read_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `notice_id_wsh` BIGINT NOT NULL,
    `user_id_wsh` BIGINT NOT NULL,
    `read_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_notice_read_notice_user` (`notice_id_wsh`, `user_id_wsh`)
);

CREATE TABLE IF NOT EXISTS `notification_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `title_wsh` VARCHAR(200) NOT NULL,
    `content_wsh` TEXT,
    `type_wsh` VARCHAR(30),
    `is_read_wsh` TINYINT DEFAULT 0,
    `related_id_wsh` BIGINT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `file_record_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `original_name_wsh` VARCHAR(255),
    `object_name_wsh` VARCHAR(500) NOT NULL,
    `size_wsh` BIGINT,
    `content_type_wsh` VARCHAR(100),
    `user_id_wsh` BIGINT,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Ticket System
CREATE TABLE IF NOT EXISTS `ticket_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `title_wsh` VARCHAR(200) NOT NULL,
    `content_wsh` TEXT,
    `category_wsh` VARCHAR(50),
    `priority_wsh` VARCHAR(20) DEFAULT 'medium',
    `status_wsh` VARCHAR(20) DEFAULT 'pending',
    `assignee_id_wsh` BIGINT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `ticket_message_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ticket_id_wsh` BIGINT NOT NULL,
    `user_id_wsh` BIGINT NOT NULL,
    `content_wsh` TEXT NOT NULL,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Content Review Table
CREATE TABLE IF NOT EXISTS `content_review_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `target_type_wsh` VARCHAR(50) NOT NULL,
    `target_id_wsh` BIGINT NOT NULL,
    `reporter_id_wsh` BIGINT,
    `reason_wsh` VARCHAR(500),
    `status_wsh` VARCHAR(20) DEFAULT 'pending',
    `reviewer_id_wsh` BIGINT,
    `review_remark_wsh` VARCHAR(500),
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `tip_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `from_user_id_wsh` BIGINT NOT NULL,
    `to_user_id_wsh` BIGINT NOT NULL,
    `amount_wsh` DECIMAL(10, 2) NOT NULL,
    `message_wsh` VARCHAR(200),
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Operation Log Table
CREATE TABLE IF NOT EXISTS `operation_log_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT,
    `username_wsh` VARCHAR(50),
    `module_wsh` VARCHAR(50),
    `operation_wsh` VARCHAR(50),
    `description_wsh` VARCHAR(500),
    `method_wsh` VARCHAR(10),
    `request_url_wsh` VARCHAR(500),
    `request_params_wsh` TEXT,
    `request_body_wsh` TEXT,
    `response_body_wsh` TEXT,
    `ip_address_wsh` VARCHAR(50),
    `duration_wsh` BIGINT,
    `status_wsh` TINYINT DEFAULT 1,
    `error_msg_wsh` VARCHAR(2000),
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Business Hours Table
CREATE TABLE IF NOT EXISTS `business_hours_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id_wsh` BIGINT NOT NULL,
    `day_of_week_wsh` TINYINT NOT NULL,
    `open_time_wsh` TIME,
    `close_time_wsh` TIME,
    `is_closed_wsh` TINYINT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_merchant_day` (`merchant_id_wsh`, `day_of_week_wsh`)
);
