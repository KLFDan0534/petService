-- Pet Service Database Schema

CREATE DATABASE IF NOT EXISTS pet_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pet_service;

-- User Table
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
    `status_wsh` TINYINT DEFAULT 1 COMMENT '0-disabled 1-enabled',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Role Table
CREATE TABLE IF NOT EXISTS `role_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name_wsh` VARCHAR(50) NOT NULL UNIQUE,
    `code_wsh` VARCHAR(50) NOT NULL UNIQUE,
    `description_wsh` VARCHAR(255),
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User Role Table
CREATE TABLE IF NOT EXISTS `user_role_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `role_id_wsh` BIGINT NOT NULL,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_wsh` INT DEFAULT 0,
    UNIQUE KEY `uk_user_role` (`user_id_wsh`, `role_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Category Table
CREATE TABLE IF NOT EXISTS `category_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name_wsh` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id_wsh` BIGINT DEFAULT 0 COMMENT '父分类ID,0表示一级分类',
    `sort_order_wsh` INT DEFAULT 0 COMMENT '排序',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Pet Table
CREATE TABLE IF NOT EXISTS `pet_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `owner_id_wsh` BIGINT NOT NULL,
    `name_wsh` VARCHAR(50) NOT NULL,
    `type_wsh` VARCHAR(50) NOT NULL COMMENT 'dog/cat/other',
    `breed_wsh` VARCHAR(100),
    `age_wsh` INT,
    `weight_wsh` DECIMAL(10, 2),
    `gender_wsh` TINYINT COMMENT '0-female 1-male',
    `sterilized_wsh` TINYINT DEFAULT 0,
    `vaccinated_wsh` TINYINT DEFAULT 0,
    `avatar_wsh` VARCHAR(500),
    `description_wsh` TEXT,
    `allergies_wsh` TEXT,
    `habits_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Merchant Table
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
    `status_wsh` TINYINT DEFAULT 0 COMMENT '0-pending 1-approved 2-rejected',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Keeper Table
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
    `status_wsh` TINYINT DEFAULT 1 COMMENT '0-offline 1-online',
    `bio_wsh` TEXT COMMENT '简介',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Service Table
CREATE TABLE IF NOT EXISTS `pet_service_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id_wsh` BIGINT NOT NULL,
    `name_wsh` VARCHAR(100) NOT NULL,
    `type_wsh` VARCHAR(50) COMMENT 'boarding/grooming/training/walk',
    `description_wsh` TEXT,
    `price_wsh` DECIMAL(10, 2) NOT NULL,
    `unit_wsh` VARCHAR(20) DEFAULT 'day',
    `images_wsh` TEXT,
    `status_wsh` TINYINT DEFAULT 1 COMMENT '0-offline 1-online',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Order Table
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
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/paid/confirmed/delivered/received/in_progress/completed/cancelled/refunding/refunded',
    `handover_code_wsh` VARCHAR(4) COMMENT 'handover verification code',
    `delivery_address_wsh` VARCHAR(500) COMMENT 'pet delivery/dropoff address',
    `delivery_time_wsh` DATETIME COMMENT 'scheduled pet delivery/dropoff time',
    `receiver_available_start_wsh` DATETIME COMMENT 'keeper/receiver available start time',
    `receiver_available_end_wsh` DATETIME COMMENT 'keeper/receiver available end time',
    `pickup_address_wsh` VARCHAR(500) COMMENT 'pet pickup/return address',
    `pickup_time_wsh` DATETIME COMMENT 'scheduled pet pickup/return time',
    `delivered_at_wsh` DATETIME COMMENT 'owner marked delivered at',
    `received_at_wsh` DATETIME COMMENT 'keeper/merchant confirmed receipt at',
    `started_at_wsh` DATETIME COMMENT 'care service started at',
    `start_photo_wsh` VARCHAR(1000) COMMENT 'care service start pet photo URL',
    `completed_at_wsh` DATETIME COMMENT 'order completed at',
    `final_report_generated_wsh` TINYINT DEFAULT 0 COMMENT 'whether AI final report generated',
    `remark_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment Table
CREATE TABLE IF NOT EXISTS `payment_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `order_no_wsh` VARCHAR(50) NOT NULL,
    `pay_no_wsh` VARCHAR(100),
    `amount_wsh` DECIMAL(10, 2) NOT NULL,
    `method_wsh` VARCHAR(20) DEFAULT 'wechat' COMMENT 'wechat/alipay/balance',
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/success/failed',
    `paid_at_wsh` DATETIME,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Refund Table
CREATE TABLE IF NOT EXISTS `refund_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `order_no_wsh` VARCHAR(50) NOT NULL,
    `amount_wsh` DECIMAL(10, 2) NOT NULL,
    `reason_wsh` TEXT,
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected/completed',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Complaint Table
CREATE TABLE IF NOT EXISTS `complaint_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `owner_id_wsh` BIGINT NOT NULL,
    `target_id_wsh` BIGINT,
    `target_type_wsh` VARCHAR(20) COMMENT 'merchant/keeper',
    `title_wsh` VARCHAR(200),
    `content_wsh` TEXT,
    `images_wsh` TEXT,
    `status_wsh` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/processing/resolved/rejected',
    `result_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chat Message Table
CREATE TABLE IF NOT EXISTS `chat_message_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `from_user_id_wsh` BIGINT NOT NULL,
    `to_user_id_wsh` BIGINT NOT NULL,
    `order_id_wsh` BIGINT,
    `content_wsh` TEXT,
    `type_wsh` VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/video/file',
    `file_url_wsh` VARCHAR(500),
    `read_wsh` TINYINT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Favorite Table
CREATE TABLE IF NOT EXISTS `favorite_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL,
    `target_id_wsh` BIGINT NOT NULL,
    `target_type_wsh` VARCHAR(20) NOT NULL COMMENT 'merchant/keeper',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_target` (`user_id_wsh`, `target_id_wsh`, `target_type_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rating Table
CREATE TABLE IF NOT EXISTS `rating_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT,
    `user_id_wsh` BIGINT NOT NULL,
    `target_id_wsh` BIGINT NOT NULL,
    `target_type_wsh` VARCHAR(20) NOT NULL COMMENT 'merchant/keeper/service',
    `score_wsh` TINYINT NOT NULL,
    `content_wsh` TEXT,
    `images_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `reply_wsh` TEXT COMMENT '商家回复',
    `reply_at_wsh` DATETIME COMMENT '回复时间',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI Report Table
CREATE TABLE IF NOT EXISTS `ai_report_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id_wsh` BIGINT NOT NULL,
    `pet_id_wsh` BIGINT NOT NULL,
    `keeper_id_wsh` BIGINT NOT NULL,
    `content_wsh` TEXT,
    `type_wsh` VARCHAR(50) COMMENT 'daily/final/health',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Knowledge Document Table
CREATE TABLE IF NOT EXISTS `knowledge_document_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title_wsh` VARCHAR(200) NOT NULL,
    `content_wsh` TEXT NOT NULL,
    `category_wsh` VARCHAR(50) COMMENT 'boarding/refund/complaint/care/vaccine/agreement/rule',
    `source_type_wsh` VARCHAR(20) COMMENT 'pdf/word/markdown/txt',
    `source_path_wsh` VARCHAR(500),
    `word_count_wsh` INT DEFAULT 0,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Init Knowledge Documents
INSERT IGNORE INTO `knowledge_document_wsh` (`title_wsh`, `content_wsh`, `category_wsh`, `source_type_wsh`) VALUES
('宠物寄养规则', '1. 寄养前需确保宠物已完成疫苗接种\n2. 寄养期间宠物主需提供宠物食物\n3. 寄养员需每日提供宠物活动时间不少于2小时\n4. 寄养期间如宠物生病需及时通知宠物主\n5. 寄养期间宠物意外伤害由寄养员负责\n6. 寄养期间宠物死亡由寄养员全责赔偿\n7. 寄养前需签订寄养协议\n8. 寄养费用按天计算，不足一天按一天计算', 'boarding', 'txt'),
('退款规则', '1. 订单支付后24小时内可全额退款\n2. 寄养开始前3天退款扣除10%手续费\n3. 寄养开始前1天退款扣除30%手续费\n4. 寄养开始后不接受退款\n5. 因平台原因导致无法服务的，全额退款\n6. 因商家原因取消订单的，全额退款并赔偿20%\n7. 退款到账时间：1-3个工作日\n8. 部分退款按实际未服务天数计算', 'refund', 'txt'),
('投诉规则', '1. 投诉需在服务结束后7天内提出\n2. 投诉需要提供相关证据（照片/视频）\n3. 平台客服将在24小时内受理投诉\n4. 投诉处理周期为3-5个工作日\n5. 严重投诉升级至平台管理员处理\n6. 恶意投诉将影响用户信用分\n7. 投诉处理结果将以站内信形式通知\n8. 对处理结果不满可再次申诉', 'complaint', 'txt'),
('宠物护理指南', '1. 每日定时喂食，保持饮食规律\n2. 提供充足的饮用水\n3. 每日遛狗不少于2次\n4. 保持居住环境清洁卫生\n5. 定期梳毛，防止打结\n6. 注意观察宠物精神状态\n7. 发现异常及时记录并通知主人\n8. 按主人要求给药（如有）', 'care', 'txt'),
('疫苗要求', '1. 寄养宠物必须完成核心疫苗接种\n2. 犬类：狂犬病疫苗、犬瘟热疫苗、细小病毒疫苗\n3. 猫类：狂犬病疫苗、猫三联疫苗\n4. 疫苗需在有效期内\n5. 需提供疫苗接种证明\n6. 未接种疫苗的宠物需加收健康管理费\n7. 建议接种流感疫苗（季节性）\n8. 老年宠物建议做健康检查', 'vaccine', 'txt');

-- Document Embedding Table (for vector search)
CREATE TABLE IF NOT EXISTS `document_embedding_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `document_id_wsh` BIGINT NOT NULL,
    `embedding_wsh` LONGTEXT COMMENT 'JSON array of float embedding vector',
    `dimension_wsh` INT DEFAULT 0,
    `chunk_index_wsh` INT DEFAULT 0,
    `chunk_text_wsh` TEXT,
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`document_id_wsh`) REFERENCES `knowledge_document_wsh`(`id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Init Roles
INSERT IGNORE INTO `role_wsh` (`name_wsh`, `code_wsh`, `description_wsh`) VALUES
('管理员', 'ADMIN', '系统管理员'),
('宠物主', 'OWNER', '宠物主人'),
('寄养员', 'KEEPER', '宠物寄养员'),
('商家', 'MERCHANT', '商家'),
('客服', 'CUSTOMER_SERVICE', '客服人员');

-- Address Table
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

-- Notice / Banner Table
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

-- Notification Table
CREATE TABLE IF NOT EXISTS `notification_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_wsh` BIGINT NOT NULL COMMENT '接收用户ID',
    `title_wsh` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content_wsh` TEXT COMMENT '通知内容',
    `type_wsh` VARCHAR(30) COMMENT '通知类型(order/payment/ticket/system/order_fulfillment)',
    `is_read_wsh` TINYINT DEFAULT 0 COMMENT '是否已读 0-否 1-是',
    `related_id_wsh` BIGINT COMMENT '关联业务ID',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notification_user` (`user_id_wsh`),
    INDEX `idx_notification_read` (`is_read_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- File Upload Record Table
CREATE TABLE IF NOT EXISTS `file_record_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `original_name_wsh` VARCHAR(255) COMMENT '原始文件名',
    `object_name_wsh` VARCHAR(500) NOT NULL COMMENT 'MinIO对象名',
    `size_wsh` BIGINT COMMENT '文件大小',
    `content_type_wsh` VARCHAR(100) COMMENT '文件类型',
    `user_id_wsh` BIGINT COMMENT '上传用户ID',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_file_record_user` (`user_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传记录表';

-- Care Record Table
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

-- Finance: Wallet
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

-- Finance: Wallet Transaction
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

-- Finance: Withdrawal
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

-- Ticket System
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

-- Content Review Table
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
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打赏记录';
