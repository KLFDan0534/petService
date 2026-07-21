-- Seed Data

INSERT IGNORE INTO `role_wsh` (`id_wsh`, `name_wsh`, `code_wsh`, `description_wsh`) VALUES
(1, '系统管理员', 'ADMIN', '系统管理员'),
(2, '宠物主人', 'OWNER', '宠物主人'),
(3, '上门喂养', 'KEEPER', '上门喂养/遛狗人员'),
(4, '商家', 'MERCHANT', '商家/店铺');

INSERT IGNORE INTO `user_wsh` (`id_wsh`, `username_wsh`, `password_wsh`, `nickname_wsh`, `phone_wsh`, `email_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `status_wsh`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', '13800000001', 'admin@pet.com', 'Beijing', 39.9042, 116.4074, 1),
(2, 'owner', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '宠物主人', '13800000002', 'owner@pet.com', 'Shanghai', 31.2304, 121.4737, 1),
(3, 'merchant1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '商家小王', '13800000003', 'merchant1@pet.com', 'Shanghai', 31.2304, 121.4737, 1),
(4, 'keeper1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '遛狗小李', '13800000004', 'keeper1@pet.com', 'Shanghai', 31.2304, 121.4737, 1);

INSERT IGNORE INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`) VALUES
(1, 1), (2, 2), (3, 4), (4, 3);

INSERT IGNORE INTO `category_wsh` (`id_wsh`, `name_wsh`, `parent_id_wsh`, `sort_order_wsh`) VALUES
(1, '狗', 0, 1),
(2, '猫', 0, 2),
(3, '鸟类', 0, 3),
(4, '爬宠', 0, 4),
(5, '泰迪', 1, 1),
(6, '金毛', 1, 2),
(7, '柯基', 1, 3),
(8, '拉布拉多', 1, 4);

INSERT IGNORE INTO `pet_wsh` (`id_wsh`, `owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`) VALUES
(1, 2, '旺财', '狗', '金毛', 3, 28.50, 1, 1, 1, '温顺的金毛犬', '无', '喜欢散步和玩球'),
(2, 2, '咪咪', '猫', '布偶', 2, 4.50, 0, 1, 1, '安静的小公主', '海鲜', '喜欢在高处休息');

INSERT IGNORE INTO `merchant_wsh` (`id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `rating_wsh`, `status_wsh`) VALUES
(1, 3, '阳光宠物中心', '021-12345678', '上海市浦东新区', 31.2304, 121.4737, '专业宠物寄养服务', 4.80, 1);

INSERT IGNORE INTO `keeper_wsh` (`id_wsh`, `merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `status_wsh`) VALUES
(1, 1, 4, '李阿姨', '13800000004', 5, 4.90, 100.00, 0.00, 150.00, 5, 1, 1),
(2, 1, 4, '张叔叔', '13800000005', 3, 4.70, 98.50, 1.50, 120.00, 4, 2, 1),
(3, 1, 4, '王姐', '13800000006', 2, 4.50, 95.00, 2.00, 100.00, 3, 3, 1);

-- Service Categories
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

INSERT IGNORE INTO `pet_service_wsh` (`id_wsh`, `merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `images_wsh`, `status_wsh`) VALUES
(1, 1, '标准寄养', 'BOARDING_STANDARD', 11, '舒适安全的标准宠物寄养服务，含每日遛宠、定时喂食、宠物活动区，让您的爱宠享受温馨的寄养体验', 150.00, '天', 'http://localhost/minio/pet-service/services/8b02b8b7-1e23-443c-a197-95e5a7b96e43.jpg,http://localhost/minio/pet-service/services/8d4c3524-e21b-4d47-9370-00e0a12c4ede.jpg', 1),
(2, 1, '豪华寄养', 'BOARDING_VIP', 12, '豪华独立套房寄养，24小时专人看护、定制饮食计划、每日健康报告、专属游乐时间，尊享五星级宠物酒店体验', 250.00, '天', 'http://localhost/minio/pet-service/services/ae3332f4-abfe-4a0e-9867-e15c89168e23.jpg,http://localhost/minio/pet-service/services/a126c08d-42af-45b7-a895-c40336717da2.jpg', 1),
(3, 1, '基础美容', 'GROOMING_BASIC', 21, '专业宠物美容护理，含洗澡、剪毛、修甲、清洁耳朵、牙齿护理，使用进口宠物专用护理产品', 120.00, '次', 'http://localhost/minio/pet-service/services/f077e18f-2e7b-46ec-ae46-7ed020d9f2b2.jpg,http://localhost/minio/pet-service/services/33b25d1c-298e-490e-882a-a9a715a907cd.jpg', 1),
(4, 1, '基础训练', 'TRAINING_BASIC', 31, '专业宠物行为训练课程，含基本服从训练、社交训练、定点排便训练、不吠叫训练，持证训练师一对一指导', 200.00, '课时', 'http://localhost/minio/pet-service/services/e83dbb6a-cd15-4807-aa93-5714054351e3.jpg,http://localhost/minio/pet-service/services/37fc5a5c-31ec-4267-8f66-a8af8c336e3c.jpg', 1),
(5, 1, '标准遛弯', 'WALK_STANDARD', 41, '专业遛狗服务，每日30-60分钟户外活动，含安全牵引、拾便清理、提供实时路线分享，让工作繁忙的您无后顾之忧', 60.00, '次', 'http://localhost/minio/pet-service/services/34e87304-8be2-4d38-9ff5-dc31b9bbf7ef.jpg,http://localhost/minio/pet-service/services/54bb09f6-e4ef-4aba-bff7-e28fe0288518.jpg', 1),
(6, 1, '常规体检', 'MEDICAL_CHECKUP', 51, '专业宠物医疗护理服务，含定期体检、疫苗注射、驱虫护理、伤口处理、术后护理，合作宠物医院医生上门服务', 180.00, '次', 'http://localhost/minio/pet-service/services/b4cb1d2d-85e0-4e95-b93d-ff014c9bf01f.jpg,http://localhost/minio/pet-service/services/e37e4399-42c0-4b79-9412-4e82f3f7ed2c.jpg', 1);

INSERT IGNORE INTO `pet_order_wsh` (`id_wsh`, `order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`) VALUES
(1, 'ORDDEMO20260601', 2, 1, 1, 1, 1, '2026-06-01', '2026-06-03', 3, 150.00, 450.00, 50.00, 400.00, 'completed'),
(2, 'ORDDEMO20260610', 2, 2, 2, 1, 1, '2026-06-10', '2026-06-12', 2, 120.00, 240.00, 0.00, 240.00, 'paid');

INSERT IGNORE INTO `payment_wsh` (`id_wsh`, `order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`) VALUES
(1, 1, 'ORDDEMO20260601', 'PAYDEMO00000001', 400.00, 'wechat', 'success', NOW()),
(2, 2, 'ORDDEMO20260610', 'PAYDEMO00000002', 240.00, 'balance', 'success', NOW());

INSERT IGNORE INTO `rating_wsh` (`id_wsh`, `order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`) VALUES
(1, 1, 2, 1, 'keeper', 5, '服务非常好！'),
(2, 1, 2, 1, 'merchant', 4, '环境不错。');

-- Admin test pet & orders
INSERT IGNORE INTO `pet_wsh` (`id_wsh`, `owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `description_wsh`) VALUES
(3, 1, 'AdminPet', '狗', '泰迪', 2, 6.0, 1, 1, 1, 'Admin测试用宠物');
INSERT IGNORE INTO `pet_order_wsh` (`id_wsh`, `order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `total_amount_wsh`, `final_amount_wsh`, `status_wsh`) VALUES
(3, 'ORD_ADMIN_PENDING',  1, 3, 1, 1, 1, CURDATE() + 1, CURDATE() + 3, 2, 150.00, 300.00, 300.00, 'pending'),
(4, 'ORD_ADMIN_PAID',     1, 3, 1, 1, 1, CURDATE() + 2, CURDATE() + 6, 4, 150.00, 600.00, 600.00, 'paid'),
(5, 'ORD_ADMIN_CONFIRMED',1, 3, 1, 1, 1, CURDATE() + 3, CURDATE() + 5, 2, 150.00, 300.00, 300.00, 'confirmed'),
(6, 'ORD_ADMIN_INPROGRESS',1, 3, 1, 1, 1, CURDATE(),              CURDATE() + 3, 3, 150.00, 450.00, 450.00, 'in_progress'),
(7, 'ORD_ADMIN_COMPLETED', 1, 3, 1, 1, 1, CURDATE() - 5, CURDATE() - 2, 3, 150.00, 450.00, 450.00, 'completed');

INSERT IGNORE INTO `knowledge_document_wsh` (`id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `source_type_wsh`) VALUES
(6, '寄养协议', '标准寄养协议条款...', 'agreement', 'txt'),
(7, '常见问题', 'Q: 需要带什么？A: 食物、碗、床、玩具、疫苗记录\nQ: 可以探望吗？A: 可以，需预约', 'rule', 'txt');
