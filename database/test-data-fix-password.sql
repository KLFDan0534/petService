-- ===================================================================
-- test-data-fix-password.sql
-- 修复测试账户密码哈希
-- 原因: test-data-init.sql 首次执行时使用了错误的 BCrypt 哈希
-- 范围: 仅更新本次新增的测试用户 (_test_ 后缀), 不影响原有用户
-- 操作: 仅包含 UPDATE 语句, 不包含 DELETE/TRUNCATE/DROP
-- ===================================================================

USE `pet_service`;

-- 正确的 BCrypt 哈希 (明文: 123456, 由 Spring Security BCryptPasswordEncoder 生成)
-- $2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS

-- 修复所有测试用户的密码哈希 (25个账户: 5商家 + 15领养员 + 5看护人)
UPDATE `user_wsh`
SET `password_wsh` = '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS'
WHERE `username_wsh` IN (
    'merchant_test_001', 'merchant_test_002', 'merchant_test_003', 'merchant_test_004', 'merchant_test_005',
    'adopter_test_001', 'adopter_test_002', 'adopter_test_003', 'adopter_test_004', 'adopter_test_005',
    'adopter_test_006', 'adopter_test_007', 'adopter_test_008', 'adopter_test_009', 'adopter_test_010',
    'adopter_test_011', 'adopter_test_012', 'adopter_test_013', 'adopter_test_014', 'adopter_test_015',
    'keeper_test_001', 'keeper_test_002', 'keeper_test_003', 'keeper_test_004', 'keeper_test_005'
);
