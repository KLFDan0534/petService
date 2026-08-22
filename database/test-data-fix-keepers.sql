-- ===================================================================
-- test-data-fix-keepers.sql
-- 修复商家营业时间 + 看护人可见性 + 新增看护人
--
-- 1. 更新营业时间为 08:00-23:59
-- 2. 更新店铺状态为 OPEN(1)
-- 3. 更新现有看护人状态为 ACTIVE(1)
-- 4. 新增10个看护人用户 (keeper_test_006 ~ keeper_test_015)
-- 5. 新增看护人角色关联
-- 6. 新增看护人记录 (每个商家至少3个看护人)
--
-- 操作: UPDATE + INSERT, 不包含 DELETE/TRUNCATE/DROP/ALTER
-- 密码: 123456 (BCrypt: $2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS)
-- ===================================================================

USE `pet_service`;

-- ===================================================================
-- 1. 更新营业时间: 08:00 - 23:59 (所有测试商家, 7天)
-- ===================================================================
UPDATE `business_hours_wsh`
SET `open_time_wsh` = '08:00:00', `close_time_wsh` = '23:59:00', `is_closed_wsh` = 0
WHERE `merchant_id_wsh` IN (
    SELECT id_wsh FROM `merchant_wsh` WHERE name_wsh IN (
        '安心宠物生活馆', '萌宠驿站', '爱心动物之家', '宠乐时光寄养中心', '优品宠物服务中心'
    )
);

-- ===================================================================
-- 2. 更新店铺状态为 OPEN(1), 确保看护人可见
-- ===================================================================
UPDATE `merchant_wsh`
SET `store_status_wsh` = 1
WHERE `name_wsh` IN (
    '安心宠物生活馆', '萌宠驿站', '爱心动物之家', '宠乐时光寄养中心', '优品宠物服务中心'
);

-- ===================================================================
-- 3. 更新现有看护人状态为 ACTIVE(1), offline_source=0(系统)
-- ===================================================================
UPDATE `keeper_wsh`
SET `status_wsh` = 1, `offline_source_wsh` = 0
WHERE `user_id_wsh` IN (
    SELECT id_wsh FROM `user_wsh` WHERE username_wsh IN (
        'keeper_test_001', 'keeper_test_002', 'keeper_test_003',
        'keeper_test_004', 'keeper_test_005'
    )
);

-- ===================================================================
-- 4. 新增看护人用户 (10个: keeper_test_006 ~ keeper_test_015)
-- ===================================================================
INSERT INTO `user_wsh` (`username_wsh`, `password_wsh`, `nickname_wsh`, `phone_wsh`, `email_wsh`, `address_wsh`, `latitude_wsh`, `longitude_wsh`, `gender_wsh`, `status_wsh`, `real_name_status_wsh`) VALUES
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
-- 5. 新增看护人角色关联 (role_id=3 即 KEEPER)
-- ===================================================================
INSERT INTO `user_role_wsh` (`user_id_wsh`, `role_id_wsh`)
SELECT u.id_wsh, 3 FROM `user_wsh` u WHERE u.username_wsh IN (
    'keeper_test_006', 'keeper_test_007', 'keeper_test_008', 'keeper_test_009', 'keeper_test_010',
    'keeper_test_011', 'keeper_test_012', 'keeper_test_013', 'keeper_test_014', 'keeper_test_015'
);

-- ===================================================================
-- 6. 新增看护人记录 (每个商家至少3个看护人)
--    安心宠物生活馆: 张师傅+李师傅+周师傅 = 3
--    萌宠驿站: 王师傅+吴师傅+郑师傅 = 3
--    爱心动物之家: 赵师傅+孙师傅+钱师傅 = 3
--    宠乐时光寄养中心: 陈师傅+冯师傅+沈师傅 = 3
--    优品宠物服务中心: 褚师傅+卫师傅+蒋师傅 = 3
-- ===================================================================

-- 周师傅 → 安心宠物生活馆 (美容专长, 8年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '周师傅', '13900000016', '/minio/pet-service/keeper/avatar_016.jpg', 8, 4.85, 97.00, 0.00, 220.00, 5, 0, '高级宠物美容师，擅长各类犬种造型设计和SPA护理，从业8年，手法温柔细致', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '安心宠物生活馆' AND u.username_wsh = 'keeper_test_006';

-- 吴师傅 → 萌宠驿站 (训练专长, 6年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '吴师傅', '13900000017', '/minio/pet-service/keeper/avatar_017.jpg', 6, 4.80, 96.00, 0.00, 180.00, 4, 0, '专业训犬师，擅长犬类行为矫正和服从训练，从业6年，耐心负责', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠驿站' AND u.username_wsh = 'keeper_test_007';

-- 郑师傅 → 萌宠驿站 (寄养专长, 4年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '郑师傅', '13900000018', '/minio/pet-service/keeper/avatar_018.jpg', 4, 4.75, 95.00, 0.00, 150.00, 4, 0, '宠物寄养护理师，擅长日常护理和喂养管理，从业4年，细心周到', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '萌宠驿站' AND u.username_wsh = 'keeper_test_008';

-- 孙师傅 → 爱心动物之家 (遛宠专长, 7年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '孙师傅', '13900000019', '/minio/pet-service/keeper/avatar_019.jpg', 7, 4.88, 98.00, 0.00, 160.00, 6, 0, '专业遛宠师，熟悉各区域遛宠路线，擅长大型犬运动管理，从业7年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱心动物之家' AND u.username_wsh = 'keeper_test_009';

-- 钱师傅 → 爱心动物之家 (医疗辅助, 5年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '钱师傅', '13900000020', '/minio/pet-service/keeper/avatar_020.jpg', 5, 4.82, 97.00, 0.00, 200.00, 4, 0, '宠物医疗辅助护理师，擅长术后护理和用药管理，从业5年，专业严谨', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '爱心动物之家' AND u.username_wsh = 'keeper_test_010';

-- 冯师傅 → 宠乐时光寄养中心 (美容专长, 9年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '冯师傅', '13900000021', '/minio/pet-service/keeper/avatar_021.jpg', 9, 4.92, 99.00, 0.00, 250.00, 5, 0, '资深宠物美容师，擅长创意造型和毛色护理，从业9年，业界知名', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠乐时光寄养中心' AND u.username_wsh = 'keeper_test_011';

-- 沈师傅 → 宠乐时光寄养中心 (训练专长, 3年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '沈师傅', '13900000022', '/minio/pet-service/keeper/avatar_022.jpg', 3, 4.70, 94.00, 0.00, 130.00, 4, 0, '青年训犬师，擅长幼犬社会化训练和基础指令教学，从业3年，活力十足', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '宠乐时光寄养中心' AND u.username_wsh = 'keeper_test_012';

-- 褚师傅 → 优品宠物服务中心 (医疗专长, 10年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '褚师傅', '13900000023', '/minio/pet-service/keeper/avatar_023.jpg', 10, 4.95, 99.00, 0.00, 300.00, 4, 0, '高级宠物医疗护理专家，擅长急重症护理和康复治疗，从业10年，经验丰富', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '优品宠物服务中心' AND u.username_wsh = 'keeper_test_013';

-- 卫师傅 → 优品宠物服务中心 (美容专长, 6年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '卫师傅', '13900000024', '/minio/pet-service/keeper/avatar_024.jpg', 6, 4.85, 97.00, 0.00, 190.00, 5, 0, '宠物美容师，擅长猫咪美容和指甲护理，从业6年，温柔耐心', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '优品宠物服务中心' AND u.username_wsh = 'keeper_test_014';

-- 蒋师傅 → 优品宠物服务中心 (寄养专长, 4年)
INSERT INTO `keeper_wsh` (`merchant_id_wsh`, `user_id_wsh`, `name_wsh`, `phone_wsh`, `avatar_wsh`, `experience_years_wsh`, `rating_wsh`, `completion_rate_wsh`, `complaint_rate_wsh`, `price_per_day_wsh`, `max_pets_wsh`, `current_pets_wsh`, `bio_wsh`, `status_wsh`, `offline_source_wsh`)
SELECT m.id_wsh, u.id_wsh, '蒋师傅', '13900000025', '/minio/pet-service/keeper/avatar_025.jpg', 4, 4.78, 96.00, 0.00, 160.00, 5, 0, '宠物寄养护理师，擅长老年犬猫日常照料和特殊饮食管理，从业4年', 1, 0
FROM `merchant_wsh` m, `user_wsh` u WHERE m.name_wsh = '优品宠物服务中心' AND u.username_wsh = 'keeper_test_015';

-- ===================================================================
-- 7. 为新增看护人补充"已审核通过"的资质（必配！）
--    业务规则：可接单看护人 = 资质通过(approved) + 在职。
--    缺失本段会导致下单弹窗提示"暂无可接单看护人/近31天暂无可预约日期"。
--    与 database/test-data-fix-keeper-qualifications.sql 保持一致（幂等，可重复执行）。
-- ===================================================================

-- 周师傅 → 安心宠物生活馆
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物护理师资格证（高级）', '/minio/pet-service/qualification/keeper_test_006_cert.pdf', '中国宠物行业协会颁发，高级宠物护理师，证书编号：KPT2026006', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_006') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 吴师傅 → 萌宠驿站
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '专业训犬师资格证', '/minio/pet-service/qualification/keeper_test_007_cert.pdf', '中国工作犬管理协会颁发，专业训犬师，证书编号：KPT2026007', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_007') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 郑师傅 → 萌宠驿站
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物美容师资格证（中级）', '/minio/pet-service/qualification/keeper_test_008_cert.pdf', '中国宠物行业协会颁发，中级宠物美容师，证书编号：KPT2026008', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_008') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 冯师傅 → 宠乐时光寄养中心
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物美容师资格证（高级）', '/minio/pet-service/qualification/keeper_test_011_cert.pdf', '中国宠物行业协会颁发，高级宠物美容师，证书编号：KPT2026011', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_011') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 沈师傅 → 宠乐时光寄养中心
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物训练师资格证', '/minio/pet-service/qualification/keeper_test_012_cert.pdf', '中国工作犬管理协会颁发，宠物训练师，证书编号：KPT2026012', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_012') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 褚师傅 → 优品宠物服务中心
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物医疗护理资格证（高级）', '/minio/pet-service/qualification/keeper_test_013_cert.pdf', '中国兽医协会颁发，高级宠物医疗护理师，证书编号：KPT2026013', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_013') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 卫师傅 → 优品宠物服务中心
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物美容护理资格证（中级）', '/minio/pet-service/qualification/keeper_test_014_cert.pdf', '中国宠物行业协会颁发，中级宠物美容护理师，证书编号：KPT2026014', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_014') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 蒋师傅 → 优品宠物服务中心
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物寄养护理资格证（中级）', '/minio/pet-service/qualification/keeper_test_015_cert.pdf', '中国宠物行业协会颁发，中级宠物寄养护理师，证书编号：KPT2026015', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_015') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 孙师傅 → 爱心动物之家
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '专业遛宠师资格证', '/minio/pet-service/qualification/keeper_test_009_cert.pdf', '中国宠物行业协会颁发，专业遛宠师，证书编号：KPT2026009', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_009') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

-- 钱师傅 → 爱心动物之家
INSERT INTO `qualification_wsh` (`owner_type_wsh`,`owner_id_wsh`,`user_id_wsh`,`qual_type_wsh`,`title_wsh`,`file_url_wsh`,`summary_wsh`,`status_wsh`,`visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物医疗辅助护理证', '/minio/pet-service/qualification/keeper_test_010_cert.pdf', '中国兽医协会颁发，宠物医疗辅助护理人员，证书编号：KPT2026010', 'approved', 'public'
FROM `keeper_wsh` k WHERE k.user_id_wsh=(SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh='keeper_test_010') AND k.deleted_wsh=0 AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh='keeper' AND q.owner_id_wsh=k.id_wsh AND q.deleted_wsh=0);

