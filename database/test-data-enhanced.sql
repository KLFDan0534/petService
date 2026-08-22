-- ===================================================================
-- test-data-enhanced.sql
-- Pet Service Enhanced Test Data
-- Date: 2026-08-22
-- INSERT IGNORE to avoid duplicates
-- ===================================================================

USE `pet_service`;

SET FOREIGN_KEY_CHECKS = 0;

-- BCrypt hash (plaintext: 123456)
-- $2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS

-- ===================================================================
-- 0. Admin User
-- ===================================================================

INSERT IGNORE INTO `user_wsh` (`username_wsh`, `password_wsh`, `nickname_wsh`, `phone_wsh`, `email_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `gender_wsh`, `status_wsh`, `real_name_status_wsh`) VALUES
('admin', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '系统管理员', '13800000000', 'admin@pet-service.com', '上海市浦东新区世纪大道100号', 31.235400, 121.480000, 1, 1, 2);

INSERT IGNORE INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`)
SELECT u.id_wsh, 1 FROM `user_wsh` u WHERE u.username_wsh = 'admin';

-- ===================================================================
-- 1. New Users (adopter_test_016 ~ adopter_test_025)
-- ===================================================================

INSERT IGNORE INTO `user_wsh` (`username_wsh`, `password_wsh`, `nickname_wsh`, `phone_wsh`, `email_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `gender_wsh`, `status_wsh`, `real_name_status_wsh`, `avatar_wsh`) VALUES
('adopter_test_016', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '马小跳', '13900000026', 'adopter_test_016@pet.com', '上海市静安区南京西路1000号', 31.228800, 121.448000, 1, 1, 2, 'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=400'),
('adopter_test_017', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '田小暖', '13900000027', 'adopter_test_017@pet.com', '北京市东城区东直门内大街50号', 39.942100, 116.430000, 2, 1, 2, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400'),
('adopter_test_018', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '贺兰山', '13900000028', 'adopter_test_018@pet.com', '广州市白云区白云大道南200号', 23.161300, 113.273400, 1, 1, 2, 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=400'),
('adopter_test_019', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '林小夏', '13900000029', 'adopter_test_019@pet.com', '成都市高新区天府二街300号', 30.548000, 104.068000, 2, 1, 2, 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400'),
('adopter_test_020', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '赵大山', '13900000030', 'adopter_test_020@pet.com', '深圳市福田区深南大道1000号', 22.536200, 114.054400, 1, 1, 2, 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=400'),
('adopter_test_021', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '周小雨', '13900000031', 'adopter_test_021@pet.com', '上海市黄浦区人民大道200号', 31.230000, 121.474000, 2, 1, 2, 'https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=400'),
('adopter_test_022', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '吴明辉', '13900000032', 'adopter_test_022@pet.com', '北京市朝阳区望京街10号', 39.990000, 116.470000, 1, 1, 2, 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400'),
('adopter_test_023', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '郑小花', '13900000033', 'adopter_test_023@pet.com', '广州市番禺区大学城中环西路50号', 23.040000, 113.400000, 2, 1, 2, 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400'),
('adopter_test_024', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '孙小龙', '13900000034', 'adopter_test_024@pet.com', '成都市武侯区科华北路60号', 30.630000, 104.070000, 1, 1, 2, 'https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=400'),
('adopter_test_025', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '刘小蝶', '13900000035', 'adopter_test_025@pet.com', '深圳市南山区后海大道200号', 22.520000, 113.930000, 2, 1, 2, 'https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?w=400');

INSERT IGNORE INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`)
SELECT u.id_wsh, 2 FROM `user_wsh` u WHERE u.username_wsh IN ('adopter_test_016','adopter_test_017','adopter_test_018','adopter_test_019','adopter_test_020','adopter_test_021','adopter_test_022','adopter_test_023','adopter_test_024','adopter_test_025');

-- ===================================================================
-- 2. New Merchants (merchant_test_006 ~ merchant_test_010)
-- ===================================================================

INSERT IGNORE INTO `user_wsh` (`username_wsh`, `password_wsh`, `nickname_wsh`, `phone_wsh`, `email_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `gender_wsh`, `status_wsh`, `real_name_status_wsh`, `avatar_wsh`) VALUES
('merchant_test_006', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '爱宠之家', '13900000036', 'merchant_test_006@pet.com', '上海市浦东新区陆家嘴环路500号', 31.240000, 121.500000, 1, 1, 2, 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=200'),
('merchant_test_007', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '萌宠天地', '13900000037', 'merchant_test_007@pet.com', '北京市海淀区五道口成府路30号', 39.992000, 116.337000, 1, 1, 2, 'https://images.unsplash.com/photo-1516750105099-4b8a83e217ee?w=200'),
('merchant_test_008', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '宠物之家', '13900000038', 'merchant_test_008@pet.com', '广州市天河区珠江新城华夏路100号', 23.120000, 113.320000, 1, 1, 2, 'https://images.unsplash.com/photo-1558929996-da64ba858215?w=200'),
('merchant_test_009', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '乐宠派', '13900000039', 'merchant_test_009@pet.com', '成都市锦江区红星路三段100号', 30.655000, 104.080000, 2, 1, 2, 'https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?w=200'),
('merchant_test_010', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '宠爱有家', '13900000040', 'merchant_test_010@pet.com', '深圳市南山区海岸城天利名城50号', 22.530000, 113.950000, 2, 1, 2, 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=200');

INSERT IGNORE INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`)
SELECT u.id_wsh, 4 FROM `user_wsh` u WHERE u.username_wsh IN ('merchant_test_006','merchant_test_007','merchant_test_008','merchant_test_009','merchant_test_010');

INSERT IGNORE INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '爱宠之家', '13900000036', '上海市浦东新区陆家嘴环路500号', 31.240000, 121.500000, '高端宠物生活馆，提供寄养、美容、训练一站式服务，环境优雅设施先进', '/minio/pet-service/merchant/license_006.pdf', 4.85, 1, 1, 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=200'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_006';

INSERT IGNORE INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '萌宠天地', '13900000037', '北京市海淀区五道口成府路30号', 39.992000, 116.337000, '社区型宠物服务中心，贴心专业的宠物看护和美容，口碑极佳', '/minio/pet-service/merchant/license_007.pdf', 4.80, 1, 1, 'https://images.unsplash.com/photo-1516750105099-4b8a83e217ee?w=200'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_007';

INSERT IGNORE INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '宠物之家', '13900000038', '广州市天河区珠江新城华夏路100号', 23.120000, 113.320000, '综合宠物服务，涵盖寄养、医疗辅助、训练等，专业团队持证上岗', '/minio/pet-service/merchant/license_008.pdf', 4.90, 1, 1, 'https://images.unsplash.com/photo-1558929996-da64ba858215?w=200'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_008';

INSERT IGNORE INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '乐宠派', '13900000039', '成都市锦江区红星路三段100号', 30.655000, 104.080000, '潮流宠物生活馆，提供时尚美容和寄养服务，深受年轻人喜爱', '/minio/pet-service/merchant/license_009.pdf', 4.75, 1, 1, 'https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?w=200'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_009';

INSERT IGNORE INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '宠爱有家', '13900000040', '深圳市南山区海岸城天利名城50号', 22.530000, 113.950000, '温馨宠物寄养中心，专业看护团队，24小时在线监控', '/minio/pet-service/merchant/license_010.pdf', 4.70, 1, 1, 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=200'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_010';

-- ===================================================================
-- 3. New Keepers (keeper_test_016 ~ keeper_test_030)
-- ===================================================================

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '赵明', '13900000041', 'https://images.unsplash.com/photo-1558788353-f76d92427f16?w=400', 6, 4.85, 97.50, 0.00, 170.00, 5, 0, '专业宠物看护师，擅长大型犬日常照料和行为管理', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱宠之家' AND u.username_wsh = 'keeper_test_016';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '钱丽', '13900000042', 'https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=400', 4, 4.80, 96.00, 0.50, 150.00, 4, 0, '猫咪护理专家，温柔耐心，擅长猫咪日常护理和情绪安抚', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱宠之家' AND u.username_wsh = 'keeper_test_017';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '孙磊', '13900000043', 'https://images.unsplash.com/photo-1605568427561-40dd23c2acea?w=400', 8, 4.90, 99.00, 0.00, 200.00, 5, 0, '资深宠物美容师兼看护，擅长各类犬种美容造型', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱宠之家' AND u.username_wsh = 'keeper_test_018';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '李娜', '13900000044', 'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=400', 5, 4.82, 97.00, 0.00, 160.00, 4, 0, '宠物看护师，擅长幼犬幼猫特殊护理，细心有爱', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠天地' AND u.username_wsh = 'keeper_test_019';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '王刚', '13900000045', 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=400', 7, 4.88, 98.50, 0.00, 190.00, 5, 0, '专业训犬师，擅长犬类行为训练和社交化训练', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠天地' AND u.username_wsh = 'keeper_test_020';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '张燕', '13900000046', 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400', 3, 4.70, 95.00, 0.50, 130.00, 4, 0, '宠物看护师，热爱动物，擅长日常喂养和基础护理', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠天地' AND u.username_wsh = 'keeper_test_021';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '刘洋', '13900000047', 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=400', 6, 4.86, 98.00, 0.00, 180.00, 5, 0, '遛宠师兼看护，擅长大型犬户外运动管理', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠物之家' AND u.username_wsh = 'keeper_test_022';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '陈静', '13900000048', 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400', 4, 4.78, 96.50, 0.00, 155.00, 4, 0, '猫咪看护专家，擅长猫咪行为观察和健康管理', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠物之家' AND u.username_wsh = 'keeper_test_023';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '黄涛', '13900000049', 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400', 9, 4.92, 99.50, 0.00, 230.00, 5, 0, '资深宠物医疗护理专家，擅长术后护理和老年宠物护理', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '乐宠派' AND u.username_wsh = 'keeper_test_024';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '周芳', '13900000050', 'https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=400', 3, 4.72, 94.00, 0.50, 120.00, 4, 0, '青年宠物看护师，活泼有爱，擅长小型犬和猫咪看护', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '乐宠派' AND u.username_wsh = 'keeper_test_025';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '吴强', '13900000051', 'https://images.unsplash.com/photo-1558788353-f76d92427f16?w=400', 5, 4.83, 97.00, 0.00, 175.00, 5, 0, '专业宠物美容师，擅长泰迪、比熊等小型犬造型设计', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠爱有家' AND u.username_wsh = 'keeper_test_026';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '郑霞', '13900000052', 'https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=400', 4, 4.76, 96.00, 0.00, 145.00, 4, 0, '猫咪护理专家，擅长猫咪SPA和健康监测', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠爱有家' AND u.username_wsh = 'keeper_test_027';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '冯伟', '13900000053', 'https://images.unsplash.com/photo-1605568427561-40dd23c2acea?w=400', 7, 4.89, 98.50, 0.00, 210.00, 5, 0, '资深训犬师，擅长工作犬训练和宠物行为矫正', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠爱有家' AND u.username_wsh = 'keeper_test_028';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '沈洁', '13900000054', 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400', 2, 4.65, 93.00, 1.00, 110.00, 3, 0, '新手宠物看护师，热情负责，正在不断学习提升', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '乐宠派' AND u.username_wsh = 'keeper_test_029';

INSERT IGNORE INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`)
SELECT m.id_wsh, u.id_wsh, '韩梅', '13900000055', 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=400', 6, 4.84, 97.50, 0.00, 185.00, 4, 0, '宠物医疗辅助护理师，擅长用药管理和术后康复护理', 1
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱宠之家' AND u.username_wsh = 'keeper_test_030';

-- ===================================================================
-- 4. Business Hours for new merchants
-- ===================================================================

INSERT IGNORE INTO `business_hours_wsh` (`merchant_id_wsh`, `day_of_week_wsh`, `open_time_wsh`, `close_time_wsh`, `is_closed_wsh`)
SELECT m.id_wsh, d.day, '09:00:00', '22:00:00', 0
FROM `merchant_wsh` m
JOIN (SELECT 1 AS day UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) d
WHERE m.name_wsh IN ('爱宠之家','萌宠天地','宠物之家','乐宠派','宠爱有家');

-- ===================================================================
-- 5. New Service Items (20 more)
-- ===================================================================

-- 爱宠之家 services
INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '爱宠豪华寄养', 'boarding', 12, '独立豪华套房寄养，24小时视频监控，专人看护，每日健康报告', 260.00, 'day', 1440, 'date_range', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '爱宠之家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '爱宠精致美容', 'grooming', 22, '精致造型设计，深层清洁SPA，使用高端宠物护理产品', 180.00, 'session', 90, 'slot', 'https://images.unsplash.com/photo-1516750105099-4b8a83e217ee?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '爱宠之家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '爱宠行为训练', 'training', 31, '专业行为矫正训练，改善犬类不良习惯，培养良好社交能力', 200.00, 'session', 60, 'slot', 'https://images.unsplash.com/photo-1558929996-da64ba858215?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '爱宠之家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '爱宠健康体检', 'medical', 51, '全面健康体检，血常规、尿检、体表检查，提供健康报告', 200.00, 'session', 45, 'slot', 'https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '爱宠之家';

-- 萌宠天地 services
INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '萌宠标准寄养', 'boarding', 11, '温馨标准寄养服务，每日两次遛弯，定时喂食，专人看护', 130.00, 'day', 1440, 'date_range', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠天地';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '萌宠基础美容', 'grooming', 21, '基础洗护服务，含洗澡、吹干、修甲、耳道清洁', 80.00, 'session', 45, 'slot', 'https://images.unsplash.com/photo-1516750105099-4b8a83e217ee?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠天地';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '萌宠SPA护理', 'grooming', 23, '精油SPA按摩，毛发修复，皮肤深层护理，让宠物放松身心', 220.00, 'session', 90, 'slot', 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠天地';

-- 宠物之家 services
INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠物之家寄养', 'boarding', 11, '舒适寄养服务，宽敞活动空间，专业看护团队', 140.00, 'day', 1440, 'date_range', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠物之家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠物之家美容', 'grooming', 22, '专业美容造型，含洗护、剪毛、造型设计', 150.00, 'session', 75, 'slot', 'https://images.unsplash.com/photo-1516750105099-4b8a83e217ee?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠物之家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠物之家遛弯', 'walk', 41, '专业遛狗服务，每日45-60分钟户外活动，安全牵引', 60.00, 'session', 50, 'slot', 'https://images.unsplash.com/photo-1558929996-da64ba858215?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠物之家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠物之家疫苗', 'medical', 52, '专业疫苗接种服务，提供疫苗记录本', 120.00, 'session', 30, 'slot', 'https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠物之家';

-- 乐宠派 services
INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '乐宠时尚寄养', 'boarding', 12, '时尚主题寄养空间，网红风格装修，每日分享宠物动态', 200.00, 'day', 1440, 'date_range', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '乐宠派';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '乐宠网红美容', 'grooming', 23, '网红造型设计，创意染色，让您的宠物成为朋友圈焦点', 250.00, 'session', 120, 'slot', 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '乐宠派';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '乐宠敏捷训练', 'training', 32, '敏捷训练课程，提升宠物运动能力和协调性', 230.00, 'session', 60, 'slot', 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '乐宠派';

-- 宠爱有家 services
INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠爱标准寄养', 'boarding', 11, '温馨家庭式寄养，让宠物感受家的温暖', 120.00, 'day', 1440, 'date_range', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠爱有家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠爱猫咪美容', 'grooming', 21, '猫咪专属美容服务，温柔操作，含洗澡、剪指甲、清洁耳朵', 100.00, 'session', 50, 'slot', 'https://images.unsplash.com/photo-1516750105099-4b8a83e217ee?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠爱有家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠爱术后护理', 'medical', 53, '专业术后护理服务，伤口护理、用药管理、康复指导', 180.00, 'session', 60, 'slot', 'https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠爱有家';

INSERT IGNORE INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠爱训犬课程', 'training', 31, '一对一训犬课程，基础指令到高阶服从训练', 190.00, 'session', 60, 'slot', 'https://images.unsplash.com/photo-1558929996-da64ba858215?w=600', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠爱有家';

-- ===================================================================
-- 6. New Pets (40 more for adopter_test_016 ~ 025)
-- ===================================================================

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大毛', 'dog', '金毛寻回犬', 48, 30.00, 1, 1, 1, 'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=400', '温顺友善的金毛犬，毛发金黄漂亮', '无', '喜欢游泳和衔取物品，每天需要一小时运动'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_016';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小雪', 'cat', '布偶猫', 18, 4.00, 0, 1, 1, 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400', '蓝色眼睛的布偶猫，性格温柔粘人', '海鲜', '喜欢趴着被摸，爱玩毛线球'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_016';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '二哈', 'dog', '哈士奇', 24, 20.00, 1, 0, 1, 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400', '精力旺盛的哈士奇，表情丰富', '无', '喜欢拆家和跑步，需要大量运动'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_017';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '橘宝', 'cat', '橘猫', 30, 5.50, 1, 1, 1, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400', '胖乎乎的橘猫，贪吃可爱', '无', '喜欢吃和睡觉，性格亲人'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_017';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '旺旺', 'dog', '拉布拉多', 36, 28.00, 1, 1, 1, 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=400', '忠诚稳重的拉布拉多，服从性极佳', '无', '喜欢游泳和衔取物品，性格温顺'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_018';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '咪咪', 'cat', '暹罗猫', 24, 3.50, 0, 0, 1, 'https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=400', '暹罗猫咪咪，话多粘人，性格活泼', '无', '喜欢和人说话和跟随主人'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_018';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '豆豆', 'dog', '柯基', 18, 10.00, 0, 1, 1, 'https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=400', '短腿柯基，活泼聪明', '无', '喜欢追逐和牧羊行为，爱叫'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_019';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小白', 'cat', '英国短毛猫', 20, 4.50, 0, 1, 1, 'https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?w=400', '圆润的英短小白，性格稳重', '无', '喜欢趴着和吃罐头'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_019';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '阿黄', 'dog', '中华田园犬', 60, 22.00, 1, 1, 1, 'https://images.unsplash.com/photo-1558788353-f76d92427f16?w=400', '忠诚老实的中华田园犬，体质强健', '无', '喜欢守卫和看家，对主人忠诚'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_020';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小花', 'cat', '三花猫', 24, 3.80, 0, 1, 1, 'https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=400', '三花猫小花，色彩斑斓性格独立', '无', '喜欢独处和观察，偶尔亲人'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_020';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '毛毛', 'dog', '贵宾犬', 12, 6.00, 1, 0, 1, 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=400', '聪明的贵宾犬，学习能力强', '无', '喜欢学新把戏和讨食'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_021';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '灰灰', 'cat', '俄罗斯蓝猫', 36, 4.00, 1, 1, 1, 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400', '优雅的俄罗斯蓝猫，安静害羞', '无', '喜欢安静环境，怕生'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_021';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大壮', 'dog', '德国牧羊犬', 48, 35.00, 1, 1, 1, 'https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=400', '威武的德牧，聪明忠诚', '无', '喜欢训练和工作'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_022';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '波波', 'cat', '波斯猫', 30, 5.00, 0, 1, 1, 'https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?w=400', '毛茸茸的波斯猫，安静高雅', '无', '喜欢梳理毛发和晒太阳'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_022';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小胖', 'dog', '法国斗牛犬', 24, 11.00, 1, 0, 1, 'https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=400', '法斗小胖，憨厚可爱', '无', '喜欢睡觉和短距离散步'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_023';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '雪球', 'cat', '波斯猫', 18, 4.50, 0, 0, 1, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400', '纯白色的波斯猫，优雅可爱', '无', '喜欢在高处休息，爱玩逗猫棒'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_023';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '可乐', 'dog', '柴犬', 12, 9.00, 1, 0, 1, 'https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=400', '笑容满面的柴犬，活泼开朗', '无', '喜欢散步和晒太阳'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_024';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '美美', 'cat', '美国短毛猫', 20, 4.00, 0, 1, 1, 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400', '美短美美，花纹漂亮', '无', '喜欢玩球和攀爬'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_024';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '黑豹', 'cat', '孟买猫', 18, 3.50, 1, 1, 1, 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400', '全黑毛发的孟买猫，神秘优雅', '无', '喜欢夜间活动，爱玩激光笔'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_025';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '跳跳', 'dog', '比熊', 12, 4.50, 0, 0, 1, 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=400', '毛茸茸的比熊，甜美可爱', '无', '喜欢抱抱和撒娇'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_025';

-- More pets for earlier adopters (016-025, 2 more each)
INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小白龙', 'dog', '萨摩耶', 30, 22.00, 1, 1, 1, 'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=400', '微笑天使萨摩耶，友善热情', '无', '喜欢和人互动，爱笑'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_016';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '花花', 'cat', '狸花猫', 36, 4.00, 0, 1, 1, 'https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=400', '机灵活泼的狸花猫，体质强健', '无', '喜欢攀爬和观察窗外'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_016';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大白', 'dog', '大白熊犬', 60, 45.00, 1, 1, 1, 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=400', '大型犬大白熊，温和有耐性', '无', '喜欢户外活动，需要大量空间'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_017';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小黑', 'cat', '孟买猫', 15, 3.00, 0, 0, 1, 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400', '小黑猫，毛色纯黑发亮', '无', '喜欢夜间活动和追逐玩具'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_017';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '旺财', 'dog', '柴犬', 20, 10.00, 1, 1, 1, 'https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=400', '活泼开朗的柴犬，表情丰富', '无', '喜欢追逐和散步'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_018';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '胖橘', 'cat', '橘猫', 24, 6.00, 1, 0, 1, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400', '大胖橘，圆滚滚特别可爱', '无', '喜欢吃和睡觉'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_018';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '灰灰', 'dog', '边境牧羊犬', 24, 18.00, 1, 1, 1, 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400', '智商最高的边牧，学习能力强', '无', '喜欢飞盘和敏捷训练'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_019';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大咪', 'cat', '缅因猫', 36, 7.00, 1, 1, 1, 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400', '大型猫缅因，温柔巨人', '无', '喜欢水和玩耍'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_019';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大力', 'dog', '罗威纳', 48, 40.00, 1, 1, 1, 'https://images.unsplash.com/photo-1558788353-f76d92427f16?w=400', '强壮的罗威纳，忠诚护主', '无', '喜欢守卫和训练'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_020';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小乖', 'cat', '苏格兰折耳猫', 18, 4.00, 0, 0, 1, 'https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?w=400', '折耳猫小乖，温顺可爱', '无', '喜欢趴着和被摸'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_020';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '妞妞', 'dog', '泰迪', 24, 5.00, 0, 1, 1, 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=400', '可爱活泼的泰迪犬，聪明好学', '无', '喜欢抱抱和撒娇'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_021';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '豆沙', 'cat', '英国短毛猫', 30, 5.50, 1, 1, 1, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400', '蓝猫豆沙，性格温和', '无', '喜欢趴在窗台看外面'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_021';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '牛牛', 'dog', '澳洲牧羊犬', 30, 23.00, 1, 1, 1, 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=400', '精力充沛的澳牧，运动能力强', '无', '喜欢飞盘和跑步'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_022';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '灵犀', 'cat', '斯芬克斯无毛猫', 18, 3.50, 0, 1, 1, 'https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=400', '无毛猫灵犀，独特外型', '无', '喜欢温暖环境和拥抱'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_022';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '跳跳', 'dog', '杰克罗素梗', 18, 7.00, 1, 0, 1, 'https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=400', '精力旺盛的杰克罗素，敏捷好动', '无', '喜欢跳跃和挖洞'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_023';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '绒绒', 'cat', '挪威森林猫', 24, 6.00, 1, 1, 1, 'https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=400', '毛茸茸的挪威森林猫，体型大', '无', '喜欢攀爬和户外'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_023';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '宝贝', 'dog', '比格犬', 15, 10.00, 0, 0, 1, 'https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=400', '嗅觉灵敏的比格犬，活泼好动', '无', '喜欢追踪气味和嚎叫'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_024';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '星空', 'cat', '孟加拉豹猫', 18, 5.00, 1, 1, 1, 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400', '花纹华丽的孟加拉豹猫，活泼好动', '无', '喜欢水和攀爬'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_024';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小柯', 'dog', '柯基', 12, 9.00, 1, 0, 1, 'https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=400', '小短腿柯基，活泼可爱', '无', '喜欢追球和牧羊'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_025';

INSERT IGNORE INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '奶茶', 'cat', '暹罗猫', 12, 3.50, 0, 0, 1, 'https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=400', '奶茶色暹罗猫，话多粘人', '无', '喜欢和主人说话'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_025';
