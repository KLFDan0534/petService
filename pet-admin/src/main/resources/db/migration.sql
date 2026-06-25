CREATE TABLE IF NOT EXISTS `category_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name_wsh` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id_wsh` BIGINT DEFAULT 0 COMMENT '父分类ID,0表示一级分类',
    `sort_order_wsh` INT DEFAULT 0 COMMENT '排序',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ALTER TABLE keeper_wsh ADD COLUMN IF NOT EXISTS `bio_wsh` TEXT COMMENT '简介' AFTER `status_wsh`;

ALTER TABLE rating_wsh MODIFY COLUMN `order_id_wsh` BIGINT COMMENT '订单ID(服务评价可为空)';

CREATE TABLE IF NOT EXISTS `address_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL COMMENT '用户ID',
    `label_wsh` VARCHAR(50) DEFAULT '' COMMENT '标签(家/公司/学校)',
    `name_wsh` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `phone_wsh` VARCHAR(20) NOT NULL COMMENT '收货人电话',
    `address_wsh` VARCHAR(500) NOT NULL COMMENT '详细地址',
    `detail_wsh` VARCHAR(500) DEFAULT '' COMMENT '门牌号等补充信息',
    `latitude_wsh` DECIMAL(10, 7) COMMENT '纬度',
    `longitude_wsh` DECIMAL(10, 7) COMMENT '经度',
    `is_default_wsh` TINYINT DEFAULT 0 COMMENT '是否默认 0-否 1-是',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址';

CREATE TABLE IF NOT EXISTS `notice_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title_wsh` VARCHAR(200) NOT NULL COMMENT '标题',
    `content_wsh` TEXT COMMENT '内容',
    `type_wsh` VARCHAR(20) DEFAULT 'notice' COMMENT '类型: notice-公告 banner-Banner',
    `image_url_wsh` VARCHAR(500) COMMENT '图片URL',
    `link_url_wsh` VARCHAR(500) COMMENT '跳转链接',
    `sort_order_wsh` INT DEFAULT 0 COMMENT '排序',
    `status_wsh` TINYINT DEFAULT 1 COMMENT '状态 1-显示 0-隐藏',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告与Banner';

CREATE TABLE IF NOT EXISTS `notice_read_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `notice_id_wsh` BIGINT NOT NULL COMMENT '公告ID',
    `user_id_wsh` BIGINT NOT NULL COMMENT '用户ID',
    `read_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_notice_read_notice_user` (`notice_id_wsh`, `user_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读记录';

CREATE TABLE IF NOT EXISTS `care_record_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL COMMENT '订单ID',
    `pet_id_wsh` BIGINT COMMENT '宠物ID',
    `keeper_id_wsh` BIGINT COMMENT '寄养员ID',
    `type_wsh` VARCHAR(20) DEFAULT 'feed' COMMENT '类型: feed-喂食 activity-活动 medication-用药 health-健康',
    `content_wsh` TEXT COMMENT '记录内容',
    `images_wsh` VARCHAR(2000) COMMENT '图片URL(逗号分隔)',
    `record_time_wsh` DATETIME COMMENT '记录时间',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寄养过程记录';

CREATE TABLE IF NOT EXISTS `wallet_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL COMMENT '用户ID',
    `balance_wsh` DECIMAL(12,2) DEFAULT 0.00 COMMENT '余额',
    `frozen_amount_wsh` DECIMAL(12,2) DEFAULT 0.00 COMMENT '冻结金额',
    `version_wsh` INT DEFAULT 0 COMMENT '乐观锁',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户钱包';

CREATE TABLE IF NOT EXISTS `wallet_transaction_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `wallet_id_wsh` BIGINT COMMENT '钱包ID',
    `user_id_wsh` BIGINT NOT NULL COMMENT '用户ID',
    `type_wsh` VARCHAR(20) DEFAULT 'income' COMMENT '类型: income-收入 expense-支出 withdraw-提现 refund-退款',
    `amount_wsh` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `balance_after_wsh` DECIMAL(12,2) COMMENT '变动后余额',
    `order_id_wsh` BIGINT COMMENT '关联订单',
    `description_wsh` VARCHAR(500) COMMENT '描述',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水';

CREATE TABLE IF NOT EXISTS `withdrawal_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL COMMENT '用户ID',
    `amount_wsh` DECIMAL(12,2) NOT NULL COMMENT '提现金额',
    `fee_wsh` DECIMAL(12,2) DEFAULT 0.00 COMMENT '手续费',
    `actual_amount_wsh` DECIMAL(12,2) COMMENT '实际到账',
    `bank_name_wsh` VARCHAR(100) COMMENT '银行名称',
    `bank_card_wsh` VARCHAR(100) COMMENT '银行卡号',
    `account_name_wsh` VARCHAR(100) COMMENT '持卡人',
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending-待审核 approved-已通过 rejected-已驳回 completed-已完成',
    `remark_wsh` VARCHAR(500) COMMENT '备注',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现记录';

CREATE TABLE IF NOT EXISTS `ticket_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL COMMENT '创建用户ID',
    `title_wsh` VARCHAR(200) NOT NULL COMMENT '工单标题',
    `content_wsh` TEXT COMMENT '工单内容',
    `category_wsh` VARCHAR(50) COMMENT '分类: complaint-投诉 question-咨询 suggestion-建议 other-其他',
    `priority_wsh` VARCHAR(20) DEFAULT 'medium' COMMENT '优先级: low/medium/high/urgent',
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending-待处理 processing-处理中 resolved-已解决 closed-已关闭',
    `assignee_id_wsh` BIGINT COMMENT '处理人ID',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单';

CREATE TABLE IF NOT EXISTS `ticket_message_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ticket_id_wsh` BIGINT NOT NULL COMMENT '工单ID',
    `user_id_wsh` BIGINT NOT NULL COMMENT '发送人ID',
    `content_wsh` TEXT NOT NULL COMMENT '消息内容',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单消息';

CREATE TABLE IF NOT EXISTS `content_review_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `target_type_wsh` VARCHAR(50) NOT NULL COMMENT '目标类型: rating/complaint/chat/等',
    `target_id_wsh` BIGINT NOT NULL COMMENT '目标ID',
    `reporter_id_wsh` BIGINT COMMENT '举报人',
    `reason_wsh` VARCHAR(500) COMMENT '举报原因',
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending-待审核 approved-已通过 rejected-已驳回',
    `reviewer_id_wsh` BIGINT COMMENT '审核人',
    `review_remark_wsh` VARCHAR(500) COMMENT '审核备注',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核';

CREATE TABLE IF NOT EXISTS `tip_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL COMMENT '订单ID',
    `from_user_id_wsh` BIGINT NOT NULL COMMENT '打赏用户',
    `to_user_id_wsh` BIGINT NOT NULL COMMENT '接收用户',
    `amount_wsh` DECIMAL(10, 2) NOT NULL COMMENT '打赏金额',
    `message_wsh` VARCHAR(200) COMMENT '打赏留言',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打赏记录';

-- Operation Log Table
CREATE TABLE IF NOT EXISTS `operation_log_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT COMMENT '用户ID',
    `username_wsh` VARCHAR(50) COMMENT '用户名',
    `module_wsh` VARCHAR(50) COMMENT '模块',
    `operation_wsh` VARCHAR(50) COMMENT '操作类型',
    `description_wsh` VARCHAR(500) COMMENT '操作描述',
    `method_wsh` VARCHAR(10) COMMENT '请求方法',
    `request_url_wsh` VARCHAR(500) COMMENT '请求URL',
    `request_params_wsh` TEXT COMMENT '请求参数',
    `request_body_wsh` TEXT COMMENT '请求体',
    `response_body_wsh` TEXT COMMENT '响应体',
    `ip_address_wsh` VARCHAR(50) COMMENT 'IP地址',
    `duration_wsh` BIGINT COMMENT '耗时(ms)',
    `status_wsh` TINYINT DEFAULT 1 COMMENT '状态 1-成功 0-失败',
    `error_msg_wsh` VARCHAR(2000) COMMENT '错误信息',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id_wsh`),
    KEY `idx_module` (`module_wsh`),
    KEY `idx_created_at` (`created_at_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- Notification table
CREATE TABLE IF NOT EXISTS `notification_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL COMMENT 'Receiver user ID',
    `title_wsh` VARCHAR(200) NOT NULL COMMENT 'Notification title',
    `content_wsh` TEXT COMMENT 'Notification content',
    `type_wsh` VARCHAR(30) COMMENT 'Type: order/payment/ticket/system',
    `is_read_wsh` TINYINT DEFAULT 0 COMMENT 'Is read 1=yes',
    `related_id_wsh` BIGINT COMMENT 'Related business ID',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (`user_id_wsh`),
    INDEX idx_read (`is_read_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification table';

-- Business Hours table
CREATE TABLE IF NOT EXISTS `business_hours_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id_wsh` BIGINT NOT NULL COMMENT 'Merchant ID',
    `day_of_week_wsh` TINYINT NOT NULL COMMENT 'Day of week (1-7)',
    `open_time_wsh` TIME NOT NULL COMMENT 'Open time',
    `close_time_wsh` TIME NOT NULL COMMENT 'Close time',
    `is_closed_wsh` TINYINT DEFAULT 0 COMMENT 'Is closed 1=yes',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_day (`merchant_id_wsh`, `day_of_week_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Business hours table';

-- Adoption Pet table
CREATE TABLE IF NOT EXISTS `adoption_pet_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id_wsh` BIGINT NOT NULL COMMENT 'Merchant ID',
    `name_wsh` VARCHAR(50) NOT NULL COMMENT 'Pet name',
    `type_wsh` VARCHAR(30) COMMENT 'Pet type',
    `breed_wsh` VARCHAR(100) COMMENT 'Breed',
    `age_wsh` INT COMMENT 'Age (months)',
    `gender_wsh` VARCHAR(10) COMMENT 'Gender',
    `weight_wsh` DECIMAL(5,2) COMMENT 'Weight (kg)',
    `color_wsh` VARCHAR(50) COMMENT 'Color',
    `health_status_wsh` VARCHAR(500) COMMENT 'Health status',
    `vaccinated_wsh` TINYINT DEFAULT 0 COMMENT 'Vaccinated',
    `sterilized_wsh` TINYINT DEFAULT 0 COMMENT 'Sterilized',
    `personality_wsh` VARCHAR(500) COMMENT 'Personality',
    `story_wsh` TEXT COMMENT 'Rescue story',
    `adoption_requirements_wsh` TEXT COMMENT 'Adoption requirements',
    `adoption_fee_wsh` DECIMAL(10,2) DEFAULT 0 COMMENT 'Adoption fee',
    `cover_image_wsh` VARCHAR(500) COMMENT 'Cover image',
    `images_wsh` VARCHAR(2000) COMMENT 'Images (comma separated)',
    `status_wsh` VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT 'Status: AVAILABLE/APPLIED/ADOPTED',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (`merchant_id_wsh`),
    INDEX idx_status (`status_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Adoption pet table';

-- Adoption Application table
CREATE TABLE IF NOT EXISTS `adoption_application_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL COMMENT 'Applicant user ID',
    `pet_id_wsh` BIGINT NOT NULL COMMENT 'Adoption pet ID',
    `merchant_id_wsh` BIGINT NOT NULL COMMENT 'Merchant ID',
    `applicant_name_wsh` VARCHAR(50) NOT NULL COMMENT 'Applicant name',
    `applicant_phone_wsh` VARCHAR(20) NOT NULL COMMENT 'Applicant phone',
    `applicant_address_wsh` VARCHAR(500) NOT NULL COMMENT 'Home address',
    `housing_type_wsh` VARCHAR(50) COMMENT 'Housing type',
    `has_yard_wsh` TINYINT DEFAULT 0 COMMENT 'Has yard',
    `family_members_wsh` VARCHAR(200) COMMENT 'Family members',
    `pet_experience_wsh` TEXT COMMENT 'Pet experience',
    `reason_wsh` TEXT NOT NULL COMMENT 'Adoption reason',
    `economic_condition_wsh` VARCHAR(200) COMMENT 'Economic condition',
    `agree_visit_wsh` TINYINT DEFAULT 0 COMMENT 'Agree to visit',
    `merchant_status_wsh` VARCHAR(20) COMMENT 'Merchant review status',
    `merchant_remark_wsh` VARCHAR(500) COMMENT 'Merchant review remark',
    `admin_status_wsh` VARCHAR(20) COMMENT 'Admin review status',
    `admin_remark_wsh` VARCHAR(500) COMMENT 'Admin review remark',
    `status_wsh` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'Overall status',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (`user_id_wsh`),
    INDEX idx_pet (`pet_id_wsh`),
    INDEX idx_status (`status_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Adoption application table';

-- File Record table
CREATE TABLE IF NOT EXISTS `file_record_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `original_name_wsh` VARCHAR(255) COMMENT 'Original file name',
    `object_name_wsh` VARCHAR(255) COMMENT 'Storage object name',
    `size_wsh` BIGINT COMMENT 'File size (bytes)',
    `content_type_wsh` VARCHAR(100) COMMENT 'MIME content type',
    `user_id_wsh` BIGINT COMMENT 'Uploader user ID',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (`user_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='File upload record table';

-- Fix double-encoded UTF-8 Chinese data (UTF-8 bytes stored via latin1 connection)
UPDATE user_wsh SET nickname_wsh = CONVERT(UNHEX(HEX(CONVERT(nickname_wsh USING latin1))) USING utf8mb4), address_wsh = CONVERT(UNHEX(HEX(CONVERT(address_wsh USING latin1))) USING utf8mb4) WHERE CONVERT(nickname_wsh USING latin1) NOT LIKE '%?%' OR CONVERT(address_wsh USING latin1) NOT LIKE '%?%';
UPDATE merchant_wsh SET name_wsh = CONVERT(UNHEX(HEX(CONVERT(name_wsh USING latin1))) USING utf8mb4), address_wsh = CONVERT(UNHEX(HEX(CONVERT(address_wsh USING latin1))) USING utf8mb4), description_wsh = CONVERT(UNHEX(HEX(CONVERT(description_wsh USING latin1))) USING utf8mb4) WHERE CONVERT(name_wsh USING latin1) NOT LIKE '%?%';
UPDATE keeper_wsh SET name_wsh = CONVERT(UNHEX(HEX(CONVERT(name_wsh USING latin1))) USING utf8mb4) WHERE CONVERT(name_wsh USING latin1) NOT LIKE '%?%';
UPDATE role_wsh SET name_wsh = CONVERT(UNHEX(HEX(CONVERT(name_wsh USING latin1))) USING utf8mb4), description_wsh = CONVERT(UNHEX(HEX(CONVERT(description_wsh USING latin1))) USING utf8mb4) WHERE CONVERT(name_wsh USING latin1) NOT LIKE '%?%' OR CONVERT(description_wsh USING latin1) NOT LIKE '%?%';
UPDATE knowledge_document_wsh SET title_wsh = CONVERT(UNHEX(HEX(CONVERT(title_wsh USING latin1))) USING utf8mb4), content_wsh = CONVERT(UNHEX(HEX(CONVERT(content_wsh USING latin1))) USING utf8mb4) WHERE CONVERT(title_wsh USING latin1) NOT LIKE '%?%';
DELETE FROM merchant_wsh WHERE id_wsh > 1 AND HEX(name_wsh) = '3F3F3F3F3F3F3F3F';
DELETE FROM keeper_wsh WHERE HEX(name_wsh) = '3F3F3F';
DELETE FROM user_wsh WHERE HEX(nickname_wsh) = '3F3F3F3F' OR HEX(nickname_wsh) = '3F3F3F3F3F';

-- Seed admin test orders (safe: SELECT returns 0 rows if admin doesn't exist yet)
INSERT IGNORE INTO pet_wsh (owner_id_wsh, name_wsh, type_wsh, breed_wsh, age_wsh, weight_wsh, gender_wsh, sterilized_wsh, vaccinated_wsh, description_wsh, created_at_wsh)
SELECT id_wsh, 'AdminPet', 'Dog', 'Teddy', 2, 6.0, 1, 1, 1, 'Admin测试用宠物', NOW() FROM user_wsh WHERE username_wsh = 'admin';
INSERT IGNORE INTO pet_order_wsh (order_no_wsh, owner_id_wsh, pet_id_wsh, keeper_id_wsh, merchant_id_wsh, service_id_wsh, start_date_wsh, end_date_wsh, days_wsh, price_per_day_wsh, total_amount_wsh, final_amount_wsh, status_wsh, created_at_wsh)
SELECT 'ORD_ADMIN_PENDING', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, CURDATE() + INTERVAL 1 DAY, CURDATE() + INTERVAL 3 DAY, 2, 88.00, 176.00, 176.00, 'pending', NOW()
FROM user_wsh u, pet_wsh p, keeper_wsh k, merchant_wsh m, pet_service_wsh s WHERE u.username_wsh = 'admin' AND p.name_wsh = 'AdminPet' LIMIT 1;
INSERT IGNORE INTO pet_order_wsh (order_no_wsh, owner_id_wsh, pet_id_wsh, keeper_id_wsh, merchant_id_wsh, service_id_wsh, start_date_wsh, end_date_wsh, days_wsh, price_per_day_wsh, total_amount_wsh, final_amount_wsh, status_wsh, created_at_wsh)
SELECT 'ORD_ADMIN_PAID', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 6 DAY, 4, 88.00, 352.00, 352.00, 'paid', NOW()
FROM user_wsh u, pet_wsh p, keeper_wsh k, merchant_wsh m, pet_service_wsh s WHERE u.username_wsh = 'admin' AND p.name_wsh = 'AdminPet' LIMIT 1;
INSERT IGNORE INTO pet_order_wsh (order_no_wsh, owner_id_wsh, pet_id_wsh, keeper_id_wsh, merchant_id_wsh, service_id_wsh, start_date_wsh, end_date_wsh, days_wsh, price_per_day_wsh, total_amount_wsh, final_amount_wsh, status_wsh, created_at_wsh)
SELECT 'ORD_ADMIN_CONFIRMED', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 5 DAY, 2, 88.00, 176.00, 176.00, 'confirmed', NOW()
FROM user_wsh u, pet_wsh p, keeper_wsh k, merchant_wsh m, pet_service_wsh s WHERE u.username_wsh = 'admin' AND p.name_wsh = 'AdminPet' LIMIT 1;
INSERT IGNORE INTO pet_order_wsh (order_no_wsh, owner_id_wsh, pet_id_wsh, keeper_id_wsh, merchant_id_wsh, service_id_wsh, start_date_wsh, end_date_wsh, days_wsh, price_per_day_wsh, total_amount_wsh, final_amount_wsh, status_wsh, created_at_wsh)
SELECT 'ORD_ADMIN_INPROGRESS', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, CURDATE(), CURDATE() + INTERVAL 3 DAY, 3, 88.00, 264.00, 264.00, 'in_progress', NOW()
FROM user_wsh u, pet_wsh p, keeper_wsh k, merchant_wsh m, pet_service_wsh s WHERE u.username_wsh = 'admin' AND p.name_wsh = 'AdminPet' LIMIT 1;
INSERT IGNORE INTO pet_order_wsh (order_no_wsh, owner_id_wsh, pet_id_wsh, keeper_id_wsh, merchant_id_wsh, service_id_wsh, start_date_wsh, end_date_wsh, days_wsh, price_per_day_wsh, total_amount_wsh, final_amount_wsh, status_wsh, created_at_wsh)
SELECT 'ORD_ADMIN_COMPLETED', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 2 DAY, 3, 88.00, 264.00, 264.00, 'completed', NOW()
FROM user_wsh u, pet_wsh p, keeper_wsh k, merchant_wsh m, pet_service_wsh s WHERE u.username_wsh = 'admin' AND p.name_wsh = 'AdminPet' LIMIT 1;

