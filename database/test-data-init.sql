-- ===================================================================
-- test-data-init.sql
-- 宠物服务项目测试数据初始化
-- 日期: 2026-08-15
-- 说明: 仅包含 INSERT 语句，不包含 DELETE/TRUNCATE/DROP/UPDATE/ALTER
-- 密码: 所有账户密码统一为 123456 (BCrypt 加密)
-- 执行顺序: 用户 → 角色 → 商家 → 看护人 → 营业时间 → 服务 → 宠物 → 领养宠物 → 领养申请 → 订单 → 支付 → 评价 → 钱包 → 资质
-- ===================================================================

USE `pet_service`;

-- BCrypt 哈希值 (明文: 123456)
-- $2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS

-- ===================================================================
-- 1. 用户测试数据 (25个: 5商家 + 15领养员 + 5看护人)
-- ===================================================================

INSERT INTO `user_wsh` (`username_wsh`, `password_wsh`, `nickname_wsh`, `phone_wsh`, `email_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `gender_wsh`, `status_wsh`, `real_name_status_wsh`) VALUES
-- 商家用户
('merchant_test_001', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '安心宠物商家', '13900000001', 'merchant_test_001@pet.com', '上海市浦东新区张江路100号', 31.230400, 121.473700, 1, 1, 2),
('merchant_test_002', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '萌宠驿站商家', '13900000002', 'merchant_test_002@pet.com', '北京市朝阳区建国路88号', 39.904200, 116.407400, 1, 1, 2),
('merchant_test_003', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '爱心动物商家', '13900000003', 'merchant_test_003@pet.com', '广州市天河区体育西路66号', 23.129100, 113.264400, 2, 1, 2),
('merchant_test_004', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '宠乐时光商家', '13900000004', 'merchant_test_004@pet.com', '成都市武侯区天府大道200号', 30.572800, 104.066800, 1, 1, 2),
('merchant_test_005', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '优品宠物商家', '13900000005', 'merchant_test_005@pet.com', '深圳市南山区科技园路300号', 22.543100, 114.057900, 2, 1, 2),
-- 领养员用户
('adopter_test_001', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '张三', '13900000006', 'adopter_test_001@pet.com', '上海市闵行区莘庄镇', 31.111700, 121.382500, 1, 1, 2),
('adopter_test_002', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '李四', '13900000007', 'adopter_test_002@pet.com', '北京市海淀区中关村大街', 39.984700, 116.307600, 1, 1, 2),
('adopter_test_003', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '王五', '13900000008', 'adopter_test_003@pet.com', '广州市越秀区中山五路', 23.135300, 113.262000, 1, 1, 2),
('adopter_test_004', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '赵六', '13900000009', 'adopter_test_004@pet.com', '成都市锦江区春熙路', 30.653500, 104.081700, 2, 1, 2),
('adopter_test_005', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '孙七', '13900000010', 'adopter_test_005@pet.com', '深圳市福田区华强北', 22.541000, 114.058300, 1, 1, 2),
('adopter_test_006', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '周八', '13900000011', 'adopter_test_006@pet.com', '上海市徐汇区漕河泾', 31.174400, 121.399800, 2, 1, 2),
('adopter_test_007', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '吴九', '13900000012', 'adopter_test_007@pet.com', '北京市西城区西单', 39.909700, 116.372300, 1, 1, 2),
('adopter_test_008', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '郑十', '13900000013', 'adopter_test_008@pet.com', '广州市海珠区江南大道', 23.083300, 113.317200, 2, 1, 2),
('adopter_test_009', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '钱多多', '13900000014', 'adopter_test_009@pet.com', '成都市青羊区人民公园', 30.662500, 104.062800, 1, 1, 2),
('adopter_test_010', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '刘畅', '13900000015', 'adopter_test_010@pet.com', '深圳市罗湖区东门', 22.548500, 114.122600, 2, 1, 2),
('adopter_test_011', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '陈晨', '13900000016', 'adopter_test_011@pet.com', '上海市长宁区中山公园', 31.418800, 121.421600, 1, 1, 2),
('adopter_test_012', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '杨光', '13900000017', 'adopter_test_012@pet.com', '北京市丰台区方庄', 39.865300, 116.431200, 1, 1, 2),
('adopter_test_013', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '黄丽', '13900000018', 'adopter_test_013@pet.com', '广州市番禺区市桥', 22.937300, 113.364000, 2, 1, 2),
('adopter_test_014', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '林峰', '13900000019', 'adopter_test_014@pet.com', '成都市金牛区荷花池', 30.697500, 104.044000, 1, 1, 2),
('adopter_test_015', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '徐敏', '13900000020', 'adopter_test_015@pet.com', '深圳市宝安区西乡', 22.556000, 113.846300, 2, 1, 2),
-- 看护人用户
('keeper_test_001', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '看护张师傅', '13900000021', 'keeper_test_001@pet.com', '上海市浦东新区', 31.230400, 121.473700, 1, 1, 2),
('keeper_test_002', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '看护李师傅', '13900000022', 'keeper_test_002@pet.com', '上海市浦东新区', 31.230400, 121.473700, 2, 1, 2),
('keeper_test_003', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '看护王师傅', '13900000023', 'keeper_test_003@pet.com', '北京市朝阳区', 39.904200, 116.407400, 1, 1, 2),
('keeper_test_004', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '看护赵师傅', '13900000024', 'keeper_test_004@pet.com', '广州市天河区', 23.129100, 113.264400, 2, 1, 2),
('keeper_test_005', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '看护陈师傅', '13900000025', 'keeper_test_005@pet.com', '成都市武侯区', 30.572800, 104.066800, 1, 1, 2),
-- 新增看护人用户 (keeper_test_006 ~ keeper_test_015)
('keeper_test_006', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '周师傅', '13900000016', 'keeper_test_006@pet.com', '上海市浦东新区张江路100号', 31.230400, 121.473700, 1, 1, 2),
('keeper_test_007', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '吴师傅', '13900000017', 'keeper_test_007@pet.com', '北京市朝阳区建国路88号', 39.904200, 116.407396, 1, 1, 2),
('keeper_test_008', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '郑师傅', '13900000018', 'keeper_test_008@pet.com', '北京市朝阳区建国路88号', 39.904200, 116.407396, 1, 1, 2),
('keeper_test_009', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '孙师傅', '13900000019', 'keeper_test_009@pet.com', '广州市天河区天河路200号', 23.129100, 113.264385, 1, 1, 2),
('keeper_test_010', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '钱师傅', '13900000020', 'keeper_test_010@pet.com', '广州市天河区天河路200号', 23.129100, 113.264385, 1, 1, 2),
('keeper_test_011', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '冯师傅', '13900000021', 'keeper_test_011@pet.com', '成都市武侯区天府大道300号', 30.572810, 104.066800, 1, 1, 2),
('keeper_test_012', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '沈师傅', '13900000022', 'keeper_test_012@pet.com', '成都市武侯区天府大道300号', 30.572810, 104.066800, 1, 1, 2),
('keeper_test_013', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '褚师傅', '13900000023', 'keeper_test_013@pet.com', '深圳市南山区科技园路400号', 22.543100, 114.057900, 1, 1, 2),
('keeper_test_014', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '卫师傅', '13900000024', 'keeper_test_014@pet.com', '深圳市南山区科技园路400号', 22.543100, 114.057900, 1, 1, 2),
('keeper_test_015', '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS', '蒋师傅', '13900000025', 'keeper_test_015@pet.com', '深圳市南山区科技园路400号', 22.543100, 114.057900, 1, 1, 2);

-- ===================================================================
-- 2. 用户角色关联
-- ===================================================================

-- 商家角色 (role_id=4)
INSERT INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`)
SELECT u.id_wsh, 4 FROM `user_wsh` u WHERE u.username_wsh IN ('merchant_test_001','merchant_test_002','merchant_test_003','merchant_test_004','merchant_test_005');

-- 领养员角色 (role_id=2)
INSERT INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`)
SELECT u.id_wsh, 2 FROM `user_wsh` u WHERE u.username_wsh IN ('adopter_test_001','adopter_test_002','adopter_test_003','adopter_test_004','adopter_test_005','adopter_test_006','adopter_test_007','adopter_test_008','adopter_test_009','adopter_test_010','adopter_test_011','adopter_test_012','adopter_test_013','adopter_test_014','adopter_test_015');

-- 看护人角色 (role_id=3)
INSERT INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`)
SELECT u.id_wsh, 3 FROM `user_wsh` u WHERE u.username_wsh IN ('keeper_test_001','keeper_test_002','keeper_test_003','keeper_test_004','keeper_test_005','keeper_test_006','keeper_test_007','keeper_test_008','keeper_test_009','keeper_test_010','keeper_test_011','keeper_test_012','keeper_test_013','keeper_test_014','keeper_test_015');

-- ===================================================================
-- 3. 商家记录 (5家)
-- ===================================================================

INSERT INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `store_mode_wsh`, `store_status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '安心宠物生活馆', '13900000001', '上海市浦东新区张江路100号', 31.230400, 121.473700, '专业宠物寄养、美容、医疗服务，拥有10年行业经验，环境优美，设施齐全', '/minio/pet-service/merchant/license_001.jpg', 4.90, 1, 0, 1, 1, '/minio/pet-service/merchant/avatar_001.jpg'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_001';

INSERT INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `store_mode_wsh`, `store_status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '萌宠驿站', '13900000002', '北京市朝阳区建国路88号', 39.904200, 116.407400, '温馨宠物寄养中心，提供豪华寄养、SPA护理、训练课程，24小时监控', '/minio/pet-service/merchant/license_002.jpg', 4.80, 1, 0, 1, 1, '/minio/pet-service/merchant/avatar_002.jpg'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_002';

INSERT INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `store_mode_wsh`, `store_status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '爱心动物之家', '13900000003', '广州市天河区体育西路66号', 23.129100, 113.264400, '爱心宠物服务机构，提供日间寄养、美容、遛宠服务，关注流浪动物救助', '/minio/pet-service/merchant/license_003.jpg', 4.70, 1, 0, 1, 1, '/minio/pet-service/merchant/avatar_003.jpg'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_003';

INSERT INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `store_mode_wsh`, `store_status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '宠乐时光寄养中心', '13900000004', '成都市武侯区天府大道200号', 30.572800, 104.066800, '高端宠物寄养中心，提供标准寄养、全效美容、进阶训练，持证专业团队', '/minio/pet-service/merchant/license_004.jpg', 4.85, 1, 0, 1, 1, '/minio/pet-service/merchant/avatar_004.jpg'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_004';

INSERT INTO `merchant_wsh` (`user_id_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `description_wsh`, `business_license_wsh`, `rating_wsh`, `status_wsh`, `store_mode_wsh`, `store_status_wsh`, `future_booking_enabled_wsh`, `avatar_wsh`)
SELECT u.id_wsh, '优品宠物服务中心', '13900000005', '深圳市南山区科技园路300号', 22.543100, 114.057900, '综合宠物服务中心，提供寄养、美容、疫苗接种等全方位服务', '/minio/pet-service/merchant/license_005.jpg', 4.75, 1, 0, 1, 1, '/minio/pet-service/merchant/avatar_005.jpg'
FROM `user_wsh` u WHERE u.username_wsh = 'merchant_test_005';

-- ===================================================================
-- 4. 看护人记录 (5名)
-- ===================================================================

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `status_wsh`, `bio_wsh`)
SELECT m.id_wsh, u.id_wsh, '张师傅', '13900000021', 8, 4.95, 100.00, 0.00, 150.00, 5, 0, 1, '拥有8年宠物看护经验，擅长大型犬护理，持宠物护理师资格证'
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '安心宠物生活馆' AND u.username_wsh = 'keeper_test_001';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `status_wsh`, `bio_wsh`)
SELECT m.id_wsh, u.id_wsh, '李师傅', '13900000022', 5, 4.80, 98.50, 0.50, 120.00, 4, 0, 1, '5年宠物美容与看护经验，擅长猫咪护理，温柔耐心'
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '安心宠物生活馆' AND u.username_wsh = 'keeper_test_002';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `status_wsh`, `bio_wsh`)
SELECT m.id_wsh, u.id_wsh, '王师傅', '13900000023', 6, 4.90, 99.00, 0.00, 130.00, 5, 0, 1, '6年宠物训练与看护经验，擅长犬类行为训练，专业训犬师'
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠驿站' AND u.username_wsh = 'keeper_test_003';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `status_wsh`, `bio_wsh`)
SELECT m.id_wsh, u.id_wsh, '赵师傅', '13900000024', 4, 4.70, 97.00, 1.00, 100.00, 3, 0, 1, '4年宠物看护经验，擅长小型犬和猫咪护理，细心负责'
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱心动物之家' AND u.username_wsh = 'keeper_test_004';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `status_wsh`, `bio_wsh`)
SELECT m.id_wsh, u.id_wsh, '陈师傅', '13900000025', 7, 4.85, 98.00, 0.50, 140.00, 4, 0, 1, '7年宠物看护与医疗辅助经验，擅长术后护理和老年宠物看护'
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠乐时光寄养中心' AND u.username_wsh = 'keeper_test_005';

-- 新增看护人记录 (keeper_test_006 ~ keeper_test_015, 每个商家至少3个看护人)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '周师傅', '13900000016', '/minio/pet-service/keeper/avatar_016.jpg', 8, 4.85, 97.00, 0.00, 220.00, 5, 0, '高级宠物美容师，擅长各类犬种造型设计和SPA护理，从业8年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '安心宠物生活馆' AND u.username_wsh = 'keeper_test_006';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '吴师傅', '13900000017', '/minio/pet-service/keeper/avatar_017.jpg', 6, 4.80, 96.00, 0.00, 180.00, 4, 0, '专业训犬师，擅长犬类行为矫正和服从训练，从业6年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠驿站' AND u.username_wsh = 'keeper_test_007';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '郑师傅', '13900000018', '/minio/pet-service/keeper/avatar_018.jpg', 4, 4.75, 95.00, 0.00, 150.00, 4, 0, '宠物寄养护理师，擅长日常护理和喂养管理，从业4年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠驿站' AND u.username_wsh = 'keeper_test_008';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '孙师傅', '13900000019', '/minio/pet-service/keeper/avatar_019.jpg', 7, 4.88, 98.00, 0.00, 160.00, 6, 0, '专业遛宠师，擅长大型犬运动管理，从业7年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱心动物之家' AND u.username_wsh = 'keeper_test_009';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '钱师傅', '13900000020', '/minio/pet-service/keeper/avatar_020.jpg', 5, 4.82, 97.00, 0.00, 200.00, 4, 0, '宠物医疗辅助护理师，擅长术后护理和用药管理，从业5年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱心动物之家' AND u.username_wsh = 'keeper_test_010';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '冯师傅', '13900000021', '/minio/pet-service/keeper/avatar_021.jpg', 9, 4.92, 99.00, 0.00, 250.00, 5, 0, '资深宠物美容师，擅长创意造型和毛色护理，从业9年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠乐时光寄养中心' AND u.username_wsh = 'keeper_test_011';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '沈师傅', '13900000022', '/minio/pet-service/keeper/avatar_022.jpg', 3, 4.70, 94.00, 0.00, 130.00, 4, 0, '青年训犬师，擅长幼犬社会化训练和基础指令教学，从业3年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠乐时光寄养中心' AND u.username_wsh = 'keeper_test_012';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '褚师傅', '13900000023', '/minio/pet-service/keeper/avatar_023.jpg', 10, 4.95, 99.00, 0.00, 300.00, 4, 0, '高级宠物医疗护理专家，擅长急重症护理和康复治疗，从业10年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '优品宠物服务中心' AND u.username_wsh = 'keeper_test_013';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '卫师傅', '13900000024', '/minio/pet-service/keeper/avatar_024.jpg', 6, 4.85, 97.00, 0.00, 190.00, 5, 0, '宠物美容师，擅长猫咪美容和指甲护理，从业6年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '优品宠物服务中心' AND u.username_wsh = 'keeper_test_014';

INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '蒋师傅', '13900000025', '/minio/pet-service/keeper/avatar_025.jpg', 4, 4.78, 96.00, 0.00, 160.00, 5, 0, '宠物寄养护理师，擅长老年犬猫日常照料和特殊饮食管理，从业4年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '优品宠物服务中心' AND u.username_wsh = 'keeper_test_015';

-- ===================================================================
-- 5. 营业时间 (5商家 x 7天 = 35条)
-- ===================================================================

INSERT INTO `business_hours_wsh` (`merchant_id_wsh`, `day_of_week_wsh`, `open_time_wsh`, `close_time_wsh`, `is_closed_wsh`)
SELECT m.id_wsh, d.day, '08:00:00', '23:59:00', 0
FROM `merchant_wsh` m
JOIN (SELECT 1 AS day UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) d
WHERE m.name_wsh IN ('安心宠物生活馆','萌宠驿站','爱心动物之家','宠乐时光寄养中心','优品宠物服务中心');

-- ===================================================================
-- 6. 服务项目 (15条, 每商家3个)
-- ===================================================================

-- 商家1: 安心宠物生活馆
INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '安心标准寄养', 'boarding', 11, '舒适安全的标准宠物寄养服务，含每日遛宠、定时喂食、宠物活动区，让您的爱宠享受温馨的寄养体验', 150.00, 'day', 1440, 'date_range', '/minio/pet-service/services/service_001.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '安心宠物生活馆';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '安心宠物美容', 'grooming', 21, '专业宠物美容护理，含洗澡、剪毛、修甲、清洁耳朵，使用进口宠物专用护理产品', 120.00, 'session', 60, 'slot', '/minio/pet-service/services/service_002.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '安心宠物生活馆';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '安心健康检查', 'medical', 51, '专业宠物医疗护理服务，含定期体检、疫苗注射、驱虫护理，合作宠物医院医生上门服务', 180.00, 'session', 30, 'slot', '/minio/pet-service/services/service_003.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '安心宠物生活馆';

-- 商家2: 萌宠驿站
INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '萌宠豪华寄养', 'boarding', 12, '豪华独立套房寄养，24小时专人看护、定制饮食计划、每日健康报告、专属游乐时间', 280.00, 'day', 1440, 'date_range', '/minio/pet-service/services/service_004.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠驿站';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '萌宠SPA护理', 'grooming', 23, '深层清洁SPA护理，含精油按摩、毛发修复、皮肤护理，让宠物放松身心', 200.00, 'session', 90, 'slot', '/minio/pet-service/services/service_005.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠驿站';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '萌宠基础训练', 'training', 31, '专业宠物行为训练课程，含基本服从训练、社交训练、定点排便训练，持证训练师一对一指导', 180.00, 'session', 60, 'slot', '/minio/pet-service/services/service_006.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠驿站';

-- 商家3: 爱心动物之家
INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '爱心日间寄养', 'boarding', 13, '日间寄养服务，适合上班期间宠物照看，含两餐、活动时间、午休安排', 80.00, 'day', 720, 'date_range', '/minio/pet-service/services/service_007.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '爱心动物之家';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '爱心基础美容', 'grooming', 21, '基础宠物美容服务，含洗澡、吹干、修甲、耳道清洁，经济实惠', 90.00, 'session', 45, 'slot', '/minio/pet-service/services/service_008.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '爱心动物之家';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '爱心标准遛弯', 'walk', 41, '专业遛狗服务，每日30-60分钟户外活动，含安全牵引、拾便清理', 50.00, 'session', 45, 'slot', '/minio/pet-service/services/service_009.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '爱心动物之家';

-- 商家4: 宠乐时光寄养中心
INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠乐标准寄养', 'boarding', 11, '舒适标准寄养服务，独立空间、每日活动、专业看护', 130.00, 'day', 1440, 'date_range', '/minio/pet-service/services/service_010.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠乐时光寄养中心';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠乐全效美容', 'grooming', 22, '全效美容护理，含深层洗浴、毛发修剪、造型设计、SPA护理', 160.00, 'session', 90, 'slot', '/minio/pet-service/services/service_011.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠乐时光寄养中心';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '宠乐进阶训练', 'training', 32, '进阶训练课程，含高阶服从、敏捷训练、特技训练，适合有一定基础的宠物', 220.00, 'session', 60, 'slot', '/minio/pet-service/services/service_012.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '宠乐时光寄养中心';

-- 商家5: 优品宠物服务中心
INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '优品标准寄养', 'boarding', 11, '优质标准寄养服务，宽敞空间、专业护理、实时监控', 140.00, 'day', 1440, 'date_range', '/minio/pet-service/services/service_013.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '优品宠物服务中心';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '优品宠物美容', 'grooming', 21, '专业宠物美容，含洗护、造型、护理，资深美容师服务', 110.00, 'session', 60, 'slot', '/minio/pet-service/services/service_014.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '优品宠物服务中心';

INSERT INTO `pet_service_wsh` (`merchant_id_wsh`, `name_wsh`, `type_wsh`, `category_id_wsh`, `description_wsh`, `price_wsh`, `unit_wsh`, `duration_minutes_wsh`, `booking_mode_wsh`, `images_wsh`, `status_wsh`)
SELECT m.id_wsh, '优品疫苗接种', 'medical', 52, '专业疫苗接种服务，含狂犬疫苗、六联疫苗、驱虫，提供疫苗记录本', 100.00, 'session', 30, 'slot', '/minio/pet-service/services/service_015.jpg', 1
FROM `merchant_wsh` m WHERE m.name_wsh = '优品宠物服务中心';

-- ===================================================================
-- 7. 宠物数据 (35条)
-- ===================================================================

-- adopter_test_001 的宠物 (3条)
INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '旺财', 'dog', '金毛寻回犬', 36, 28.50, 1, 1, 1, '/minio/pet-service/pets/pet_001.jpg', '温顺友善的金毛犬，喜欢和人互动', '无', '喜欢散步和玩球，每天需要至少一小时运动'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_001';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '咪咪', 'cat', '布偶猫', 24, 4.50, 0, 1, 1, '/minio/pet-service/pets/pet_002.jpg', '安静优雅的布偶猫，性格温柔', '海鲜', '喜欢在高处休息，爱玩逗猫棒'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_001';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '阿福', 'dog', '柴犬', 18, 12.00, 1, 0, 1, '/minio/pet-service/pets/pet_003.jpg', '活泼好动的柴犬，表情丰富', '无', '喜欢追球和挖洞，需要充足运动量'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_001';

-- adopter_test_002 的宠物 (3条)
INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大黄', 'dog', '拉布拉多', 48, 30.00, 1, 1, 1, '/minio/pet-service/pets/pet_004.jpg', '忠诚稳重的拉布拉多，服从性极佳', '无', '喜欢游泳和衔取物品，性格温顺'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_002';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小黑', 'cat', '孟买猫', 12, 3.50, 0, 0, 1, '/minio/pet-service/pets/pet_005.jpg', '全黑毛发的孟买猫，神秘优雅', '无', '喜欢夜间活动，爱玩激光笔'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_002';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小花', 'cat', '狸花猫', 36, 4.00, 0, 1, 1, '/minio/pet-service/pets/pet_006.jpg', '机灵活泼的狸花猫，体质强健', '无', '喜欢攀爬和观察窗外，独立性强'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_002';

-- adopter_test_003 的宠物 (3条)
INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '豆豆', 'dog', '泰迪', 24, 5.00, 1, 0, 1, '/minio/pet-service/pets/pet_007.jpg', '可爱活泼的泰迪犬，聪明好学', '无', '喜欢抱抱和撒娇，爱叫'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_003';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '雪球', 'cat', '波斯猫', 30, 5.00, 0, 1, 1, '/minio/pet-service/pets/pet_008.jpg', '毛茸茸的波斯猫，安静高雅', '无', '喜欢梳理毛发和晒太阳'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_003';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大白', 'dog', '大白熊犬', 60, 45.00, 1, 1, 1, '/minio/pet-service/pets/pet_009.jpg', '大型犬大白熊，温和有耐性', '无', '喜欢户外活动，需要大量空间'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_003';

-- adopter_test_004 的宠物 (3条)
INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '闪电', 'dog', '哈士奇', 30, 22.00, 1, 0, 1, '/minio/pet-service/pets/pet_010.jpg', '精力旺盛的哈士奇，表情包制造机', '无', '喜欢拆家和跑步，需要大量运动'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_004';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '橘子', 'cat', '橘猫', 18, 5.00, 1, 0, 1, '/minio/pet-service/pets/pet_011.jpg', '胖乎乎的橘猫，贪吃可爱', '无', '喜欢吃和睡觉，性格亲人'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_004';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小橘', 'cat', '橘猫', 6, 2.00, 0, 0, 0, '/minio/pet-service/pets/pet_012.jpg', '幼年橘猫，活泼好动', '无', '喜欢玩逗猫棒和追毛球'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_004';

-- adopter_test_005 的宠物 (3条)
INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '麻辣', 'dog', '柯基', 24, 10.00, 0, 1, 1, '/minio/pet-service/pets/pet_013.jpg', '短腿柯基，活泼聪明', '无', '喜欢追赶和牧羊行为，爱叫'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '花花', 'cat', '三花猫', 36, 4.50, 0, 1, 1, '/minio/pet-service/pets/pet_014.jpg', '三花猫，色彩斑斓性格独立', '无', '喜欢独处和观察，偶尔亲人'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '旺财二号', 'dog', '拉布拉多', 12, 15.00, 1, 0, 1, '/minio/pet-service/pets/pet_015.jpg', '年幼拉布拉多，活泼好动', '无', '喜欢咬东西和玩耍，需要训练'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

-- adopter_test_006 ~ 015 的宠物 (各2条, 共20条)
INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '元宝', 'dog', '贵宾犬', 18, 8.00, 1, 1, 1, '/minio/pet-service/pets/pet_016.jpg', '聪明的贵宾犬，学习能力强', '无', '喜欢学新把戏和讨食'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_006';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小白', 'dog', '萨摩耶', 36, 20.00, 0, 1, 1, '/minio/pet-service/pets/pet_017.jpg', '微笑天使萨摩耶，友善热情', '无', '喜欢和人互动，爱笑'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_006';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '黑豹', 'dog', '边境牧羊犬', 24, 18.00, 1, 1, 1, '/minio/pet-service/pets/pet_018.jpg', '智商最高的边牧，学习能力强', '无', '喜欢飞盘和敏捷训练'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_007';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '灰灰', 'cat', '俄罗斯蓝猫', 30, 4.50, 1, 1, 1, '/minio/pet-service/pets/pet_019.jpg', '优雅的俄罗斯蓝猫，安静害羞', '无', '喜欢安静环境，怕生'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_007';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '甜甜', 'dog', '比熊', 12, 5.00, 0, 0, 1, '/minio/pet-service/pets/pet_020.jpg', '毛茸茸的比熊，甜美可爱', '无', '喜欢抱抱和撒娇'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_008';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '胖虎', 'cat', '英国短毛猫', 24, 6.00, 1, 0, 1, '/minio/pet-service/pets/pet_021.jpg', '圆润的英短，性格稳重', '无', '喜欢趴着和吃罐头'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_008';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '飞毛', 'dog', '澳洲牧羊犬', 36, 25.00, 1, 1, 1, '/minio/pet-service/pets/pet_022.jpg', '精力充沛的澳牧，运动能力强', '无', '喜欢飞盘和跑步'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_009';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '丫丫', 'cat', '缅因猫', 30, 7.00, 0, 1, 1, '/minio/pet-service/pets/pet_023.jpg', '大型猫缅因，温柔巨人', '无', '喜欢水和玩耍'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_009';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '大力', 'dog', '罗威纳', 48, 40.00, 1, 1, 1, '/minio/pet-service/pets/pet_024.jpg', '强壮的罗威纳，忠诚护主', '无', '喜欢守卫和训练'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_010';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小乖', 'cat', '苏格兰折耳猫', 18, 4.00, 0, 0, 1, '/minio/pet-service/pets/pet_025.jpg', '折耳猫小乖，温顺可爱', '无', '喜欢趴着和被摸'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_010';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '快乐', 'dog', '柴犬', 20, 10.00, 1, 0, 1, '/minio/pet-service/pets/pet_026.jpg', '笑容满面的柴犬，活泼开朗', '无', '喜欢散步和晒太阳'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_011';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '美美', 'cat', '美国短毛猫', 24, 4.50, 0, 1, 1, '/minio/pet-service/pets/pet_027.jpg', '美短美美，花纹漂亮', '无', '喜欢玩球和攀爬'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_011';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '威风', 'dog', '德国牧羊犬', 36, 32.00, 1, 1, 1, '/minio/pet-service/pets/pet_028.jpg', '威武的德牧，聪明忠诚', '无', '喜欢训练和工作'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_012';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '乖乖', 'cat', '暹罗猫', 20, 4.00, 0, 0, 1, '/minio/pet-service/pets/pet_029.jpg', '暹罗猫乖乖，话多粘人', '无', '喜欢和人说话和跟随主人'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_012';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '小胖', 'dog', '法国斗牛犬', 18, 11.00, 1, 0, 1, '/minio/pet-service/pets/pet_030.jpg', '法斗小胖，憨厚可爱', '无', '喜欢睡觉和短距离散步'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_013';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '灵犀', 'cat', '斯芬克斯无毛猫', 24, 3.50, 0, 1, 1, '/minio/pet-service/pets/pet_031.jpg', '无毛猫灵犀，独特外型', '无', '喜欢温暖环境和拥抱'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_013';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '跳跳', 'dog', '杰克罗素梗', 24, 7.00, 1, 0, 1, '/minio/pet-service/pets/pet_032.jpg', '精力旺盛的杰克罗素，敏捷好动', '无', '喜欢跳跃和挖洞'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_014';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '绒绒', 'cat', '挪威森林猫', 30, 6.00, 1, 1, 1, '/minio/pet-service/pets/pet_033.jpg', '毛茸茸的挪威森林猫，体型大', '无', '喜欢攀爬和户外'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_014';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '宝贝', 'dog', '比格犬', 18, 10.00, 0, 0, 1, '/minio/pet-service/pets/pet_034.jpg', '嗅觉灵敏的比格犬，活泼好动', '无', '喜欢追踪气味和嚎叫'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_015';

INSERT INTO `pet_wsh` (`owner_id_wsh`, `name_wsh`, `type_wsh`, `breed_wsh`, `age_wsh`, `weight_wsh`, `gender_wsh`, `sterilized_wsh`, `vaccinated_wsh`, `avatar_wsh`, `description_wsh`, `allergies_wsh`, `habits_wsh`)
SELECT u.id_wsh, '星空', 'cat', '孟加拉豹猫', 24, 5.00, 1, 1, 1, '/minio/pet-service/pets/pet_035.jpg', '花纹华丽的孟加拉豹猫，活泼好动', '无', '喜欢水和攀爬'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_015';

-- ===================================================================
-- 10. 订单数据 (15条, 覆盖不同状态)
-- ===================================================================

-- 订单1: pending (待支付)
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`)
SELECT 'ORDTEST20260815001', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-20', '2026-08-22', 2, 150.00, 'day', 2, 150.00, 300.00, 0.00, 300.00, 'pending'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_001' AND p.name_wsh = '旺财' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '张师傅' AND m.name_wsh = '安心宠物生活馆' AND s.name_wsh = '安心标准寄养' AND s.merchant_id_wsh = m.id_wsh;

-- 订单2: paid (已支付)
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`)
SELECT 'ORDTEST20260815002', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-21', '2026-08-21', 1, 120.00, 'session', 1, 120.00, 120.00, 0.00, 120.00, 'paid'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_002' AND p.name_wsh = '大黄' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '李师傅' AND m.name_wsh = '安心宠物生活馆' AND s.name_wsh = '安心宠物美容' AND s.merchant_id_wsh = m.id_wsh;

-- 订单3: in_progress (服务中)
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`)
SELECT 'ORDTEST20260815003', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-14', '2026-08-17', 3, 280.00, 'day', 3, 280.00, 840.00, 40.00, 800.00, 'in_progress', '2026-08-14 09:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_003' AND p.name_wsh = '豆豆' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '王师傅' AND m.name_wsh = '萌宠驿站' AND s.name_wsh = '萌宠豪华寄养' AND s.merchant_id_wsh = m.id_wsh;

-- 订单4: completed (已完成)
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`, `completed_at_wsh`)
SELECT 'ORDTEST20260815004', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-01', '2026-08-05', 4, 80.00, 'day', 4, 80.00, 320.00, 20.00, 300.00, 'completed', '2026-08-01 09:00:00', '2026-08-05 18:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_004' AND p.name_wsh = '闪电' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '赵师傅' AND m.name_wsh = '爱心动物之家' AND s.name_wsh = '爱心日间寄养' AND s.merchant_id_wsh = m.id_wsh;

-- 订单5: completed
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`, `completed_at_wsh`)
SELECT 'ORDTEST20260815005', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-07-28', '2026-07-30', 2, 130.00, 'day', 2, 130.00, 260.00, 0.00, 260.00, 'completed', '2026-07-28 09:00:00', '2026-07-30 18:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_005' AND p.name_wsh = '麻辣' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '陈师傅' AND m.name_wsh = '宠乐时光寄养中心' AND s.name_wsh = '宠乐标准寄养' AND s.merchant_id_wsh = m.id_wsh;

-- 订单6: cancelled (已取消)
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`)
SELECT 'ORDTEST20260815006', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-25', '2026-08-25', 1, 120.00, 'session', 1, 120.00, 120.00, 0.00, 120.00, 'cancelled'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_006' AND p.name_wsh = '元宝' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '张师傅' AND m.name_wsh = '安心宠物生活馆' AND s.name_wsh = '安心宠物美容' AND s.merchant_id_wsh = m.id_wsh;

-- 订单7: paid
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`)
SELECT 'ORDTEST20260815007', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-22', '2026-08-22', 1, 180.00, 'session', 1, 180.00, 180.00, 0.00, 180.00, 'paid'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_007' AND p.name_wsh = '黑豹' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '王师傅' AND m.name_wsh = '萌宠驿站' AND s.name_wsh = '萌宠基础训练' AND s.merchant_id_wsh = m.id_wsh;

-- 订单8: completed
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`, `completed_at_wsh`)
SELECT 'ORDTEST20260815008', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-07-20', '2026-07-20', 1, 90.00, 'session', 1, 90.00, 90.00, 0.00, 90.00, 'completed', '2026-07-20 10:00:00', '2026-07-20 11:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_008' AND p.name_wsh = '甜甜' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '赵师傅' AND m.name_wsh = '爱心动物之家' AND s.name_wsh = '爱心基础美容' AND s.merchant_id_wsh = m.id_wsh;

-- 订单9: paid
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`)
SELECT 'ORDTEST20260815009', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-23', '2026-08-23', 1, 220.00, 'session', 1, 220.00, 220.00, 0.00, 220.00, 'paid'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_009' AND p.name_wsh = '飞毛' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '陈师傅' AND m.name_wsh = '宠乐时光寄养中心' AND s.name_wsh = '宠乐进阶训练' AND s.merchant_id_wsh = m.id_wsh;

-- 订单10: in_progress
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`)
SELECT 'ORDTEST20260815010', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-14', '2026-08-16', 2, 140.00, 'day', 2, 140.00, 280.00, 0.00, 280.00, 'in_progress', '2026-08-14 09:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_010' AND p.name_wsh = '大力' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '张师傅' AND m.name_wsh = '优品宠物服务中心' AND s.name_wsh = '优品标准寄养' AND s.merchant_id_wsh = m.id_wsh;

-- 订单11: completed
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`, `completed_at_wsh`)
SELECT 'ORDTEST20260815011', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-07-15', '2026-07-15', 1, 180.00, 'session', 1, 180.00, 180.00, 0.00, 180.00, 'completed', '2026-07-15 10:00:00', '2026-07-15 11:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_001' AND p.name_wsh = '咪咪' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '李师傅' AND m.name_wsh = '安心宠物生活馆' AND s.name_wsh = '安心健康检查' AND s.merchant_id_wsh = m.id_wsh;

-- 订单12: pending
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`)
SELECT 'ORDTEST20260815012', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-28', '2026-08-28', 1, 200.00, 'session', 1, 200.00, 200.00, 0.00, 200.00, 'pending'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_002' AND p.name_wsh = '小黑' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '王师傅' AND m.name_wsh = '萌宠驿站' AND s.name_wsh = '萌宠SPA护理' AND s.merchant_id_wsh = m.id_wsh;

-- 订单13: completed
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`, `completed_at_wsh`)
SELECT 'ORDTEST20260815013', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-07-10', '2026-07-10', 1, 50.00, 'session', 1, 50.00, 50.00, 0.00, 50.00, 'completed', '2026-07-10 08:00:00', '2026-07-10 09:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_003' AND p.name_wsh = '雪球' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '赵师傅' AND m.name_wsh = '爱心动物之家' AND s.name_wsh = '爱心标准遛弯' AND s.merchant_id_wsh = m.id_wsh;

-- 订单14: paid
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`)
SELECT 'ORDTEST20260815014', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-08-24', '2026-08-24', 1, 160.00, 'session', 1, 160.00, 160.00, 0.00, 160.00, 'paid'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_004' AND p.name_wsh = '橘子' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '陈师傅' AND m.name_wsh = '宠乐时光寄养中心' AND s.name_wsh = '宠乐全效美容' AND s.merchant_id_wsh = m.id_wsh;

-- 订单15: completed
INSERT INTO `pet_order_wsh` (`order_no_wsh`, `owner_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `merchant_id_wsh`, `service_id_wsh`, `start_date_wsh`, `end_date_wsh`, `days_wsh`, `price_per_day_wsh`, `billing_unit_wsh`, `quantity_wsh`, `unit_price_wsh`, `total_amount_wsh`, `discount_wsh`, `final_amount_wsh`, `status_wsh`, `started_at_wsh`, `completed_at_wsh`)
SELECT 'ORDTEST20260815015', u.id_wsh, p.id_wsh, k.id_wsh, m.id_wsh, s.id_wsh, '2026-07-05', '2026-07-05', 1, 100.00, 'session', 1, 100.00, 100.00, 0.00, 100.00, 'completed', '2026-07-05 14:00:00', '2026-07-05 15:00:00'
FROM `user_wsh` u, `pet_wsh` p, `keeper_wsh` k, `merchant_wsh` m, `pet_service_wsh` s
WHERE u.username_wsh = 'adopter_test_005' AND p.name_wsh = '花花' AND p.owner_id_wsh = u.id_wsh
  AND k.name_wsh = '张师傅' AND m.name_wsh = '优品宠物服务中心' AND s.name_wsh = '优品疫苗接种' AND s.merchant_id_wsh = m.id_wsh;

-- ===================================================================
-- 11. 支付记录 (10条, 对应已支付/已完成订单)
-- ===================================================================

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000001', final_amount_wsh, 'wechat', 'success', '2026-08-21 10:30:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815002';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000002', final_amount_wsh, 'alipay', 'success', '2026-08-14 08:00:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815003';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000003', final_amount_wsh, 'wechat', 'success', '2026-08-01 08:30:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815004';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000004', final_amount_wsh, 'balance', 'success', '2026-07-28 09:00:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815005';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000005', final_amount_wsh, 'wechat', 'success', '2026-08-22 11:00:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815007';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000006', final_amount_wsh, 'alipay', 'success', '2026-07-20 10:00:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815008';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000007', final_amount_wsh, 'wechat', 'success', '2026-08-23 09:00:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815009';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000008', final_amount_wsh, 'balance', 'success', '2026-08-14 08:30:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815010';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000009', final_amount_wsh, 'wechat', 'success', '2026-07-15 10:00:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815011';

INSERT INTO `payment_wsh` (`order_id_wsh`, `order_no_wsh`, `pay_no_wsh`, `amount_wsh`, `method_wsh`, `status_wsh`, `paid_at_wsh`)
SELECT id_wsh, order_no_wsh, 'PAYTEST000010', final_amount_wsh, 'wechat', 'success', '2026-08-24 10:00:00' FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815014';

-- ===================================================================
-- 12. 评价数据 (8条, 对应已完成订单)
-- ===================================================================

INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`, `images_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', 5, '张师傅非常专业，对闪电照顾得很好，每天发照片汇报，非常满意！', '/minio/pet-service/ratings/rating_001.jpg'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815004';

INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.merchant_id_wsh, 'merchant', 5, '爱心动物之家环境很好，设施齐全，服务专业，强烈推荐！'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815004';

-- 评价3: 订单005 (宠乐时光寄养中心 - 麻辣) - 看护人评价
INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`, `images_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', 5, '陈师傅非常细心，对麻辣照顾得很周到，每天都会发送视频和照片，非常放心！', '/minio/pet-service/ratings/rating_002.jpg'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815005';

-- 评价4: 订单005 - 商家评价
INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.merchant_id_wsh, 'merchant', 4, '宠乐时光寄养中心环境不错，设施完善，下次还会再来'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815005';

-- 评价5: 订单008 (爱心动物之家 - 甜甜) - 看护人评价
INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', 5, '赵师傅手艺很好，甜甜剪完毛造型特别可爱，服务态度也非常棒！'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815008';

-- 评价6: 订单011 (安心宠物生活馆 - 咪咪) - 看护人评价
INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`, `images_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', 5, '李师傅对咪咪的健康检查非常仔细，给出了专业的养护建议，非常感谢！', '/minio/pet-service/ratings/rating_003.jpg'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815011';

-- 评价7: 订单013 (爱心动物之家 - 雪球) - 看护人评价
INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', 4, '遛弯服务很贴心，雪球每次回来都很开心，赵师傅很专业'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815013';

-- 评价8: 订单015 (优品宠物服务中心 - 花花) - 商家评价
INSERT INTO `rating_wsh` (`order_id_wsh`, `user_id_wsh`, `target_id_wsh`, `target_type_wsh`, `score_wsh`, `content_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, o.merchant_id_wsh, 'merchant', 5, '优品宠物服务中心的疫苗接种服务很专业，医生耐心解答了所有问题，还送了疫苗记录本'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815015';

-- ===================================================================
-- 13. 钱包数据 (为领养员创建钱包)
-- ===================================================================

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 5000.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_001';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 3000.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_002';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 8000.00, 500.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_003';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 2000.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_004';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 6000.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 1500.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_006';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 4000.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_007';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 3500.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_008';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 7000.00, 0.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_009';

INSERT INTO `wallet_wsh` (`user_id_wsh`, `balance_wsh`, `frozen_amount_wsh`, `version_wsh`)
SELECT u.id_wsh, 9000.00, 1000.00, 0 FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_010';

-- ===================================================================
-- 14. 资质数据 (商家营业执照 + 看护人资格证书)
-- ===================================================================

-- 商家营业执照 (5条, 已审核通过)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'merchant', m.id_wsh, m.user_id_wsh, 'business_license', '安心宠物生活馆营业执照', '/minio/pet-service/qualification/merchant_001_license.pdf', '统一社会信用代码：91310115MA1K1234AB，经营范围：宠物寄养、美容、医疗', 'approved', 'public'
FROM `merchant_wsh` m WHERE m.name_wsh = '安心宠物生活馆';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'merchant', m.id_wsh, m.user_id_wsh, 'business_license', '萌宠驿站营业执照', '/minio/pet-service/qualification/merchant_002_license.pdf', '统一社会信用代码：91110105MA2K5678CD，经营范围：宠物寄养、训练', 'approved', 'public'
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠驿站';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'merchant', m.id_wsh, m.user_id_wsh, 'business_license', '爱心动物之家营业执照', '/minio/pet-service/qualification/merchant_003_license.pdf', '统一社会信用代码：91440106MA3K9012EF，经营范围：宠物寄养、遛宠服务', 'approved', 'public'
FROM `merchant_wsh` m WHERE m.name_wsh = '爱心动物之家';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'merchant', m.id_wsh, m.user_id_wsh, 'business_license', '宠乐时光寄养中心营业执照', '/minio/pet-service/qualification/merchant_004_license.pdf', '统一社会信用代码：91510107MA4K3456GH，经营范围：宠物寄养、美容、训练', 'approved', 'public'
FROM `merchant_wsh` m WHERE m.name_wsh = '宠乐时光寄养中心';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'merchant', m.id_wsh, m.user_id_wsh, 'business_license', '优品宠物服务中心营业执照', '/minio/pet-service/qualification/merchant_005_license.pdf', '统一社会信用代码：91440105MA5K7890IJ，经营范围：宠物寄养、美容、医疗', 'approved', 'public'
FROM `merchant_wsh` m WHERE m.name_wsh = '优品宠物服务中心';

-- 看护人资格证书 (5条, 已审核通过)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物护理师资格证（高级）', '/minio/pet-service/qualification/keeper_001_cert.pdf', '中国宠物行业协会颁发，高级宠物护理师，证书编号：PCA2024001', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.name_wsh = '张师傅';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物美容师资格证（中级）', '/minio/pet-service/qualification/keeper_002_cert.pdf', '中国宠物行业协会颁发，中级宠物美容师，证书编号：PGS2024002', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.name_wsh = '李师傅';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '专业训犬师资格证', '/minio/pet-service/qualification/keeper_003_cert.pdf', '中国工作犬管理协会颁发，专业训犬师，证书编号：CPDT2024003', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.name_wsh = '王师傅';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物护理师资格证（初级）', '/minio/pet-service/qualification/keeper_004_cert.pdf', '中国宠物行业协会颁发，初级宠物护理师，证书编号：PCA2024004', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.name_wsh = '赵师傅';

INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物医疗辅助护理证', '/minio/pet-service/qualification/keeper_005_cert.pdf', '中国兽医协会颁发，宠物医疗辅助护理人员，证书编号：CVMA2024005', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.name_wsh = '陈师傅';

-- ===================================================================
-- 测试数据初始化完成
-- 账户密码统一为: 123456
-- 商家账号: merchant_test_001 ~ merchant_test_005
-- 领养员账号: adopter_test_001 ~ adopter_test_015
-- 看护人账号: keeper_test_001 ~ keeper_test_015
-- ===================================================================