-- ============================================================
-- fix_service_media_110_111.sql
-- E2E 测试服务 110/111 缺少 pet_service_media_wsh / file_record_wsh
-- 关联行,公共服务 API 不下发图片。本脚本补齐(幂等)。
-- ============================================================

INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_016.jpg', 'services/service_016.jpg', NULL, NULL, 'product', 104, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_016.jpg');

INSERT INTO file_record_wsh (original_name_wsh, object_name_wsh, size_wsh, content_type_wsh, purpose_wsh, merchant_id_wsh, user_id_wsh, created_at_wsh, updated_at_wsh)
SELECT 'service_017.jpg', 'services/service_017.jpg', NULL, NULL, 'product', 201, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM file_record_wsh WHERE object_name_wsh = 'services/service_017.jpg');

INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 110, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_016.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 110);

INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh, created_at_wsh, updated_at_wsh)
SELECT 111, fr.id_wsh, 1, 1, NOW(), NOW() FROM file_record_wsh fr WHERE fr.object_name_wsh = 'services/service_017.jpg'
AND NOT EXISTS (SELECT 1 FROM pet_service_media_wsh m WHERE m.service_id_wsh = 111);
