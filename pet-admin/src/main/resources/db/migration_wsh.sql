-- ============================================================
-- _wsh 后缀重构迁移脚本
-- 将所有表名、字段名统一改为 _wsh 后缀
-- ============================================================

-- 1. user -> user_wsh
ALTER TABLE `user` RENAME TO `user_wsh`;
ALTER TABLE `user_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `user_wsh` CHANGE `username` `username_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `user_wsh` CHANGE `password` `password_wsh` VARCHAR(255) NOT NULL;
ALTER TABLE `user_wsh` CHANGE `nickname` `nickname_wsh` VARCHAR(50);
ALTER TABLE `user_wsh` CHANGE `phone` `phone_wsh` VARCHAR(20);
ALTER TABLE `user_wsh` CHANGE `avatar` `avatar_wsh` VARCHAR(500);
ALTER TABLE `user_wsh` CHANGE `email` `email_wsh` VARCHAR(100);
ALTER TABLE `user_wsh` CHANGE `address` `address_wsh` VARCHAR(255);
ALTER TABLE `user_wsh` CHANGE `latitude` `latitude_wsh` DECIMAL(10, 6);
ALTER TABLE `user_wsh` CHANGE `longitude` `longitude_wsh` DECIMAL(10, 6);
ALTER TABLE `user_wsh` CHANGE `status` `status_wsh` TINYINT DEFAULT 1;
ALTER TABLE `user_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `user_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `user_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2. role -> role_wsh
ALTER TABLE `role` RENAME TO `role_wsh`;
ALTER TABLE `role_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `role_wsh` CHANGE `name` `name_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `role_wsh` CHANGE `code` `code_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `role_wsh` CHANGE `description` `description_wsh` VARCHAR(255);
ALTER TABLE `role_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `role_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 3. user_role -> user_role_wsh
ALTER TABLE `user_role` RENAME TO `user_role_wsh`;
ALTER TABLE `user_role_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `user_role_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `user_role_wsh` CHANGE `role_id` `role_id_wsh` BIGINT NOT NULL;
ALTER TABLE `user_role_wsh` CHANGE `deleted` `deleted_wsh` INT DEFAULT 0;
ALTER TABLE `user_role_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 4. category -> category_wsh
ALTER TABLE `category` RENAME TO `category_wsh`;
ALTER TABLE `category_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `category_wsh` CHANGE `name` `name_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `category_wsh` CHANGE `parent_id` `parent_id_wsh` BIGINT DEFAULT 0;
ALTER TABLE `category_wsh` CHANGE `sort_order` `sort_order_wsh` INT DEFAULT 0;
ALTER TABLE `category_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `category_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `category_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 5. pet -> pet_wsh
ALTER TABLE `pet` RENAME TO `pet_wsh`;
ALTER TABLE `pet_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `pet_wsh` CHANGE `owner_id` `owner_id_wsh` BIGINT NOT NULL;
ALTER TABLE `pet_wsh` CHANGE `name` `name_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `pet_wsh` CHANGE `type` `type_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `pet_wsh` CHANGE `breed` `breed_wsh` VARCHAR(100);
ALTER TABLE `pet_wsh` CHANGE `age` `age_wsh` INT;
ALTER TABLE `pet_wsh` CHANGE `weight` `weight_wsh` DECIMAL(10, 2);
ALTER TABLE `pet_wsh` CHANGE `gender` `gender_wsh` TINYINT;
ALTER TABLE `pet_wsh` CHANGE `sterilized` `sterilized_wsh` TINYINT DEFAULT 0;
ALTER TABLE `pet_wsh` CHANGE `vaccinated` `vaccinated_wsh` TINYINT DEFAULT 0;
ALTER TABLE `pet_wsh` CHANGE `avatar` `avatar_wsh` VARCHAR(500);
ALTER TABLE `pet_wsh` CHANGE `description` `description_wsh` TEXT;
ALTER TABLE `pet_wsh` CHANGE `allergies` `allergies_wsh` TEXT;
ALTER TABLE `pet_wsh` CHANGE `habits` `habits_wsh` TEXT;
ALTER TABLE `pet_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `pet_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `pet_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 6. merchant -> merchant_wsh
ALTER TABLE `merchant` RENAME TO `merchant_wsh`;
ALTER TABLE `merchant_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `merchant_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `merchant_wsh` CHANGE `name` `name_wsh` VARCHAR(100) NOT NULL;
ALTER TABLE `merchant_wsh` CHANGE `phone` `phone_wsh` VARCHAR(20);
ALTER TABLE `merchant_wsh` CHANGE `address` `address_wsh` VARCHAR(255);
ALTER TABLE `merchant_wsh` CHANGE `latitude` `latitude_wsh` DECIMAL(10, 6);
ALTER TABLE `merchant_wsh` CHANGE `longitude` `longitude_wsh` DECIMAL(10, 6);
ALTER TABLE `merchant_wsh` CHANGE `description` `description_wsh` TEXT;
ALTER TABLE `merchant_wsh` CHANGE `business_license` `business_license_wsh` VARCHAR(500);
ALTER TABLE `merchant_wsh` CHANGE `rating` `rating_wsh` DECIMAL(3, 2) DEFAULT 5.00;
ALTER TABLE `merchant_wsh` CHANGE `status` `status_wsh` TINYINT DEFAULT 0;
ALTER TABLE `merchant_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `merchant_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `merchant_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 7. keeper -> keeper_wsh
ALTER TABLE `keeper` RENAME TO `keeper_wsh`;
ALTER TABLE `keeper_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `keeper_wsh` CHANGE `merchant_id` `merchant_id_wsh` BIGINT NOT NULL;
ALTER TABLE `keeper_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `keeper_wsh` CHANGE `name` `name_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `keeper_wsh` CHANGE `phone` `phone_wsh` VARCHAR(20);
ALTER TABLE `keeper_wsh` CHANGE `avatar` `avatar_wsh` VARCHAR(500);
ALTER TABLE `keeper_wsh` CHANGE `experience_years` `experience_years_wsh` INT DEFAULT 0;
ALTER TABLE `keeper_wsh` CHANGE `rating` `rating_wsh` DECIMAL(3, 2) DEFAULT 5.00;
ALTER TABLE `keeper_wsh` CHANGE `completion_rate` `completion_rate_wsh` DECIMAL(5, 2) DEFAULT 100.00;
ALTER TABLE `keeper_wsh` CHANGE `complaint_rate` `complaint_rate_wsh` DECIMAL(5, 2) DEFAULT 0.00;
ALTER TABLE `keeper_wsh` CHANGE `price_per_day` `price_per_day_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `keeper_wsh` CHANGE `max_pets` `max_pets_wsh` INT DEFAULT 5;
ALTER TABLE `keeper_wsh` CHANGE `current_pets` `current_pets_wsh` INT DEFAULT 0;
ALTER TABLE `keeper_wsh` CHANGE `bio` `bio_wsh` TEXT;
ALTER TABLE `keeper_wsh` CHANGE `status` `status_wsh` TINYINT DEFAULT 1;
ALTER TABLE `keeper_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `keeper_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `keeper_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 8. pet_service -> pet_service_wsh
ALTER TABLE `pet_service` RENAME TO `pet_service_wsh`;
ALTER TABLE `pet_service_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `pet_service_wsh` CHANGE `merchant_id` `merchant_id_wsh` BIGINT NOT NULL;
ALTER TABLE `pet_service_wsh` CHANGE `name` `name_wsh` VARCHAR(100) NOT NULL;
ALTER TABLE `pet_service_wsh` CHANGE `type` `type_wsh` VARCHAR(50);
ALTER TABLE `pet_service_wsh` CHANGE `description` `description_wsh` TEXT;
ALTER TABLE `pet_service_wsh` CHANGE `price` `price_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `pet_service_wsh` CHANGE `unit` `unit_wsh` VARCHAR(20) DEFAULT 'day';
ALTER TABLE `pet_service_wsh` CHANGE `images` `images_wsh` TEXT;
ALTER TABLE `pet_service_wsh` CHANGE `status` `status_wsh` TINYINT DEFAULT 1;
ALTER TABLE `pet_service_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `pet_service_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `pet_service_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 9. pet_order -> pet_order_wsh
ALTER TABLE `pet_order` RENAME TO `pet_order_wsh`;
ALTER TABLE `pet_order_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `pet_order_wsh` CHANGE `order_no` `order_no_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `owner_id` `owner_id_wsh` BIGINT NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `pet_id` `pet_id_wsh` BIGINT NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `keeper_id` `keeper_id_wsh` BIGINT NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `merchant_id` `merchant_id_wsh` BIGINT NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `service_id` `service_id_wsh` BIGINT;
ALTER TABLE `pet_order_wsh` CHANGE `start_date` `start_date_wsh` DATE NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `end_date` `end_date_wsh` DATE NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `days` `days_wsh` INT NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `price_per_day` `price_per_day_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `total_amount` `total_amount_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `discount` `discount_wsh` DECIMAL(10, 2) DEFAULT 0;
ALTER TABLE `pet_order_wsh` CHANGE `final_amount` `final_amount_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `pet_order_wsh` CHANGE `status` `status_wsh` VARCHAR(20) DEFAULT 'pending';
ALTER TABLE `pet_order_wsh` CHANGE `remark` `remark_wsh` TEXT;
ALTER TABLE `pet_order_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `pet_order_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `pet_order_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 10. payment -> payment_wsh
ALTER TABLE `payment` RENAME TO `payment_wsh`;
ALTER TABLE `payment_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `payment_wsh` CHANGE `order_id` `order_id_wsh` BIGINT NOT NULL;
ALTER TABLE `payment_wsh` CHANGE `order_no` `order_no_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `payment_wsh` CHANGE `pay_no` `pay_no_wsh` VARCHAR(100);
ALTER TABLE `payment_wsh` CHANGE `amount` `amount_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `payment_wsh` CHANGE `method` `method_wsh` VARCHAR(20) DEFAULT 'wechat';
ALTER TABLE `payment_wsh` CHANGE `status` `status_wsh` VARCHAR(20) DEFAULT 'pending';
ALTER TABLE `payment_wsh` CHANGE `paid_at` `paid_at_wsh` DATETIME;
ALTER TABLE `payment_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `payment_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `payment_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 11. refund -> refund_wsh
ALTER TABLE `refund` RENAME TO `refund_wsh`;
ALTER TABLE `refund_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `refund_wsh` CHANGE `order_id` `order_id_wsh` BIGINT NOT NULL;
ALTER TABLE `refund_wsh` CHANGE `order_no` `order_no_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `refund_wsh` CHANGE `amount` `amount_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `refund_wsh` CHANGE `reason` `reason_wsh` TEXT;
ALTER TABLE `refund_wsh` CHANGE `status` `status_wsh` VARCHAR(20) DEFAULT 'pending';
ALTER TABLE `refund_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `refund_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `refund_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 12. complaint -> complaint_wsh
ALTER TABLE `complaint` RENAME TO `complaint_wsh`;
ALTER TABLE `complaint_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `complaint_wsh` CHANGE `order_id` `order_id_wsh` BIGINT NOT NULL;
ALTER TABLE `complaint_wsh` CHANGE `owner_id` `owner_id_wsh` BIGINT NOT NULL;
ALTER TABLE `complaint_wsh` CHANGE `target_id` `target_id_wsh` BIGINT;
ALTER TABLE `complaint_wsh` CHANGE `target_type` `target_type_wsh` VARCHAR(20);
ALTER TABLE `complaint_wsh` CHANGE `title` `title_wsh` VARCHAR(200);
ALTER TABLE `complaint_wsh` CHANGE `content` `content_wsh` TEXT;
ALTER TABLE `complaint_wsh` CHANGE `images` `images_wsh` TEXT;
ALTER TABLE `complaint_wsh` CHANGE `status` `status_wsh` VARCHAR(20) DEFAULT 'pending';
ALTER TABLE `complaint_wsh` CHANGE `result` `result_wsh` TEXT;
ALTER TABLE `complaint_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `complaint_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `complaint_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 13. chat_message -> chat_message_wsh
ALTER TABLE `chat_message` RENAME TO `chat_message_wsh`;
ALTER TABLE `chat_message_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `chat_message_wsh` CHANGE `from_user_id` `from_user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `chat_message_wsh` CHANGE `to_user_id` `to_user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `chat_message_wsh` CHANGE `order_id` `order_id_wsh` BIGINT;
ALTER TABLE `chat_message_wsh` CHANGE `content` `content_wsh` TEXT;
ALTER TABLE `chat_message_wsh` CHANGE `type` `type_wsh` VARCHAR(20) DEFAULT 'text';
ALTER TABLE `chat_message_wsh` CHANGE `file_url` `file_url_wsh` VARCHAR(500);
ALTER TABLE `chat_message_wsh` CHANGE `read` `read_wsh` TINYINT DEFAULT 0;
ALTER TABLE `chat_message_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `chat_message_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 14. favorite -> favorite_wsh
ALTER TABLE `favorite` RENAME TO `favorite_wsh`;
ALTER TABLE `favorite_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `favorite_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `favorite_wsh` CHANGE `target_id` `target_id_wsh` BIGINT NOT NULL;
ALTER TABLE `favorite_wsh` CHANGE `target_type` `target_type_wsh` VARCHAR(20) NOT NULL;
ALTER TABLE `favorite_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `favorite_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 15. rating -> rating_wsh
ALTER TABLE `rating` RENAME TO `rating_wsh`;
ALTER TABLE `rating_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `rating_wsh` CHANGE `order_id` `order_id_wsh` BIGINT NOT NULL;
ALTER TABLE `rating_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `rating_wsh` CHANGE `target_id` `target_id_wsh` BIGINT NOT NULL;
ALTER TABLE `rating_wsh` CHANGE `target_type` `target_type_wsh` VARCHAR(20) NOT NULL;
ALTER TABLE `rating_wsh` CHANGE `score` `score_wsh` TINYINT NOT NULL;
ALTER TABLE `rating_wsh` CHANGE `content` `content_wsh` TEXT;
ALTER TABLE `rating_wsh` CHANGE `images` `images_wsh` TEXT;
ALTER TABLE `rating_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `rating_wsh` CHANGE `reply` `reply_wsh` TEXT;
ALTER TABLE `rating_wsh` CHANGE `reply_at` `reply_at_wsh` DATETIME;
ALTER TABLE `rating_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 16. ai_report -> ai_report_wsh
ALTER TABLE `ai_report` RENAME TO `ai_report_wsh`;
ALTER TABLE `ai_report_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `ai_report_wsh` CHANGE `order_id` `order_id_wsh` BIGINT NOT NULL;
ALTER TABLE `ai_report_wsh` CHANGE `pet_id` `pet_id_wsh` BIGINT NOT NULL;
ALTER TABLE `ai_report_wsh` CHANGE `keeper_id` `keeper_id_wsh` BIGINT NOT NULL;
ALTER TABLE `ai_report_wsh` CHANGE `content` `content_wsh` TEXT;
ALTER TABLE `ai_report_wsh` CHANGE `type` `type_wsh` VARCHAR(50);
ALTER TABLE `ai_report_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `ai_report_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 17. knowledge_document -> knowledge_document_wsh
ALTER TABLE `knowledge_document` RENAME TO `knowledge_document_wsh`;
ALTER TABLE `knowledge_document_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `knowledge_document_wsh` CHANGE `title` `title_wsh` VARCHAR(200) NOT NULL;
ALTER TABLE `knowledge_document_wsh` CHANGE `content` `content_wsh` TEXT NOT NULL;
ALTER TABLE `knowledge_document_wsh` CHANGE `category` `category_wsh` VARCHAR(50);
ALTER TABLE `knowledge_document_wsh` CHANGE `source_type` `source_type_wsh` VARCHAR(20);
ALTER TABLE `knowledge_document_wsh` CHANGE `source_path` `source_path_wsh` VARCHAR(500);
ALTER TABLE `knowledge_document_wsh` CHANGE `word_count` `word_count_wsh` INT DEFAULT 0;
ALTER TABLE `knowledge_document_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `knowledge_document_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `knowledge_document_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 18. document_embedding -> document_embedding_wsh
ALTER TABLE `document_embedding` RENAME TO `document_embedding_wsh`;
ALTER TABLE `document_embedding_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `document_embedding_wsh` CHANGE `document_id` `document_id_wsh` BIGINT NOT NULL;
ALTER TABLE `document_embedding_wsh` CHANGE `embedding` `embedding_wsh` LONGTEXT;
ALTER TABLE `document_embedding_wsh` CHANGE `dimension` `dimension_wsh` INT DEFAULT 0;
ALTER TABLE `document_embedding_wsh` CHANGE `chunk_index` `chunk_index_wsh` INT DEFAULT 0;
ALTER TABLE `document_embedding_wsh` CHANGE `chunk_text` `chunk_text_wsh` TEXT;
ALTER TABLE `document_embedding_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `document_embedding_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 19. address -> address_wsh
ALTER TABLE `address` RENAME TO `address_wsh`;
ALTER TABLE `address_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `address_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `address_wsh` CHANGE `label` `label_wsh` VARCHAR(50) DEFAULT '';
ALTER TABLE `address_wsh` CHANGE `name` `name_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `address_wsh` CHANGE `phone` `phone_wsh` VARCHAR(20) NOT NULL;
ALTER TABLE `address_wsh` CHANGE `address` `address_wsh` VARCHAR(500) NOT NULL;
ALTER TABLE `address_wsh` CHANGE `detail` `detail_wsh` VARCHAR(500) DEFAULT '';
ALTER TABLE `address_wsh` CHANGE `latitude` `latitude_wsh` DECIMAL(10, 7);
ALTER TABLE `address_wsh` CHANGE `longitude` `longitude_wsh` DECIMAL(10, 7);
ALTER TABLE `address_wsh` CHANGE `is_default` `is_default_wsh` TINYINT DEFAULT 0;
ALTER TABLE `address_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `address_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `address_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 20. notice -> notice_wsh
ALTER TABLE `notice` RENAME TO `notice_wsh`;
ALTER TABLE `notice_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `notice_wsh` CHANGE `title` `title_wsh` VARCHAR(200) NOT NULL;
ALTER TABLE `notice_wsh` CHANGE `content` `content_wsh` TEXT;
ALTER TABLE `notice_wsh` CHANGE `type` `type_wsh` VARCHAR(20) DEFAULT 'notice';
ALTER TABLE `notice_wsh` CHANGE `image_url` `image_url_wsh` VARCHAR(500);
ALTER TABLE `notice_wsh` CHANGE `link_url` `link_url_wsh` VARCHAR(500);
ALTER TABLE `notice_wsh` CHANGE `sort_order` `sort_order_wsh` INT DEFAULT 0;
ALTER TABLE `notice_wsh` CHANGE `status` `status_wsh` TINYINT DEFAULT 1;
ALTER TABLE `notice_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `notice_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `notice_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 21. care_record -> care_record_wsh
ALTER TABLE `care_record` RENAME TO `care_record_wsh`;
ALTER TABLE `care_record_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `care_record_wsh` CHANGE `order_id` `order_id_wsh` BIGINT NOT NULL;
ALTER TABLE `care_record_wsh` CHANGE `pet_id` `pet_id_wsh` BIGINT;
ALTER TABLE `care_record_wsh` CHANGE `keeper_id` `keeper_id_wsh` BIGINT;
ALTER TABLE `care_record_wsh` CHANGE `type` `type_wsh` VARCHAR(20) DEFAULT 'feed';
ALTER TABLE `care_record_wsh` CHANGE `content` `content_wsh` TEXT;
ALTER TABLE `care_record_wsh` CHANGE `images` `images_wsh` VARCHAR(2000);
ALTER TABLE `care_record_wsh` CHANGE `record_time` `record_time_wsh` DATETIME;
ALTER TABLE `care_record_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `care_record_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 22. wallet -> wallet_wsh
ALTER TABLE `wallet` RENAME TO `wallet_wsh`;
ALTER TABLE `wallet_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `wallet_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `wallet_wsh` CHANGE `balance` `balance_wsh` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `wallet_wsh` CHANGE `frozen_amount` `frozen_amount_wsh` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `wallet_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `wallet_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `wallet_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 23. wallet_transaction -> wallet_transaction_wsh
ALTER TABLE `wallet_transaction` RENAME TO `wallet_transaction_wsh`;
ALTER TABLE `wallet_transaction_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `wallet_transaction_wsh` CHANGE `wallet_id` `wallet_id_wsh` BIGINT;
ALTER TABLE `wallet_transaction_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `wallet_transaction_wsh` CHANGE `type` `type_wsh` VARCHAR(20) DEFAULT 'income';
ALTER TABLE `wallet_transaction_wsh` CHANGE `amount` `amount_wsh` DECIMAL(12,2) NOT NULL;
ALTER TABLE `wallet_transaction_wsh` CHANGE `balance_after` `balance_after_wsh` DECIMAL(12,2);
ALTER TABLE `wallet_transaction_wsh` CHANGE `order_id` `order_id_wsh` BIGINT;
ALTER TABLE `wallet_transaction_wsh` CHANGE `description` `description_wsh` VARCHAR(500);
ALTER TABLE `wallet_transaction_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `wallet_transaction_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 24. withdrawal -> withdrawal_wsh
ALTER TABLE `withdrawal` RENAME TO `withdrawal_wsh`;
ALTER TABLE `withdrawal_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `withdrawal_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `withdrawal_wsh` CHANGE `amount` `amount_wsh` DECIMAL(12,2) NOT NULL;
ALTER TABLE `withdrawal_wsh` CHANGE `fee` `fee_wsh` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `withdrawal_wsh` CHANGE `actual_amount` `actual_amount_wsh` DECIMAL(12,2);
ALTER TABLE `withdrawal_wsh` CHANGE `bank_name` `bank_name_wsh` VARCHAR(100);
ALTER TABLE `withdrawal_wsh` CHANGE `bank_card` `bank_card_wsh` VARCHAR(100);
ALTER TABLE `withdrawal_wsh` CHANGE `account_name` `account_name_wsh` VARCHAR(100);
ALTER TABLE `withdrawal_wsh` CHANGE `status` `status_wsh` VARCHAR(20) DEFAULT 'pending';
ALTER TABLE `withdrawal_wsh` CHANGE `remark` `remark_wsh` VARCHAR(500);
ALTER TABLE `withdrawal_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `withdrawal_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `withdrawal_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 25. ticket -> ticket_wsh
ALTER TABLE `ticket` RENAME TO `ticket_wsh`;
ALTER TABLE `ticket_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `ticket_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `ticket_wsh` CHANGE `title` `title_wsh` VARCHAR(200) NOT NULL;
ALTER TABLE `ticket_wsh` CHANGE `content` `content_wsh` TEXT;
ALTER TABLE `ticket_wsh` CHANGE `category` `category_wsh` VARCHAR(50);
ALTER TABLE `ticket_wsh` CHANGE `priority` `priority_wsh` VARCHAR(20) DEFAULT 'medium';
ALTER TABLE `ticket_wsh` CHANGE `status` `status_wsh` VARCHAR(20) DEFAULT 'pending';
ALTER TABLE `ticket_wsh` CHANGE `assignee_id` `assignee_id_wsh` BIGINT;
ALTER TABLE `ticket_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `ticket_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `ticket_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 26. ticket_message -> ticket_message_wsh
ALTER TABLE `ticket_message` RENAME TO `ticket_message_wsh`;
ALTER TABLE `ticket_message_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `ticket_message_wsh` CHANGE `ticket_id` `ticket_id_wsh` BIGINT NOT NULL;
ALTER TABLE `ticket_message_wsh` CHANGE `user_id` `user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `ticket_message_wsh` CHANGE `content` `content_wsh` TEXT NOT NULL;
ALTER TABLE `ticket_message_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `ticket_message_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 27. content_review -> content_review_wsh
ALTER TABLE `content_review` RENAME TO `content_review_wsh`;
ALTER TABLE `content_review_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `content_review_wsh` CHANGE `target_type` `target_type_wsh` VARCHAR(50) NOT NULL;
ALTER TABLE `content_review_wsh` CHANGE `target_id` `target_id_wsh` BIGINT NOT NULL;
ALTER TABLE `content_review_wsh` CHANGE `reporter_id` `reporter_id_wsh` BIGINT;
ALTER TABLE `content_review_wsh` CHANGE `reason` `reason_wsh` VARCHAR(500);
ALTER TABLE `content_review_wsh` CHANGE `status` `status_wsh` VARCHAR(20) DEFAULT 'pending';
ALTER TABLE `content_review_wsh` CHANGE `reviewer_id` `reviewer_id_wsh` BIGINT;
ALTER TABLE `content_review_wsh` CHANGE `review_remark` `review_remark_wsh` VARCHAR(500);
ALTER TABLE `content_review_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `content_review_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `content_review_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 28. tip -> tip_wsh
ALTER TABLE `tip` RENAME TO `tip_wsh`;
ALTER TABLE `tip_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `tip_wsh` CHANGE `order_id` `order_id_wsh` BIGINT NOT NULL;
ALTER TABLE `tip_wsh` CHANGE `from_user_id` `from_user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `tip_wsh` CHANGE `to_user_id` `to_user_id_wsh` BIGINT NOT NULL;
ALTER TABLE `tip_wsh` CHANGE `amount` `amount_wsh` DECIMAL(10, 2) NOT NULL;
ALTER TABLE `tip_wsh` CHANGE `message` `message_wsh` VARCHAR(200);
ALTER TABLE `tip_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 29. business_hours -> business_hours_wsh
ALTER TABLE `business_hours` RENAME TO `business_hours_wsh`;
ALTER TABLE `business_hours_wsh` CHANGE `id` `id_wsh` BIGINT AUTO_INCREMENT;
ALTER TABLE `business_hours_wsh` CHANGE `merchant_id` `merchant_id_wsh` BIGINT NOT NULL;
ALTER TABLE `business_hours_wsh` CHANGE `day_of_week` `day_of_week_wsh` TINYINT NOT NULL;
ALTER TABLE `business_hours_wsh` CHANGE `open_time` `open_time_wsh` TIME;
ALTER TABLE `business_hours_wsh` CHANGE `close_time` `close_time_wsh` TIME;
ALTER TABLE `business_hours_wsh` CHANGE `is_closed` `is_closed_wsh` TINYINT DEFAULT 0;
ALTER TABLE `business_hours_wsh` CHANGE `deleted` `deleted_wsh` TINYINT DEFAULT 0;
ALTER TABLE `business_hours_wsh` CHANGE `created_at` `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `business_hours_wsh` CHANGE `updated_at` `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
