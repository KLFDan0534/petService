-- ============================================================
-- Pet Service Comprehensive Seed Data
-- All INSERTs use IGNORE so this script is safe to re-run.
-- ============================================================

-- Roles (IDs 1-5, consistent with DataInitializer)
INSERT IGNORE INTO `role_wsh` (`id_wsh`, `name_wsh`, `code_wsh`, `description_wsh`) VALUES
(1, 'System Admin',   'ADMIN',            'System Administrator'),
(2, 'Pet Owner',      'OWNER',            'Pet Owner'),
(3, 'Pet Keeper',     'KEEPER',           'Pet Keeper'),
(4, 'Merchant',       'MERCHANT',         'Merchant'),
(5, 'Customer Service', 'CUSTOMER_SERVICE', 'Customer Service');

-- Users (IDs 1-5, password hash = BCrypt("123456"))
INSERT IGNORE INTO `user_wsh` (`id_wsh`, `username_wsh`, `password_wsh`, `nickname_wsh`, `phone_wsh`, `email_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `status_wsh`) VALUES
(1, 'admin',     '$2a$10$x.wRAVAMXvHfLg/Q7rgEdOegbuT7mu9rO7kG.ZYfnbKKJKYbQ.fU6', 'Admin',   '13800000001', 'admin@pet.com',     'Beijing',  39.9042, 116.4074, 1),
(2, 'owner',     '$2a$10$x.wRAVAMXvHfLg/Q7rgEdOegbuT7mu9rO7kG.ZYfnbKKJKYbQ.fU6', 'Tom',     '13800000002', 'owner@pet.com',     'Shanghai', 31.2304, 121.4737, 1),
(3, 'merchant1', '$2a$10$x.wRAVAMXvHfLg/Q7rgEdOegbuT7mu9rO7kG.ZYfnbKKJKYbQ.fU6', 'Mike',    '13800000003', 'merchant1@pet.com', 'Shanghai', 31.2304, 121.4737, 1),
(4, 'keeper1',   '$2a$10$x.wRAVAMXvHfLg/Q7rgEdOegbuT7mu9rO7kG.ZYfnbKKJKYbQ.fU6', 'Lucy',    '13800000004', 'keeper1@pet.com',   'Shanghai', 31.2304, 121.4737, 1),
(5, 'cs1',       '$2a$10$x.wRAVAMXvHfLg/Q7rgEdOegbuT7mu9rO7kG.ZYfnbKKJKYbQ.fU6', 'Eva',     '13800000005', 'cs1@pet.com',       'Shanghai', 31.2304, 121.4737, 1);

-- User-Role assignments
INSERT IGNORE INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`) VALUES
(1, 1), (2, 2), (3, 4), (4, 3), (5, 5), (2, 3);

-- Categories (IDs 1-8)
INSERT IGNORE INTO `category_wsh` (`id_wsh`, `name_wsh`, `parent_id_wsh`, `sort_order_wsh`) VALUES
(1, 'Dog',     0, 1),
(2, 'Cat',     0, 2),
(3, 'Bird',    0, 3),
(4, 'Reptile', 0, 4),
(5, 'Teddy',   1, 1),
(6, 'Golden',  1, 2),
(7, 'Corgi',   1, 3),
(8, 'Labrador',1, 4);

-- Pets (IDs 1-2, owner = user 2)
INSERT IGNORE INTO `pet_wsh` (`id_wsh`, `owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`) VALUES
(1, 2, 'Fluffy', 'Cat', 'Persian',        3, 5.50,  1, 1, 1, 'A lovely Persian cat',           'None',         'Likes high places and scratching posts'),
(2, 2, 'Buddy',  'Dog', 'Golden Retriever',2, 25.00, 0, 0, 1, 'Friendly Golden Retriever',       'Chicken',      'Loves fetching balls and swimming');

-- Merchant (ID 1, user = user 3)
INSERT IGNORE INTO `merchant_wsh` (`id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `rating_wsh`, `status_wsh`) VALUES
(1, 3, 'Happy Pet Store', '13800000003', 'Shanghai Pudong', 31.2304, 121.4737, 'Professional pet boarding services', 4.80, 1);

-- Keepers (IDs 1-3, same merchant ID 1)
INSERT IGNORE INTO `keeper_wsh` (`id_wsh`, `merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `status_wsh`, `bio_wsh`) VALUES
(1, 1, 4, 'Lucy',    '13800000004', 5, 4.90, 100.00, 0.00, 150.00, 5, 1, 1, 'Experienced pet care specialist'),
(2, 1, 4, 'Zhang',   '13800000005', 3, 4.70,  98.50, 1.50, 120.00, 4, 2, 1, 'Loves all animals'),
(3, 1, 4, 'Wang',    '13800000006', 2, 4.50,  95.00, 2.00, 100.00, 3, 3, 1, 'Patient and caring');

-- Service Items (IDs 1-3, merchant ID 1)
INSERT IGNORE INTO `pet_service_wsh` (`id_wsh`, `merchant_id_wsh`, `name_wsh`, `type_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `images_wsh`, `status_wsh`) VALUES
(1, 1, '标准寄养', 'boarding', '舒适安全的标准宠物寄养服务，含每日遛宠、定时喂食、宠物活动区', 150.00, 'day', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&q=80', 1),
(2, 1, '高级寄养', 'boarding', '豪华独立套房寄养，24小时专人看护、定制饮食计划、每日健康报告', 250.00, 'day', 'https://images.unsplash.com/photo-1513360371669-4adf3dd7dff8?w=800&q=80', 1),
(3, 1, '宠物美容', 'grooming', '专业宠物美容护理，含洗澡、剪毛、修甲、清洁耳朵、牙齿护理', 120.00, 'hour', 'https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=800&q=80', 1),
(4, 1, '宠物训练', 'training', '专业宠物行为训练课程，含基本服从训练、社交训练、定点排便训练', 200.00, 'class', 'https://images.unsplash.com/photo-1553882809-a4f57e595701?w=800&q=80', 1),
(5, 1, '宠物遛弯', 'walk', '专业遛狗服务，每日30-60分钟户外活动，含安全牵引、拾便清理', 60.00, 'time', 'https://images.unsplash.com/photo-1554696468-8983b92c0dc4?w=800&q=80', 1),
(6, 1, '宠物医疗护理', 'medical', '专业宠物医疗护理服务，含定期体检、疫苗注射、驱虫护理', 180.00, 'time', 'https://images.unsplash.com/photo-1628009368231-3bb7cfcb8c9c?w=800&q=80', 1);

-- Orders (IDs 1-2, completed + paid)
INSERT IGNORE INTO `pet_order_wsh` (`id_wsh`, `order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`) VALUES
(1, 'ORD20260601001', 2, 1, 1, 1, 1, '2026-06-01', '2026-06-03', 2, 150.00, 300.00, 0.00,  300.00, 'completed'),
(2, 'ORD20260610002', 2, 2, 2, 1, 1, '2026-06-10', '2026-06-12', 2, 120.00, 240.00, 0.00,  240.00, 'paid');

-- Admin test pet & orders (IDs 3+, admin user = 1)
INSERT IGNORE INTO `pet_wsh` (`id_wsh`, `owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `description_wsh`) VALUES
(3, 1, 'AdminPet', 'Dog', 'Teddy', 2, 6.0, 1, 1, 1, 'Admin测试用宠物');
INSERT IGNORE INTO `pet_order_wsh` (`id_wsh`, `order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `total_amount_wsh`, `final_amount_wsh`, `status_wsh`) VALUES
(3, 'ORD_ADMIN_PENDING',   1, 3, 1, 1, 1, CURDATE() + INTERVAL 1 DAY, CURDATE() + INTERVAL 3 DAY, 2, 150.00, 300.00, 300.00, 'pending'),
(4, 'ORD_ADMIN_PAID',      1, 3, 1, 1, 1, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 6 DAY, 4, 150.00, 600.00, 600.00, 'paid'),
(5, 'ORD_ADMIN_CONFIRMED', 1, 3, 1, 1, 1, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 5 DAY, 2, 150.00, 300.00, 300.00, 'confirmed'),
(6, 'ORD_ADMIN_INPROGRESS',1, 3, 1, 1, 1, CURDATE(),                   CURDATE() + INTERVAL 3 DAY, 3, 150.00, 450.00, 450.00, 'in_progress'),
(7, 'ORD_ADMIN_COMPLETED', 1, 3, 1, 1, 1, CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 2 DAY, 3, 150.00, 450.00, 450.00, 'completed');

-- Payments (corresponding to orders 1-2)
INSERT IGNORE INTO `payment_wsh` (`id_wsh`, `order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`) VALUES
(1, 1, 'ORD20260601001', 'PAY20260601001', 300.00, 'wechat',  'success', NOW()),
(2, 2, 'ORD20260610002', 'PAY20260610002', 240.00, 'alipay',  'success', NOW());

-- Ratings (for order 1)
INSERT IGNORE INTO `rating_wsh` (`id_wsh`, `order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`) VALUES
(1, 1, 2, 1, 'keeper',  5, 'Lucy took great care of my cat!'),
(2, 1, 2, 1, 'merchant', 4, 'Good service, convenient location');

-- Addresses (IDs 1-3, for users 2, 3, 4)
INSERT IGNORE INTO `address_wsh` (`id_wsh`, `user_id_wsh`, `label_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `detail_wsh`, `latitude_wsh`, `longitude_wsh`, `is_default_wsh`) VALUES
(1, 2, 'Home', 'Tom',     '13800000002', 'Shanghai Jingan', 'Room 101', 31.2289, 121.4581, 1),
(2, 3, 'Home', 'Mike',   '13800000003', 'Shanghai Pudong', 'Room 202', 31.2400, 121.5000, 1),
(3, 4, 'Home', 'Lucy',   '13800000004', 'Shanghai Xuhui',  'Room 303', 31.2000, 121.4400, 1);

-- Wallets (for users 1-5)
INSERT IGNORE INTO `wallet_wsh` (`id_wsh`, `user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`) VALUES
(1, 1, 10000.00, 0.00),
(2, 2,  5000.00, 0.00),
(3, 3,  8000.00, 200.00),
(4, 4,  3000.00, 0.00),
(5, 5,  2000.00, 0.00);

-- Wallet Transactions (some sample transactions)
INSERT IGNORE INTO `wallet_transaction_wsh` (`id_wsh`, `wallet_id_wsh`, `user_id_wsh`, `type_wsh`, `amount_wsh`, `balance_after_wsh`, `order_id_wsh`, `description_wsh`) VALUES
(1, 2, 2, 'expense',  300.00, 4700.00, 1, 'Payment for order ORD20260601001'),
(2, 4, 4, 'income',   300.00, 3300.00, 1, 'Service fee for order ORD20260601001');

-- Notices (IDs 1-2)
INSERT IGNORE INTO `notice_wsh` (`id_wsh`, `title_wsh`, `content_wsh`, `type_wsh`, `sort_order_wsh`, `status_wsh`) VALUES
(1, 'Welcome to Pet Service',       'Your trusted pet boarding platform!',            'notice', 1, 1);

-- Favorites (user 2 favorites merchant 1)
INSERT IGNORE INTO `favorite_wsh` (`id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`) VALUES
(1, 2, 1, 'merchant'),
(2, 2, 1, 'keeper');

-- Chat Messages (between owner and keeper, about order 1)
INSERT IGNORE INTO `chat_message_wsh` (`id_wsh`, `from_user_id_wsh`, `to_user_id_wsh`, `order_id_wsh`, `content_wsh`, `type_wsh`, `read_wsh`) VALUES
(1, 2, 4, 1, 'Hi Lucy, please take good care of Fluffy!', 'text', 1),
(2, 4, 2, 1, 'No worries! Fluffy is doing great.',          'text', 1),
(3, 2, 4, 1, 'Thanks! Can you send me some photos?',        'text', 0);

-- Complaints
INSERT IGNORE INTO `complaint_wsh` (`id_wsh`, `order_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`) VALUES
(1, 1, 2, 1, 'merchant', 'Late pickup', 'The merchant was 30 minutes late for pickup', 'resolved');

-- Tickets (IDs 1-2)
INSERT IGNORE INTO `ticket_wsh` (`id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `assignee_id_wsh`) VALUES
(1, 2, 'How to change my pet info?',  'I need to update my pet vaccination record', 'question', 'medium', 'resolved', 5),
(2, 2, 'Refund request',              'I want to request a refund for order 2',     'complaint','high',   'processing', 5);

-- Ticket Messages
INSERT IGNORE INTO `ticket_message_wsh` (`id_wsh`, `ticket_id_wsh`, `user_id_wsh`, `content_wsh`) VALUES
(1, 1, 2, 'Hi, I need to update my pet info'),
(2, 1, 5, 'Sure, please tell me what needs to be changed'),
(3, 2, 2, 'I would like to request a refund'),
(4, 2, 5, 'Processing your request');

-- Content Reviews (report on rating 1)
INSERT IGNORE INTO `content_review_wsh` (`id_wsh`, `target_type_wsh`, `target_id_wsh`, `reporter_id_wsh`, `reason_wsh`, `status_wsh`, `reviewer_id_wsh`) VALUES
(1, 'rating', 1, 3, 'Inappropriate content', 'approved', 5);

-- Care Records (for order 1)
INSERT IGNORE INTO `care_record_wsh` (`id_wsh`, `order_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `type_wsh`, `content_wsh`, `record_time_wsh`) VALUES
(1, 1, 1, 1, 'feed',     'Morning feeding: provided Royal Canin cat food',       '2026-06-01 08:00:00'),
(2, 1, 1, 1, 'activity', 'Played with feather toy for 30 minutes',                '2026-06-01 10:00:00'),
(3, 1, 1, 1, 'feed',     'Evening feeding: provided Royal Canin cat food',        '2026-06-01 18:00:00');

-- Tips (order 1, owner tips keeper)
INSERT IGNORE INTO `tip_wsh` (`id_wsh`, `order_id_wsh`, `from_user_id_wsh`, `to_user_id_wsh`, `amount_wsh`, `message_wsh`) VALUES
(1, 1, 2, 4, 50.00, 'Thank you for the excellent care!');

-- Notifications (for users)
INSERT IGNORE INTO `notification_wsh` (`id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `type_wsh`, `is_read_wsh`, `created_at_wsh`) VALUES
(1, 2, 'Order completed',        'Your order ORD20260601001 has been completed',              'order',     1, NOW()),
(2, 2, 'Rating reminder',        'Please rate your experience with keeper Lucy',              'reminder',  1, NOW()),
(3, 2, 'Ticket resolved',        'Your ticket #1 has been resolved',                          'ticket',    1, NOW()),
(4, 4, 'New tip received',       'You received a 50.00 tip from Tom',                          'payment',   1, NOW()),
(5, 2, 'Payment confirmed',      'Your payment of 240.00 for order ORD20260610002 confirmed', 'payment',   0, NOW());

-- Business Hours (merchant 1)
INSERT IGNORE INTO `business_hours_wsh` (`id_wsh`, `merchant_id_wsh`, `day_of_week_wsh`, `open_time_wsh`, `close_time_wsh`, `is_closed_wsh`, `created_at_wsh`) VALUES
(1, 1, 1, '08:00:00', '20:00:00', 0, NOW()),
(2, 1, 2, '08:00:00', '20:00:00', 0, NOW()),
(3, 1, 3, '08:00:00', '20:00:00', 0, NOW()),
(4, 1, 4, '08:00:00', '20:00:00', 0, NOW()),
(5, 1, 5, '08:00:00', '20:00:00', 0, NOW()),
(6, 1, 6, '09:00:00', '18:00:00', 0, NOW()),
(7, 1, 7, '09:00:00', '18:00:00', 0, NOW());

-- Adoption Pets (IDs 1-2, merchant 1)
INSERT IGNORE INTO `adoption_pet_wsh` (`id_wsh`, `merchant_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `color_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `health_status_wsh`, `personality_wsh`, `adoption_fee_wsh`, `status_wsh`, `created_at_wsh`) VALUES
(1, 1, 'Snow', 'Cat', 'Persian', 24, 4.00, 'F', 'White', 1, 1, 'Healthy', 'Gentle and quiet', 200.00, 'AVAILABLE', NOW()),
(2, 1, 'Max',  'Dog', 'Labrador', 12, 15.00, 'M', 'Yellow', 0, 1, 'Healthy', 'Energetic and friendly', 150.00, 'AVAILABLE', NOW());

-- Adoption Applications (user 2 applies for pet 1)
INSERT IGNORE INTO `adoption_application_wsh` (`id_wsh`, `user_id_wsh`, `pet_id_wsh`, `merchant_id_wsh`, `applicant_name_wsh`, `applicant_phone_wsh`, `applicant_address_wsh`, `housing_type_wsh`, `has_yard_wsh`, `reason_wsh`, `agree_visit_wsh`, `status_wsh`, `created_at_wsh`) VALUES
(1, 2, 1, 1, 'Tom', '13800000002', 'Shanghai Jingan Room 101', 'Apartment', 0, 'I love cats and have experience with Persians', 1, 'PENDING', NOW());

-- Knowledge Documents
INSERT IGNORE INTO `knowledge_document_wsh` (`id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `source_type_wsh`, `word_count_wsh`) VALUES
(1, 'Boarding Guide',  '1. Ensure pets are healthy before boarding\n2. Provide pet regular food\n3. Daily activity at least 2 hours\n4. Notify owner of issues immediately', 'boarding', 'txt', 120),
(2, 'Refund Policy',   '1. Full refund within 24h\n2. 3 days before: 10% fee\n3. 1 day before: 30% fee\n4. No refund after start', 'refund', 'txt', 90),
(3, 'Complaint Rules', '1. File within 7 days after service\n2. Provide evidence\n3. Processed in 3-5 business days', 'complaint', 'txt', 70),
(4, 'Care Guide',      '1. Feed on schedule\n2. Fresh water always\n3. Walk dogs twice daily\n4. Monitor health daily', 'care', 'txt', 60),
(5, 'Vaccine Requirements', '1. Core vaccines required\n2. Dogs: Rabies, Distemper, Parvo\n3. Cats: Rabies, FVRCP', 'vaccine', 'txt', 80);
