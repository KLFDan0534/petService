-- ===================================================================
-- test-data-fix-keeper-qualifications.sql
-- 修复：为 test-data-fix-keepers.sql 新增的看护人补充"已审核通过"的资质
--
-- 背景：
--   业务规则要求"可接单看护人 = 资质通过(approved) + 在职(ACTIVE/OFFLINE/BUSY)"。
--   test-data-fix-keepers.sql 创建了 keeper_test_006 ~ 015（周/吴/郑/孙/钱/冯/沈/褚/卫/蒋师傅）
--   并置为在职，但遗漏了 qualification_wsh 资质记录，导致下单弹窗
--   "暂无可接单看护人"、"近 31 天暂无可预约日期"。
--
-- 覆盖商家：202 安心宠物生活馆 / 203 萌宠驿站 / 204 爱心动物之家
--          205 宠乐时光寄养中心 / 206 优品宠物服务中心
--
-- 幂等性：NOT EXISTS 防重复，可重复执行。
-- 操作：仅 INSERT，不包含 DELETE/TRUNCATE/DROP/ALTER。
-- ===================================================================

USE `pet_service`;

SET NAMES utf8mb4;

-- 周师傅 → 安心宠物生活馆 (宠物护理)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物护理师资格证（高级）', '/minio/pet-service/qualification/keeper_test_006_cert.pdf', '中国宠物行业协会颁发，高级宠物护理师，证书编号：KPT2026006', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_006')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 吴师傅 → 萌宠驿站 (训犬)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '专业训犬师资格证', '/minio/pet-service/qualification/keeper_test_007_cert.pdf', '中国工作犬管理协会颁发，专业训犬师，证书编号：KPT2026007', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_007')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 郑师傅 → 萌宠驿站 (美容)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物美容师资格证（中级）', '/minio/pet-service/qualification/keeper_test_008_cert.pdf', '中国宠物行业协会颁发，中级宠物美容师，证书编号：KPT2026008', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_008')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 孙师傅 → 爱心动物之家 (遛宠)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '专业遛宠师资格证', '/minio/pet-service/qualification/keeper_test_009_cert.pdf', '中国宠物行业协会颁发，专业遛宠师，证书编号：KPT2026009', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_009')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 钱师傅 → 爱心动物之家 (医疗辅助)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物医疗辅助护理证', '/minio/pet-service/qualification/keeper_test_010_cert.pdf', '中国兽医协会颁发，宠物医疗辅助护理人员，证书编号：KPT2026010', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_010')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 冯师傅 → 宠乐时光寄养中心 (美容)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物美容师资格证（高级）', '/minio/pet-service/qualification/keeper_test_011_cert.pdf', '中国宠物行业协会颁发，高级宠物美容师，证书编号：KPT2026011', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_011')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 沈师傅 → 宠乐时光寄养中心 (训练)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物训练师资格证', '/minio/pet-service/qualification/keeper_test_012_cert.pdf', '中国工作犬管理协会颁发，宠物训练师，证书编号：KPT2026012', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_012')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 褚师傅 → 优品宠物服务中心 (医疗)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物医疗护理资格证（高级）', '/minio/pet-service/qualification/keeper_test_013_cert.pdf', '中国兽医协会颁发，高级宠物医疗护理师，证书编号：KPT2026013', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_013')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 卫师傅 → 优品宠物服务中心 (美容)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物美容护理资格证（中级）', '/minio/pet-service/qualification/keeper_test_014_cert.pdf', '中国宠物行业协会颁发，中级宠物美容护理师，证书编号：KPT2026014', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_014')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

-- 蒋师傅 → 优品宠物服务中心 (寄养)
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物寄养护理资格证（中级）', '/minio/pet-service/qualification/keeper_test_015_cert.pdf', '中国宠物行业协会颁发，中级宠物寄养护理师，证书编号：KPT2026015', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_015')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);

SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'pet_care_cert', '宠物医疗辅助护理证', '/minio/pet-service/qualification/keeper_test_010_cert.pdf', '中国兽医协会颁发，宠物医疗辅助护理人员，证书编号：KPT2026010', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.user_id_wsh = (SELECT id_wsh FROM `user_wsh` u WHERE u.username_wsh = 'keeper_test_010')
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.deleted_wsh = 0);
