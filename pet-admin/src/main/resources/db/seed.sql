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

INSERT IGNORE INTO `pet_service_wsh` (`id_wsh`, `merchant_id_wsh`, `name_wsh`, `type_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `images_wsh`, `status_wsh`) VALUES
(1, 1, '标准寄养', 'boarding', '舒适安全的标准宠物寄养服务，含每日遛宠、定时喂食、宠物活动区，让您的爱宠享受温馨的寄养体验', 150.00, '天', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&q=80,https://images.unsplash.com/photo-1544568100-847a948585b9?w=800&q=80', 1),
(2, 1, '高级寄养', 'boarding', '豪华独立套房寄养，24小时专人看护、定制饮食计划、每日健康报告、专属游乐时间，尊享五星级宠物酒店体验', 250.00, '天', 'https://images.unsplash.com/photo-1513360371669-4adf3dd7dff8?w=800&q=80,https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&q=80', 1),
(3, 1, '宠物美容', 'grooming', '专业宠物美容护理，含洗澡、剪毛、修甲、清洁耳朵、牙齿护理，使用进口宠物专用护理产品', 120.00, '次', 'https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=800&q=80,https://images.unsplash.com/photo-1518288774672-b94e808873ff?w=800&q=80', 1),
(4, 1, '宠物训练', 'training', '专业宠物行为训练课程，含基本服从训练、社交训练、定点排便训练、不吠叫训练，持证训练师一对一指导', 200.00, '课时', 'https://images.unsplash.com/photo-1553882809-a4f57e595701?w=800&q=80,https://images.unsplash.com/photo-1544568100-847a948585b9?w=800&q=80', 1),
(5, 1, '宠物遛弯', 'walk', '专业遛狗服务，每日30-60分钟户外活动，含安全牵引、拾便清理、提供实时路线分享，让工作繁忙的您无后顾之忧', 60.00, '次', 'https://images.unsplash.com/photo-1554696468-8983b92c0dc4?w=800&q=80,https://images.unsplash.com/photo-1544568100-847a948585b9?w=800&q=80', 1),
(6, 1, '宠物医疗护理', 'medical', '专业宠物医疗护理服务，含定期体检、疫苗注射、驱虫护理、伤口处理、术后护理，合作宠物医院医生上门服务', 180.00, '次', 'https://images.unsplash.com/photo-1628009368231-3bb7cfcb8c9c?w=800&q=80,https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=800&q=80', 1);

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
(3, 'ORD_ADMIN_PENDING',  1, 3, 1, 1, 1, CURDATE() + INTERVAL 1 DAY, CURDATE() + INTERVAL 3 DAY, 2, 150.00, 300.00, 300.00, 'pending'),
(4, 'ORD_ADMIN_PAID',     1, 3, 1, 1, 1, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 6 DAY, 4, 150.00, 600.00, 600.00, 'paid'),
(5, 'ORD_ADMIN_CONFIRMED',1, 3, 1, 1, 1, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 5 DAY, 2, 150.00, 300.00, 300.00, 'confirmed'),
(6, 'ORD_ADMIN_INPROGRESS',1, 3, 1, 1, 1, CURDATE(),              CURDATE() + INTERVAL 3 DAY, 3, 150.00, 450.00, 450.00, 'in_progress'),
(7, 'ORD_ADMIN_COMPLETED', 1, 3, 1, 1, 1, CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 2 DAY, 3, 150.00, 450.00, 450.00, 'completed');

INSERT IGNORE INTO `knowledge_document_wsh` (`id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `source_type_wsh`) VALUES
(6, '寄养协议', '标准寄养协议条款...', 'agreement', 'txt'),
(7, '常见问题', 'Q: 需要带什么？A: 食物、碗、床、玩具、疫苗记录\nQ: 可以探望吗？A: 可以，需预约', 'rule', 'txt');
