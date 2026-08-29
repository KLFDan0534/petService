
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `label_wsh` varchar(50) DEFAULT '' COMMENT '标签(家/公司/学校)',
  `name_wsh` varchar(50) NOT NULL COMMENT '收货人姓名',
  `phone_wsh` varchar(20) NOT NULL COMMENT '收货人电话',
  `address_wsh` varchar(500) NOT NULL COMMENT '详细地址',
  `detail_wsh` varchar(500) DEFAULT '' COMMENT '门牌号等补充信息',
  `latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '??',
  `longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '??',
  `is_default_wsh` tinyint DEFAULT '0' COMMENT '是否默认: 0-否 1-是',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=1005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收货地址表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `adoption_application_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `user_id_wsh` bigint NOT NULL COMMENT '?????ID',
  `pet_id_wsh` bigint NOT NULL COMMENT '????ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '??ID',
  `applicant_name_wsh` varchar(50) NOT NULL COMMENT '?????',
  `applicant_phone_wsh` varchar(20) NOT NULL COMMENT '?????',
  `applicant_address_wsh` varchar(500) NOT NULL COMMENT '????',
  `housing_type_wsh` varchar(50) DEFAULT NULL COMMENT '????',
  `has_yard_wsh` tinyint DEFAULT '0' COMMENT '?????',
  `family_members_wsh` varchar(200) DEFAULT NULL COMMENT '????',
  `pet_experience_wsh` text COMMENT '????',
  `reason_wsh` text NOT NULL COMMENT '????',
  `economic_condition_wsh` varchar(200) DEFAULT NULL COMMENT '????',
  `agree_visit_wsh` tinyint DEFAULT '0' COMMENT '????',
  `merchant_status_wsh` varchar(20) DEFAULT NULL COMMENT '??????',
  `merchant_remark_wsh` varchar(500) DEFAULT NULL COMMENT '??????',
  `admin_status_wsh` varchar(20) DEFAULT NULL COMMENT '???????',
  `admin_remark_wsh` varchar(500) DEFAULT NULL COMMENT '???????',
  `status_wsh` varchar(20) DEFAULT 'PENDING' COMMENT '????',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '????',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user` (`user_id_wsh`) COMMENT '????',
  KEY `idx_pet` (`pet_id_wsh`) COMMENT '????',
  KEY `idx_status` (`status_wsh`) COMMENT '????'
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `adoption_pet_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '????ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '??ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '????',
  `type_wsh` varchar(30) DEFAULT NULL COMMENT '????',
  `breed_wsh` varchar(100) DEFAULT NULL COMMENT '??',
  `age_wsh` int DEFAULT NULL COMMENT '??(?)',
  `gender_wsh` varchar(10) DEFAULT NULL COMMENT '??',
  `weight_wsh` decimal(5,2) DEFAULT NULL COMMENT '??(kg)',
  `color_wsh` varchar(50) DEFAULT NULL COMMENT '??',
  `health_status_wsh` varchar(500) DEFAULT NULL COMMENT '????',
  `vaccinated_wsh` tinyint DEFAULT '0' COMMENT '??????',
  `sterilized_wsh` tinyint DEFAULT '0' COMMENT '????',
  `personality_wsh` varchar(500) DEFAULT NULL COMMENT '??',
  `story_wsh` text COMMENT '????',
  `adoption_requirements_wsh` text COMMENT '????',
  `adoption_fee_wsh` decimal(10,2) DEFAULT '0.00' COMMENT '???',
  `cover_image_wsh` varchar(500) DEFAULT NULL COMMENT '????',
  `images_wsh` varchar(2000) DEFAULT NULL COMMENT '??(????)',
  `status_wsh` varchar(20) DEFAULT 'AVAILABLE' COMMENT '??: AVAILABLE/APPLIED/ADOPTED',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '????',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_merchant` (`merchant_id_wsh`) COMMENT '????',
  KEY `idx_status` (`status_wsh`) COMMENT '????'
) ENGINE=InnoDB AUTO_INCREMENT=115 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_history_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `user_id_wsh` bigint NOT NULL,
  `session_id_wsh` varchar(64) NOT NULL,
  `role_wsh` varchar(20) NOT NULL,
  `content_wsh` text NOT NULL,
  `sources_wsh` varchar(500) DEFAULT NULL,
  `need_human_wsh` tinyint DEFAULT '0',
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  KEY `idx_ai_chat_user` (`user_id_wsh`),
  KEY `idx_ai_chat_session` (`session_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI报告表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  UNIQUE KEY `uk_merchant_day` (`merchant_id_wsh`,`day_of_week_wsh`) COMMENT '星期(1-7)'
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营业时间表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='照护记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '分类名称',
  `parent_id_wsh` bigint DEFAULT '0' COMMENT '父分类ID',
  `sort_order_wsh` int DEFAULT '0' COMMENT '排序',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `complaint_message_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `complaint_id_wsh` bigint NOT NULL COMMENT '投诉ID',
  `from_user_id_wsh` bigint DEFAULT NULL COMMENT '发送人ID',
  `to_user_id_wsh` bigint DEFAULT NULL COMMENT '接收人ID',
  `content_wsh` text NOT NULL COMMENT '消息内容',
  `file_url_wsh` varchar(500) DEFAULT NULL COMMENT '图片附件URL',
  `is_read_wsh` tinyint DEFAULT '0' COMMENT '是否已读',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_complaint_id` (`complaint_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投诉沟通消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `complaint_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '投诉ID',
  `order_id_wsh` bigint DEFAULT NULL COMMENT '订单ID(可空: 支持仅投诉商家不关联订单)',
  `merchant_id_wsh` bigint DEFAULT NULL COMMENT '商家ID',
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
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投诉表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容审核表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_template_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `name_wsh` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_wsh` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `threshold_amount_wsh` decimal(10,2) DEFAULT '0.00',
  `discount_amount_wsh` decimal(10,2) DEFAULT '0.00',
  `discount_rate_wsh` decimal(5,2) DEFAULT NULL,
  `max_discount_amount_wsh` decimal(10,2) DEFAULT NULL,
  `total_quantity_wsh` int DEFAULT NULL,
  `issued_quantity_wsh` int DEFAULT '0',
  `per_user_limit_wsh` int DEFAULT '1',
  `valid_from_wsh` datetime NOT NULL,
  `valid_to_wsh` datetime NOT NULL,
  `status_wsh` tinyint DEFAULT '1',
  `scope_type_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'platform',
  `merchant_id_wsh` bigint DEFAULT NULL,
  `created_by_wsh` bigint DEFAULT NULL,
  `remark_wsh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  KEY `idx_coupon_template_status` (`status_wsh`,`valid_from_wsh`,`valid_to_wsh`),
  KEY `idx_coupon_template_scope` (`scope_type_wsh`,`merchant_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_usage_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `user_coupon_id_wsh` bigint NOT NULL,
  `template_id_wsh` bigint NOT NULL,
  `user_id_wsh` bigint NOT NULL,
  `order_id_wsh` bigint NOT NULL,
  `order_no_wsh` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_amount_wsh` decimal(10,2) NOT NULL,
  `funding_party_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'platform',
  `status_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'used',
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_coupon_usage_user_coupon` (`user_coupon_id_wsh`),
  KEY `idx_coupon_usage_order` (`order_id_wsh`),
  KEY `idx_coupon_usage_user` (`user_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `document_id_wsh` (`document_id_wsh`) COMMENT '文档ID',
  CONSTRAINT `document_embedding_wsh_ibfk_1` FOREIGN KEY (`document_id_wsh`) REFERENCES `knowledge_document_wsh` (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档嵌入向量表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `target_id_wsh` bigint NOT NULL COMMENT '目标ID',
  `target_type_wsh` varchar(20) NOT NULL COMMENT '目标类型: merchant/keeper',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_user_target` (`user_id_wsh`,`target_id_wsh`,`target_type_wsh`) COMMENT '目标类型: merchant/keeper'
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_record_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name_wsh` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `object_name_wsh` varchar(255) DEFAULT NULL COMMENT '存储对象名(MiniO)',
  `size_wsh` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `content_type_wsh` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `purpose_wsh` varchar(30) DEFAULT NULL COMMENT '文件用途: product-产品图片 avatar-头像 evidence-资质证明 其他',
  `merchant_id_wsh` bigint DEFAULT NULL COMMENT '服务端归属商家ID(产品图片等受管文件)',
  `user_id_wsh` bigint DEFAULT NULL COMMENT '上传用户ID',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user` (`user_id_wsh`) COMMENT '上传用户ID',
  KEY `idx_purpose_merchant` (`purpose_wsh`,`merchant_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=1101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件上传记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keeper_attendance_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `keeper_id_wsh` bigint NOT NULL,
  `merchant_id_wsh` bigint NOT NULL,
  `check_in_at_wsh` datetime NOT NULL,
  `check_in_latitude_wsh` decimal(10,7) NOT NULL,
  `check_in_longitude_wsh` decimal(10,7) NOT NULL,
  `check_in_address_wsh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `check_in_accuracy_wsh` decimal(10,2) DEFAULT NULL,
  `check_in_distance_wsh` decimal(10,2) NOT NULL,
  `check_out_at_wsh` datetime DEFAULT NULL,
  `check_out_latitude_wsh` decimal(10,7) DEFAULT NULL,
  `check_out_longitude_wsh` decimal(10,7) DEFAULT NULL,
  `check_out_address_wsh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `check_out_accuracy_wsh` decimal(10,2) DEFAULT NULL,
  `check_out_distance_wsh` decimal(10,2) DEFAULT NULL,
  `radius_meters_wsh` int NOT NULL,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  KEY `idx_keeper_day` (`keeper_id_wsh`,`check_in_at_wsh`),
  KEY `idx_merchant_day` (`merchant_id_wsh`,`check_in_at_wsh`),
  KEY `idx_open_shift` (`keeper_id_wsh`,`check_out_at_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keeper_leave_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `keeper_id_wsh` bigint NOT NULL,
  `merchant_id_wsh` bigint NOT NULL,
  `start_date_wsh` date NOT NULL,
  `end_date_wsh` date NOT NULL,
  `reason_wsh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by_wsh` bigint DEFAULT NULL,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  KEY `idx_keeper_range` (`keeper_id_wsh`,`start_date_wsh`,`end_date_wsh`),
  KEY `idx_merchant_range` (`merchant_id_wsh`,`start_date_wsh`,`end_date_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keeper_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '看护人ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '姓名',
  `phone_wsh` varchar(20) DEFAULT NULL COMMENT '电话',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '头像',
  `experience_years_wsh` int DEFAULT '0' COMMENT '经验(年)',
  `rating_wsh` decimal(3,2) DEFAULT '5.00' COMMENT '??',
  `completion_rate_wsh` decimal(5,2) DEFAULT '100.00' COMMENT '???',
  `complaint_rate_wsh` decimal(5,2) DEFAULT '0.00' COMMENT '???',
  `price_per_day_wsh` decimal(10,2) NOT NULL COMMENT '????',
  `max_pets_wsh` int DEFAULT '5' COMMENT '最大宠物数',
  `current_pets_wsh` int DEFAULT '0' COMMENT '当前宠物数',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-离线 1-在线',
  `bio_wsh` text COMMENT '简介',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `offline_source_wsh` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='看护人表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_plan_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `code_wsh` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_wsh` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `level_wsh` int NOT NULL DEFAULT '1',
  `price_wsh` decimal(10,2) NOT NULL DEFAULT '0.00',
  `duration_days_wsh` int NOT NULL,
  `discount_rate_wsh` decimal(5,2) DEFAULT '1.00',
  `monthly_coupon_config_wsh` text COLLATE utf8mb4_unicode_ci,
  `benefit_config_wsh` text COLLATE utf8mb4_unicode_ci,
  `status_wsh` tinyint DEFAULT '1',
  `sort_order_wsh` int DEFAULT '0',
  `remark_wsh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_member_plan_code` (`code_wsh`),
  KEY `idx_member_plan_status` (`status_wsh`,`level_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_benefit_usage_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `user_id_wsh` bigint NOT NULL,
  `membership_id_wsh` bigint DEFAULT NULL,
  `plan_id_wsh` bigint DEFAULT NULL,
  `benefit_type_wsh` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `benefit_code_wsh` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_type_wsh` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_id_wsh` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount_wsh` decimal(10,2) DEFAULT '0.00',
  `quantity_wsh` int DEFAULT '1',
  `usage_status_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'used',
  `request_id_wsh` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `usage_snapshot_wsh` text COLLATE utf8mb4_unicode_ci,
  `used_at_wsh` datetime DEFAULT NULL,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_membership_usage_request` (`request_id_wsh`),
  KEY `idx_membership_usage_user_type` (`user_id_wsh`,`benefit_type_wsh`),
  KEY `idx_membership_usage_business` (`business_type_wsh`,`business_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_event_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `user_id_wsh` bigint NOT NULL,
  `membership_id_wsh` bigint DEFAULT NULL,
  `membership_order_id_wsh` bigint DEFAULT NULL,
  `event_type_wsh` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_status_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'success',
  `operator_id_wsh` bigint DEFAULT NULL,
  `message_wsh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_snapshot_wsh` text COLLATE utf8mb4_unicode_ci,
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  KEY `idx_membership_event_user` (`user_id_wsh`,`created_at_wsh`),
  KEY `idx_membership_event_membership` (`membership_id_wsh`),
  KEY `idx_membership_event_order` (`membership_order_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_order_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `order_no_wsh` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id_wsh` bigint NOT NULL,
  `plan_id_wsh` bigint NOT NULL,
  `plan_code_wsh` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount_wsh` decimal(10,2) NOT NULL,
  `pay_method_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'balance',
  `status_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `paid_at_wsh` datetime DEFAULT NULL,
  `membership_start_at_wsh` datetime DEFAULT NULL,
  `membership_end_at_wsh` datetime DEFAULT NULL,
  `request_id_wsh` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plan_snapshot_wsh` text COLLATE utf8mb4_unicode_ci,
  `remark_wsh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_membership_order_no` (`order_no_wsh`),
  UNIQUE KEY `uk_membership_order_request` (`request_id_wsh`),
  KEY `idx_membership_order_user_status` (`user_id_wsh`,`status_wsh`),
  KEY `idx_membership_order_plan` (`plan_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_customer_service_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id_wsh` bigint NOT NULL COMMENT 'Merchant ID',
  `user_id_wsh` bigint NOT NULL COMMENT 'Applicant/customer service user ID',
  `applicant_note_wsh` varchar(500) DEFAULT NULL COMMENT 'Application note',
  `review_note_wsh` varchar(500) DEFAULT NULL COMMENT 'Merchant review note',
  `status_wsh` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `reviewer_id_wsh` bigint DEFAULT NULL COMMENT 'Merchant reviewer user ID',
  `reviewed_at_wsh` datetime DEFAULT NULL COMMENT 'Review time',
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_mcs_merchant_user` (`merchant_id_wsh`,`user_id_wsh`),
  KEY `idx_mcs_user_status` (`user_id_wsh`,`status_wsh`),
  KEY `idx_mcs_merchant_status` (`merchant_id_wsh`,`status_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Merchant customer service applications';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '商家ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `name_wsh` varchar(100) NOT NULL COMMENT '商家名称',
  `phone_wsh` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address_wsh` varchar(255) DEFAULT NULL COMMENT '地址',
  `latitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '??',
  `longitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '??',
  `description_wsh` text COMMENT '描述',
  `business_license_wsh` varchar(500) DEFAULT NULL COMMENT '营业执照URL',
  `rating_wsh` decimal(3,2) DEFAULT '5.00' COMMENT '??',
  `status_wsh` tinyint DEFAULT '0' COMMENT '状态: 0-待审核 1-已通过 2-已驳回',
  `store_mode_wsh` tinyint DEFAULT '0' COMMENT '0-auto 1-manual-open 2-manual-close',
  `store_status_wsh` tinyint DEFAULT '0' COMMENT '0-closed 1-open',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '商家头像',
  `future_booking_enabled_wsh` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `migration_ledger_wsh` (
  `version_wsh` varchar(100) NOT NULL COMMENT '迁移版本号',
  `applied_at_wsh` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
  `summary_wsh` varchar(1000) DEFAULT NULL COMMENT '迁移摘要',
  PRIMARY KEY (`version_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库迁移台账';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice_read_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '已读记录ID',
  `notice_id_wsh` bigint NOT NULL COMMENT '公告ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `read_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_notice_read_notice_user` (`notice_id_wsh`,`user_id_wsh`) COMMENT '用户ID'
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title_wsh` varchar(200) NOT NULL COMMENT '标题',
  `content_wsh` text COMMENT '内容',
  `type_wsh` varchar(20) DEFAULT 'notice' COMMENT '类型: notice-公告 banner-Banner',
  `delivery_type_wsh` varchar(32) DEFAULT 'notice' COMMENT '投递方式: notice-普通公告 popup-弹窗通知 broadcast-全员通知',
  `image_url_wsh` varchar(500) DEFAULT NULL COMMENT '图片URL',
  `link_url_wsh` varchar(500) DEFAULT NULL COMMENT '跳转链接',
  `sort_order_wsh` int DEFAULT '0' COMMENT '排序',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 1-显示 0-隐藏',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告与Banner表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_user` (`user_id_wsh`) COMMENT '接收用户ID',
  KEY `idx_read` (`is_read_wsh`) COMMENT '是否已读: 1-已读'
) ENGINE=InnoDB AUTO_INCREMENT=754 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_user_id` (`user_id_wsh`) COMMENT '用户ID',
  KEY `idx_module` (`module_wsh`) COMMENT '模块',
  KEY `idx_created_at` (`created_at_wsh`) COMMENT '创建时间'
) ENGINE=InnoDB AUTO_INCREMENT=1380 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_snapshot_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `order_id_wsh` bigint NOT NULL,
  `order_no_wsh` varchar(50) NOT NULL,
  `owner_snapshot_wsh` text,
  `pet_snapshot_wsh` text,
  `merchant_snapshot_wsh` text,
  `keeper_snapshot_wsh` text,
  `service_snapshot_wsh` text,
  `address_snapshot_wsh` text,
  `price_snapshot_wsh` text,
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_order_snapshot_order` (`order_id_wsh`),
  KEY `idx_order_snapshot_no` (`order_no_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '支付ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `order_no_wsh` varchar(50) NOT NULL COMMENT '订单号',
  `pay_no_wsh` varchar(100) DEFAULT NULL COMMENT '支付流水号',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '????',
  `method_wsh` varchar(20) DEFAULT 'wechat' COMMENT '支付方式: wechat/alipay/balance',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/success/failed',
  `paid_at_wsh` datetime DEFAULT NULL COMMENT '支付时间',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `days_wsh` int NOT NULL COMMENT '天数(day=计费数量, session/hour=1)',
  `price_per_day_wsh` decimal(10,2) NOT NULL COMMENT '????',
  `total_amount_wsh` decimal(10,2) NOT NULL COMMENT '???',
  `discount_wsh` decimal(10,2) DEFAULT '0.00' COMMENT '??',
  `final_amount_wsh` decimal(10,2) NOT NULL COMMENT '????',
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
  `delivery_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `delivery_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `delivery_location_source_wsh` varchar(50) DEFAULT NULL COMMENT '送养位置来源',
  `pickup_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `pickup_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `pickup_location_source_wsh` varchar(50) DEFAULT NULL COMMENT '接回位置来源',
  `delivered_address_wsh` varchar(500) DEFAULT NULL COMMENT '实际交付地址',
  `delivered_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `delivered_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `delivered_accuracy_wsh` decimal(10,2) DEFAULT NULL COMMENT '??????(?)',
  `received_address_wsh` varchar(500) DEFAULT NULL COMMENT '实际接收地址',
  `received_latitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `received_longitude_wsh` decimal(10,7) DEFAULT NULL COMMENT '????',
  `received_accuracy_wsh` decimal(10,2) DEFAULT NULL COMMENT '??????(?)',
  `received_distance_m_wsh` decimal(10,1) DEFAULT NULL COMMENT '????(?)',
  `emergency_contact_name_wsh` varchar(50) DEFAULT NULL COMMENT '紧急联系人姓名',
  `emergency_contact_phone_wsh` varchar(20) DEFAULT NULL COMMENT '紧急联系人电话',
  `emergency_contact_relation_wsh` varchar(30) DEFAULT NULL COMMENT '紧急联系人与宠物关系',
  `coupon_id_wsh` bigint DEFAULT NULL,
  `coupon_template_id_wsh` bigint DEFAULT NULL,
  `coupon_discount_wsh` decimal(10,2) DEFAULT '0.00',
  `platform_subsidy_wsh` decimal(10,2) DEFAULT '0.00',
  `settlement_amount_wsh` decimal(10,2) DEFAULT NULL,
  `promotion_snapshot_wsh` text,
  `membership_id_wsh` bigint DEFAULT NULL,
  `membership_plan_id_wsh` bigint DEFAULT NULL,
  `membership_discount_wsh` decimal(10,2) DEFAULT '0.00',
  `membership_snapshot_wsh` text,
  `billing_unit_wsh` varchar(20) DEFAULT 'day',
  `quantity_wsh` int DEFAULT '1',
  `unit_price_wsh` decimal(10,2) DEFAULT NULL,
  `duration_minutes_wsh` int DEFAULT NULL,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `order_no_wsh` (`order_no_wsh`) COMMENT '订单号'
) ENGINE=InnoDB AUTO_INCREMENT=166 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_service_media_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `service_id_wsh` bigint NOT NULL COMMENT '服务产品ID(pet_service_wsh.id_wsh)',
  `file_id_wsh` bigint NOT NULL COMMENT '文件记录ID(file_record_wsh.id_wsh)',
  `sort_order_wsh` int NOT NULL DEFAULT '0' COMMENT '排序序号(0..N 连续)',
  `is_cover_wsh` tinyint NOT NULL DEFAULT '0' COMMENT '是否封面: 0-否 1-是',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_service_file` (`service_id_wsh`,`file_id_wsh`) COMMENT '文件记录ID(file_record_wsh.id_wsh)',
  UNIQUE KEY `uk_service_sort` (`service_id_wsh`,`sort_order_wsh`) COMMENT '排序序号(0..N 连续)',
  KEY `idx_media_file` (`file_id_wsh`) COMMENT '文件记录ID(file_record_wsh.id_wsh)'
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务产品图片表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_service_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `merchant_id_wsh` bigint NOT NULL COMMENT '商家ID',
  `name_wsh` varchar(100) NOT NULL COMMENT '服务名称',
  `type_wsh` varchar(50) DEFAULT NULL COMMENT '兼容旧字段，新数据从 service_category_wsh.code_wsh 派生',
  `category_id_wsh` bigint DEFAULT NULL COMMENT '关联 service_category_wsh.id_wsh',
  `description_wsh` text COMMENT '描述',
  `price_wsh` decimal(10,2) NOT NULL COMMENT '??',
  `unit_wsh` varchar(20) DEFAULT 'day' COMMENT '计费单位 day/session/hour',
  `images_wsh` text COMMENT '图片URL',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-下架 1-上架',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `duration_minutes_wsh` int DEFAULT NULL,
  `booking_mode_wsh` varchar(20) DEFAULT 'date_range',
  PRIMARY KEY (`id_wsh`),
  KEY `idx_category_id` (`category_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=145 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务项目表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '宠物ID',
  `owner_id_wsh` bigint NOT NULL COMMENT '主人ID',
  `name_wsh` varchar(50) NOT NULL COMMENT '宠物名称',
  `type_wsh` varchar(50) NOT NULL COMMENT '类型: dog/cat/other',
  `breed_wsh` varchar(100) DEFAULT NULL COMMENT '品种',
  `age_wsh` int DEFAULT NULL COMMENT '年龄(月)',
  `weight_wsh` decimal(10,2) DEFAULT NULL COMMENT '??(kg)',
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
) ENGINE=InnoDB AUTO_INCREMENT=194 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_qualification_owner` (`owner_type_wsh`,`owner_id_wsh`) COMMENT '所属业务主体ID',
  KEY `idx_qualification_user` (`user_id_wsh`) COMMENT '提交用户ID'
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资质证明表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  UNIQUE KEY `uk_rating_order_user_type` (`order_id_wsh`,`user_id_wsh`,`target_type_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=141 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refund_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '退款ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `order_no_wsh` varchar(50) NOT NULL COMMENT '订单号',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '????',
  `reason_wsh` text COMMENT '退款原因',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/approved/rejected/completed',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `order_status_before_refund_wsh` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_category_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `parent_id_wsh` bigint DEFAULT NULL COMMENT '父级分类ID，NULL为顶级分类',
  `name_wsh` varchar(50) NOT NULL COMMENT '分类名称（中文）',
  `code_wsh` varchar(50) NOT NULL COMMENT '分类编码，全大写下划线风格',
  `sort_wsh` int DEFAULT '0' COMMENT '排序号',
  `status_wsh` tinyint DEFAULT '1' COMMENT '0-禁用 1-启用',
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `code_wsh` (`code_wsh`),
  KEY `idx_parent_id` (`parent_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket_message_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `ticket_id_wsh` bigint NOT NULL COMMENT '工单ID',
  `user_id_wsh` bigint NOT NULL COMMENT '发送人ID',
  `content_wsh` text NOT NULL COMMENT '消息内容',
  `file_url_wsh` varchar(500) DEFAULT NULL COMMENT '图片附件URL',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_read_wsh` tinyint DEFAULT '0',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `merchant_id_wsh` bigint DEFAULT NULL COMMENT '商家ID',
  `order_id_wsh` bigint DEFAULT NULL COMMENT '订单ID',
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
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服工单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tip_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '打赏ID',
  `order_id_wsh` bigint NOT NULL COMMENT '订单ID',
  `from_user_id_wsh` bigint NOT NULL COMMENT '打赏用户',
  `to_user_id_wsh` bigint NOT NULL COMMENT '接收用户',
  `amount_wsh` decimal(10,2) NOT NULL COMMENT '????',
  `message_wsh` varchar(200) DEFAULT NULL COMMENT '打赏留言',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_coupon_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `template_id_wsh` bigint NOT NULL,
  `user_id_wsh` bigint NOT NULL,
  `status_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'available',
  `source_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'claim',
  `order_id_wsh` bigint DEFAULT NULL,
  `order_no_wsh` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discount_amount_wsh` decimal(10,2) DEFAULT NULL,
  `locked_at_wsh` datetime DEFAULT NULL,
  `used_at_wsh` datetime DEFAULT NULL,
  `expire_at_wsh` datetime DEFAULT NULL,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  KEY `idx_user_coupon_user_status` (`user_id_wsh`,`status_wsh`),
  KEY `idx_user_coupon_template_user` (`template_id_wsh`,`user_id_wsh`),
  KEY `idx_user_coupon_order` (`order_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_membership_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT,
  `user_id_wsh` bigint NOT NULL,
  `plan_id_wsh` bigint DEFAULT NULL,
  `plan_code_wsh` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `level_wsh` int DEFAULT '0',
  `status_wsh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'inactive',
  `started_at_wsh` datetime DEFAULT NULL,
  `expires_at_wsh` datetime DEFAULT NULL,
  `auto_renew_wsh` tinyint DEFAULT '0',
  `source_wsh` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'purchase',
  `last_order_id_wsh` bigint DEFAULT NULL,
  `benefit_snapshot_wsh` text COLLATE utf8mb4_unicode_ci,
  `deleted_wsh` tinyint DEFAULT '0',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_user_membership_user` (`user_id_wsh`),
  KEY `idx_user_membership_status` (`status_wsh`,`expires_at_wsh`),
  KEY `idx_user_membership_plan` (`plan_id_wsh`,`status_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `role_id_wsh` bigint NOT NULL COMMENT '角色ID',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted_wsh` int DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_user_role` (`user_id_wsh`,`role_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=165 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username_wsh` varchar(50) NOT NULL COMMENT '用户名',
  `password_wsh` varchar(255) NOT NULL COMMENT '密码',
  `nickname_wsh` varchar(50) DEFAULT NULL COMMENT '昵称',
  `phone_wsh` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar_wsh` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `email_wsh` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `address_wsh` varchar(255) DEFAULT NULL COMMENT '地址',
  `latitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '??',
  `longitude_wsh` decimal(10,6) DEFAULT NULL COMMENT '??',
  `status_wsh` tinyint DEFAULT '1' COMMENT '状态: 0-禁用 1-启用',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `gender_wsh` tinyint DEFAULT '0' COMMENT '性别: 0-未知 1-男 2-女',
  `real_name_wsh` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `id_card_no_wsh` varchar(32) DEFAULT NULL COMMENT '身份证号',
  `real_name_status_wsh` tinyint DEFAULT '0' COMMENT '实名状态: 0-未认证 1-待审核 2-已认证 3-已驳回',
  `reject_reason_wsh` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `payment_password_wsh` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `username_wsh` (`username_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=182 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_transaction_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `wallet_id_wsh` bigint DEFAULT NULL COMMENT '钱包ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `type_wsh` varchar(20) DEFAULT 'income' COMMENT '类型: income-收入 expense-支出 withdraw-提现 refund-退款',
  `amount_wsh` decimal(12,2) NOT NULL COMMENT '??',
  `balance_after_wsh` decimal(12,2) DEFAULT NULL COMMENT '?????',
  `order_id_wsh` bigint DEFAULT NULL COMMENT '关联订单',
  `description_wsh` varchar(500) DEFAULT NULL COMMENT '描述',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `balance_before_wsh` decimal(12,2) DEFAULT NULL COMMENT '变动前余额',
  `frozen_before_wsh` decimal(12,2) DEFAULT NULL COMMENT '变动前冻结金额',
  `frozen_after_wsh` decimal(12,2) DEFAULT NULL COMMENT '变动后冻结金额',
  `direction_wsh` varchar(20) DEFAULT NULL COMMENT '方向: in-out-freeze-unfreeze-set',
  `status_wsh` varchar(20) DEFAULT 'success' COMMENT '状态: pending-success-failed',
  `business_type_wsh` varchar(50) DEFAULT NULL COMMENT '业务类型: payment-refund-settlement-withdraw-tip-admin_adjust',
  `business_id_wsh` varchar(50) DEFAULT NULL COMMENT '业务ID',
  `request_id_wsh` varchar(100) DEFAULT NULL COMMENT '幂等请求ID',
  `operator_id_wsh` bigint DEFAULT NULL COMMENT 'æ“ä½œäººç”¨æˆ·ID(ç®¡ç†å‘˜è°ƒè´¦æ—¶è®°å½•)',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_wallet_tx_request` (`request_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易流水表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `balance_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '??',
  `frozen_amount_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '????',
  `version_wsh` int NOT NULL DEFAULT '0' COMMENT '乐观锁',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`),
  UNIQUE KEY `uk_wallet_user` (`user_id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal_wsh` (
  `id_wsh` bigint NOT NULL AUTO_INCREMENT COMMENT '提现ID',
  `user_id_wsh` bigint NOT NULL COMMENT '用户ID',
  `amount_wsh` decimal(12,2) NOT NULL COMMENT '????',
  `fee_wsh` decimal(12,2) DEFAULT '0.00' COMMENT '???',
  `actual_amount_wsh` decimal(12,2) DEFAULT NULL COMMENT '????',
  `bank_name_wsh` varchar(100) DEFAULT NULL COMMENT '银行名称',
  `bank_card_wsh` varchar(100) DEFAULT NULL COMMENT '银行卡号',
  `account_name_wsh` varchar(100) DEFAULT NULL COMMENT '持卡人',
  `status_wsh` varchar(20) DEFAULT 'pending' COMMENT '状态: pending-待审核 approved-已通过 rejected-已驳回 completed-已完成',
  `remark_wsh` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at_wsh` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id_wsh`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提现记录表';
/*!40101 SET character_set_client = @saved_cs_client */;