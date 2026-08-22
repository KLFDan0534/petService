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

-- U4: 评价唯一约束 —— 同一订单同一用户同一维度仅允许一条评价（先清理历史重复，再建唯一索引）
-- 幂等：索引已存在时跳过（重复执行不再报 1061 Duplicate key name）。
DELETE t1 FROM rating_wsh t1
INNER JOIN rating_wsh t2
  ON t1.order_id_wsh <=> t2.order_id_wsh
 AND t1.user_id_wsh = t2.user_id_wsh
 AND t1.target_type_wsh = t2.target_type_wsh
 AND t1.deleted_wsh = 0 AND t2.deleted_wsh = 0
 AND t1.id_wsh < t2.id_wsh;
SET @u4_has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'rating_wsh'
                      AND index_name = 'uk_rating_order_user_type');
SET @u4_ddl := IF(@u4_has_idx = 0,
                  'ALTER TABLE rating_wsh ADD UNIQUE INDEX `uk_rating_order_user_type` (`order_id_wsh`, `user_id_wsh`, `target_type_wsh`)',
                  'SELECT ''uk_rating_order_user_type already exists, skip'' AS u4_note');
PREPARE u4_stmt FROM @u4_ddl;
EXECUTE u4_stmt;
DEALLOCATE PREPARE u4_stmt;

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
    `delivery_type_wsh` VARCHAR(32) DEFAULT 'notice' COMMENT '投递方式: notice-普通公告 popup-弹窗通知 broadcast-全员通知',
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

-- Order immutable snapshot table
CREATE TABLE IF NOT EXISTS `order_snapshot_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL COMMENT 'Order ID',
    `order_no_wsh` VARCHAR(50) NOT NULL COMMENT 'Order number',
    `owner_snapshot_wsh` TEXT COMMENT 'Owner snapshot JSON',
    `pet_snapshot_wsh` TEXT COMMENT 'Pet snapshot JSON',
    `merchant_snapshot_wsh` TEXT COMMENT 'Merchant snapshot JSON',
    `keeper_snapshot_wsh` TEXT COMMENT 'Keeper snapshot JSON',
    `service_snapshot_wsh` TEXT COMMENT 'Service snapshot JSON',
    `address_snapshot_wsh` TEXT COMMENT 'Address and fulfillment snapshot JSON',
    `price_snapshot_wsh` TEXT COMMENT 'Price snapshot JSON',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_order_snapshot_order` (`order_id_wsh`),
    INDEX `idx_order_snapshot_no` (`order_no_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Order immutable snapshot table';

-- Qualification proof table
CREATE TABLE IF NOT EXISTS `qualification_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `owner_type_wsh` VARCHAR(30) NOT NULL COMMENT 'merchant/keeper/adopter',
    `owner_id_wsh` BIGINT NOT NULL COMMENT 'Business owner ID',
    `user_id_wsh` BIGINT COMMENT 'Submitting user ID',
    `qual_type_wsh` VARCHAR(50) NOT NULL COMMENT 'Qualification type',
    `title_wsh` VARCHAR(100) COMMENT 'Display title',
    `file_url_wsh` VARCHAR(1000) COMMENT 'Qualification image URL',
    `summary_wsh` VARCHAR(500) COMMENT 'Summary',
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    `visibility_wsh` VARCHAR(20) DEFAULT 'masked_public' COMMENT 'private/masked_public/public',
    `reviewer_id_wsh` BIGINT COMMENT 'Reviewer ID',
    `review_remark_wsh` VARCHAR(500) COMMENT 'Review remark',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_qualification_owner` (`owner_type_wsh`, `owner_id_wsh`),
    INDEX `idx_qualification_user` (`user_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Qualification proof table';

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

ALTER TABLE ticket_wsh ADD COLUMN IF NOT EXISTS `result_wsh` VARCHAR(2000) COMMENT '处理结果' AFTER `status_wsh`;
ALTER TABLE ticket_wsh ADD COLUMN IF NOT EXISTS `merchant_id_wsh` BIGINT COMMENT 'Merchant ID' AFTER `id_wsh`;
ALTER TABLE ticket_wsh ADD COLUMN IF NOT EXISTS `order_id_wsh` BIGINT COMMENT 'Order ID' AFTER `merchant_id_wsh`;
ALTER TABLE complaint_wsh MODIFY COLUMN `order_id_wsh` BIGINT;
ALTER TABLE complaint_wsh ADD COLUMN IF NOT EXISTS `merchant_id_wsh` BIGINT COMMENT 'Merchant ID' AFTER `order_id_wsh`;

CREATE TABLE IF NOT EXISTS `merchant_customer_service_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id_wsh` BIGINT NOT NULL COMMENT 'Merchant ID',
    `user_id_wsh` BIGINT NOT NULL COMMENT 'Applicant/customer service user ID',
    `applicant_note_wsh` VARCHAR(500) COMMENT 'Application note',
    `review_note_wsh` VARCHAR(500) COMMENT 'Merchant review note',
    `status_wsh` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/resigned/terminated',
    `reviewer_id_wsh` BIGINT COMMENT 'Merchant reviewer user ID',
    `reviewed_at_wsh` DATETIME COMMENT 'Review time',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_mcs_merchant_user` (`merchant_id_wsh`, `user_id_wsh`),
    INDEX `idx_mcs_user_status` (`user_id_wsh`, `status_wsh`),
    INDEX `idx_mcs_merchant_status` (`merchant_id_wsh`, `status_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Merchant customer service applications';

ALTER TABLE notice_wsh ADD COLUMN IF NOT EXISTS `delivery_type_wsh` VARCHAR(32) DEFAULT '' COMMENT '投递方式: popup-弹窗通知 notification-消息通知(可组合,逗号分隔)' AFTER `type_wsh`;

-- Finance ledger compatibility columns.
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `balance_before_wsh` DECIMAL(12,2) COMMENT 'Balance before change' AFTER `amount_wsh`;
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `frozen_before_wsh` DECIMAL(12,2) COMMENT 'Frozen amount before change' AFTER `balance_after_wsh`;
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `frozen_after_wsh` DECIMAL(12,2) COMMENT 'Frozen amount after change' AFTER `frozen_before_wsh`;
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `direction_wsh` VARCHAR(20) COMMENT 'Ledger direction' AFTER `frozen_after_wsh`;
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `status_wsh` VARCHAR(20) DEFAULT 'success' COMMENT 'Ledger status' AFTER `direction_wsh`;
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `business_type_wsh` VARCHAR(50) COMMENT 'Business type' AFTER `status_wsh`;
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `business_id_wsh` VARCHAR(100) COMMENT 'Business id' AFTER `business_type_wsh`;
ALTER TABLE wallet_transaction_wsh ADD COLUMN IF NOT EXISTS `request_id_wsh` VARCHAR(120) COMMENT 'Idempotency request id' AFTER `business_id_wsh`;
ALTER TABLE refund_wsh ADD COLUMN IF NOT EXISTS `order_status_before_refund_wsh` VARCHAR(30) COMMENT 'Order status before refund' AFTER `status_wsh`;

-- Wallet unique constraint (run once; may error if duplicates exist, clean data first)
ALTER TABLE wallet_wsh ADD UNIQUE INDEX `uk_wallet_user` (`user_id_wsh`);
-- Wallet transaction constraints
ALTER TABLE wallet_transaction_wsh ADD UNIQUE INDEX `uk_wallet_tx_request` (`request_id_wsh`);
ALTER TABLE wallet_transaction_wsh ADD INDEX `idx_wallet_tx_user` (`user_id_wsh`);
ALTER TABLE wallet_transaction_wsh ADD INDEX `idx_wallet_tx_business` (`business_type_wsh`, `business_id_wsh`);
-- Payment unique constraint
ALTER TABLE payment_wsh ADD UNIQUE INDEX `uk_payment_pay_no` (`pay_no_wsh`);

