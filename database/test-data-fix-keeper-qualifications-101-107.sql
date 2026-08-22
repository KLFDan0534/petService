-- ===================================================================
-- test-data-fix-keeper-qualifications-101-107.sql
-- 修复：为 keepers 101~107（北京/广州/成都测试商家）补充"已审核通过"的资质
--
-- 背景：
--   业务规则要求"可接单看护人 = 资质通过(approved) + 在职(ACTIVE/OFFLINE/BUSY)"。
--   test-data-fix-keeper-qualifications.sql 已为 keeper_test_006~015（商家 202~206）
--   补充资质，但遗漏了 keepers 101~107（商家 101 Beijing Pet Paradise /
--   102 Guangzhou Animal Care / 103 Chengdu Pet House）。
--   这导致这些商家下的服务（如 109 Health Check / 108 Daycare / 107 Cozy Boarding）
--   在下单弹窗中"暂无可接单看护人"，送达日期/送达时间永久置灰、无法操作。
--
-- 覆盖商家：
--   101 Beijing Pet Paradise → keepers 101 / 102 / 107
--   102 Guangzhou Animal Care → keepers 103 / 104
--   103 Chengdu Pet House    → keepers 105 / 106
--
-- 幂等性：NOT EXISTS 防重复，可重复执行。
-- 操作：仅 INSERT，不包含 DELETE/TRUNCATE/DROP/ALTER。
-- ===================================================================

USE `pet_service`;

SET NAMES utf8mb4;

-- Zhao Hong（兽医背景）→ 北京宠物天堂
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'keeper_certificate', '宠物医疗护理资格证（高级）', '/minio/pet-service/qualification/keeper_101_cert.pdf', '中国兽医协会颁发，高级宠物医疗护理师', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.id_wsh = 101
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.status_wsh = 'approved' AND q.deleted_wsh = 0);

-- Wang Wei（训犬专家）→ 北京宠物天堂
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'keeper_certificate', '专业训犬师资格证', '/minio/pet-service/qualification/keeper_102_cert.pdf', '中国工作犬管理协会颁发，专业训犬师', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.id_wsh = 102
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.status_wsh = 'approved' AND q.deleted_wsh = 0);

-- Sun Li（猫咪护理）→ 广州动物关爱
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'keeper_certificate', '猫咪护理师资格证（高级）', '/minio/pet-service/qualification/keeper_103_cert.pdf', '中国宠物行业协会颁发，高级猫咪护理师', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.id_wsh = 103
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.status_wsh = 'approved' AND q.deleted_wsh = 0);

-- Zhou Jie（小宠护理）→ 广州动物关爱
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'keeper_certificate', '小宠护理师资格证', '/minio/pet-service/qualification/keeper_104_cert.pdf', '中国宠物行业协会颁发，小宠护理师', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.id_wsh = 104
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.status_wsh = 'approved' AND q.deleted_wsh = 0);

-- Liu Mei（宠物营养）→ 成都宠物之家
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'keeper_certificate', '宠物营养护理师资格证（高级）', '/minio/pet-service/qualification/keeper_105_cert.pdf', '中国宠物营养协会颁发，高级宠物营养护理师', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.id_wsh = 105
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.status_wsh = 'approved' AND q.deleted_wsh = 0);

-- Zhang Qiang（资深寄养）→ 成都宠物之家
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'keeper_certificate', '宠物寄养护理师资格证（高级）', '/minio/pet-service/qualification/keeper_106_cert.pdf', '中国宠物行业协会颁发，高级宠物寄养护理师', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.id_wsh = 106
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.status_wsh = 'approved' AND q.deleted_wsh = 0);

-- test北京1（领养员）→ 北京宠物天堂
INSERT INTO `qualification_wsh` (`owner_type_wsh`, `owner_id_wsh`, `user_id_wsh`, `qual_type_wsh`, `title_wsh`, `file_url_wsh`, `summary_wsh`, `status_wsh`, `visibility_wsh`)
SELECT 'keeper', k.id_wsh, k.user_id_wsh, 'keeper_certificate', '宠物领养护理员资格证', '/minio/pet-service/qualification/keeper_107_cert.pdf', '中国宠物行业协会颁发，宠物领养护理员', 'approved', 'public'
FROM `keeper_wsh` k
WHERE k.id_wsh = 107
  AND k.deleted_wsh = 0
  AND NOT EXISTS (SELECT 1 FROM `qualification_wsh` q WHERE q.owner_type_wsh = 'keeper' AND q.owner_id_wsh = k.id_wsh AND q.status_wsh = 'approved' AND q.deleted_wsh = 0);
