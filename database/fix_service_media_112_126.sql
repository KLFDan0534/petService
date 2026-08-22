-- ============================================================
-- fix_service_media_112_126.sql
-- 服务 112-126 在 pet_service_wsh.images_wsh 有图片 URL,但缺少
-- pet_service_media_wsh / file_record_wsh 关联行,导致公共服务列表
-- API(只读可信图册媒体行)不下发图片,前端显示无图。
-- 本脚本为这些服务补齐 file_record + media 行(幂等,可重复执行)。
-- ============================================================

-- ---------- file_record_wsh ----------
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_001.jpg', 'services/service_001.jpg', NULL, NULL, 'product', 202, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_001.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_002.jpg', 'services/service_002.jpg', NULL, NULL, 'product', 202, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_002.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_003.jpg', 'services/service_003.jpg', NULL, NULL, 'product', 202, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_003.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_004.jpg', 'services/service_004.jpg', NULL, NULL, 'product', 203, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_004.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_005.jpg', 'services/service_005.jpg', NULL, NULL, 'product', 203, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_005.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_006.jpg', 'services/service_006.jpg', NULL, NULL, 'product', 203, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_006.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_007.jpg', 'services/service_007.jpg', NULL, NULL, 'product', 204, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_007.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_008.jpg', 'services/service_008.jpg', NULL, NULL, 'product', 204, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_008.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_009.jpg', 'services/service_009.jpg', NULL, NULL, 'product', 204, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_009.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_010.jpg', 'services/service_010.jpg', NULL, NULL, 'product', 205, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_010.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_011.jpg', 'services/service_011.jpg', NULL, NULL, 'product', 205, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_011.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_012.jpg', 'services/service_012.jpg', NULL, NULL, 'product', 205, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_012.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_013.jpg', 'services/service_013.jpg', NULL, NULL, 'product', 206, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_013.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_014.jpg', 'services/service_014.jpg', NULL, NULL, 'product', 206, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_014.jpg');
INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_015.jpg', 'services/service_015.jpg', NULL, NULL, 'product', 206, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_015.jpg');

-- ---------- pet_service_media_wsh ----------
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 112, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_001.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 112);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 113, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_002.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 113);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 114, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_003.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 114);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 115, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_004.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 115);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 116, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_005.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 116);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 117, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_006.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 117);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 118, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_007.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 118);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 119, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_008.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 119);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 120, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_009.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 120);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 121, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_010.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 121);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 122, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_011.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 122);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 123, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_012.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 123);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 124, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_013.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 124);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 125, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_014.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 125);
INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 126, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_015.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 126);
