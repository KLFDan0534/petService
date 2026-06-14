-- Pet Service Database Schema

CREATE DATABASE IF NOT EXISTS pet_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pet_service;

-- User Table
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50),
    `phone` VARCHAR(20),
    `avatar` VARCHAR(500),
    `email` VARCHAR(100),
    `address` VARCHAR(255),
    `latitude` DECIMAL(10, 6),
    `longitude` DECIMAL(10, 6),
    `status` TINYINT DEFAULT 1 COMMENT '0-disabled 1-enabled',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Role Table
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(255),
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User Role Table
CREATE TABLE IF NOT EXISTS `user_role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Pet Table
CREATE TABLE IF NOT EXISTS `pet` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `owner_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `type` VARCHAR(50) NOT NULL COMMENT 'dog/cat/other',
    `breed` VARCHAR(100),
    `age` INT,
    `weight` DECIMAL(10, 2),
    `gender` TINYINT COMMENT '0-female 1-male',
    `sterilized` TINYINT DEFAULT 0,
    `vaccinated` TINYINT DEFAULT 0,
    `avatar` VARCHAR(500),
    `description` TEXT,
    `allergies` TEXT,
    `habits` TEXT,
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Merchant Table
CREATE TABLE IF NOT EXISTS `merchant` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20),
    `address` VARCHAR(255),
    `latitude` DECIMAL(10, 6),
    `longitude` DECIMAL(10, 6),
    `description` TEXT,
    `business_license` VARCHAR(500),
    `rating` DECIMAL(3, 2) DEFAULT 5.00,
    `status` TINYINT DEFAULT 0 COMMENT '0-pending 1-approved 2-rejected',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Keeper Table
CREATE TABLE IF NOT EXISTS `keeper` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20),
    `avatar` VARCHAR(500),
    `experience_years` INT DEFAULT 0,
    `rating` DECIMAL(3, 2) DEFAULT 5.00,
    `completion_rate` DECIMAL(5, 2) DEFAULT 100.00,
    `complaint_rate` DECIMAL(5, 2) DEFAULT 0.00,
    `price_per_day` DECIMAL(10, 2) NOT NULL,
    `max_pets` INT DEFAULT 5,
    `current_pets` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '0-offline 1-online',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Service Table
CREATE TABLE IF NOT EXISTS `pet_service` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) COMMENT 'boarding/grooming/training/walk',
    `description` TEXT,
    `price` DECIMAL(10, 2) NOT NULL,
    `unit` VARCHAR(20) DEFAULT 'day',
    `images` TEXT,
    `status` TINYINT DEFAULT 1 COMMENT '0-offline 1-online',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Order Table
CREATE TABLE IF NOT EXISTS `pet_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(50) NOT NULL UNIQUE,
    `owner_id` BIGINT NOT NULL,
    `pet_id` BIGINT NOT NULL,
    `keeper_id` BIGINT NOT NULL,
    `merchant_id` BIGINT NOT NULL,
    `service_id` BIGINT,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `days` INT NOT NULL,
    `price_per_day` DECIMAL(10, 2) NOT NULL,
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `discount` DECIMAL(10, 2) DEFAULT 0,
    `final_amount` DECIMAL(10, 2) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/paid/in_progress/completed/cancelled/refunding/refunded',
    `remark` TEXT,
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment Table
CREATE TABLE IF NOT EXISTS `payment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `order_no` VARCHAR(50) NOT NULL,
    `pay_no` VARCHAR(100),
    `amount` DECIMAL(10, 2) NOT NULL,
    `method` VARCHAR(20) DEFAULT 'wechat' COMMENT 'wechat/alipay/balance',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/success/failed',
    `paid_at` DATETIME,
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Refund Table
CREATE TABLE IF NOT EXISTS `refund` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `order_no` VARCHAR(50) NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `reason` TEXT,
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected/completed',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Complaint Table
CREATE TABLE IF NOT EXISTS `complaint` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `owner_id` BIGINT NOT NULL,
    `target_id` BIGINT,
    `target_type` VARCHAR(20) COMMENT 'merchant/keeper',
    `title` VARCHAR(200),
    `content` TEXT,
    `images` TEXT,
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/processing/resolved/rejected',
    `result` TEXT,
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chat Message Table
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `from_user_id` BIGINT NOT NULL,
    `to_user_id` BIGINT NOT NULL,
    `order_id` BIGINT,
    `content` TEXT,
    `type` VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/video/file',
    `file_url` VARCHAR(500),
    `read` TINYINT DEFAULT 0,
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Favorite Table
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL,
    `target_type` VARCHAR(20) NOT NULL COMMENT 'merchant/keeper',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rating Table
CREATE TABLE IF NOT EXISTS `rating` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL,
    `target_type` VARCHAR(20) NOT NULL COMMENT 'merchant/keeper',
    `score` TINYINT NOT NULL,
    `content` TEXT,
    `images` TEXT,
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI Report Table
CREATE TABLE IF NOT EXISTS `ai_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `pet_id` BIGINT NOT NULL,
    `keeper_id` BIGINT NOT NULL,
    `content` TEXT,
    `type` VARCHAR(50) COMMENT 'daily/final/health',
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Knowledge Document Table
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `category` VARCHAR(50) COMMENT 'boarding/refund/complaint/care/vaccine/agreement/rule',
    `source_type` VARCHAR(20) COMMENT 'pdf/word/markdown/txt',
    `source_path` VARCHAR(500),
    `word_count` INT DEFAULT 0,
    `deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Init Knowledge Documents
INSERT IGNORE INTO `knowledge_document` (`title`, `content`, `category`, `source_type`) VALUES
('宠物寄养规则', '1. 寄养前需确保宠物已完成疫苗接种\n2. 寄养期间宠物主需提供宠物食物\n3. 寄养员需每日提供宠物活动时间不少于2小时\n4. 寄养期间如宠物生病需及时通知宠物主\n5. 寄养期间宠物意外伤害由寄养员负责\n6. 寄养期间宠物死亡由寄养员全责赔偿\n7. 寄养前需签订寄养协议\n8. 寄养费用按天计算，不足一天按一天计算', 'boarding', 'txt'),
('退款规则', '1. 订单支付后24小时内可全额退款\n2. 寄养开始前3天退款扣除10%手续费\n3. 寄养开始前1天退款扣除30%手续费\n4. 寄养开始后不接受退款\n5. 因平台原因导致无法服务的，全额退款\n6. 因商家原因取消订单的，全额退款并赔偿20%\n7. 退款到账时间：1-3个工作日\n8. 部分退款按实际未服务天数计算', 'refund', 'txt'),
('投诉规则', '1. 投诉需在服务结束后7天内提出\n2. 投诉需要提供相关证据（照片/视频）\n3. 平台客服将在24小时内受理投诉\n4. 投诉处理周期为3-5个工作日\n5. 严重投诉升级至平台管理员处理\n6. 恶意投诉将影响用户信用分\n7. 投诉处理结果将以站内信形式通知\n8. 对处理结果不满可再次申诉', 'complaint', 'txt'),
('宠物护理指南', '1. 每日定时喂食，保持饮食规律\n2. 提供充足的饮用水\n3. 每日遛狗不少于2次\n4. 保持居住环境清洁卫生\n5. 定期梳毛，防止打结\n6. 注意观察宠物精神状态\n7. 发现异常及时记录并通知主人\n8. 按主人要求给药（如有）', 'care', 'txt'),
('疫苗要求', '1. 寄养宠物必须完成核心疫苗接种\n2. 犬类：狂犬病疫苗、犬瘟热疫苗、细小病毒疫苗\n3. 猫类：狂犬病疫苗、猫三联疫苗\n4. 疫苗需在有效期内\n5. 需提供疫苗接种证明\n6. 未接种疫苗的宠物需加收健康管理费\n7. 建议接种流感疫苗（季节性）\n8. 老年宠物建议做健康检查', 'vaccine', 'txt');

-- Init Roles
INSERT IGNORE INTO `role` (`name`, `code`, `description`) VALUES
('管理员', 'ADMIN', '系统管理员'),
('宠物主', 'OWNER', '宠物主人'),
('寄养员', 'KEEPER', '宠物寄养员'),
('商家', 'MERCHANT', '商家'),
('客服', 'CUSTOMER_SERVICE', '客服人员');
