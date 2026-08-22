-- ===================================================================
-- test-data-fix-password-all.sql
-- 统一所有用户登录密码为 123456
-- 覆盖: 老种子账号(admin/owner/merchant1/keeper1) + 全部测试用户 + 后续新增用户
-- 操作: 仅 UPDATE password_wsh, 无 DELETE/TRUNCATE/DROP
-- 说明: password_wsh 为唯一登录密码字段; payment_password_wsh(支付密码) 保持不动
-- ===================================================================

USE `pet_service`;

-- 正确的 BCrypt 哈希 (明文: 123456, 由 Spring Security BCryptPasswordEncoder 生成)
-- $2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS

-- 将所有用户统一为密码 123456
UPDATE `user_wsh`
SET `password_wsh` = '$2a$10$B/s0Hq6i3qhgf4OeT/QGzuwj1s/spQY35zuE8KkwLB0RPd9uorpeS';