-- ===================================================================
-- test-data-transactions-ops.sql
-- Pet Service 业务/运维补充测试数据
-- 日期: 2026-08-22
-- 说明: 仅包含 INSERT 语句(含 INSERT INTO ... SELECT 子查询引用)，不含 DELETE/TRUNCATE/DROP/UPDATE/ALTER
-- 依赖: 需在 test-data-init.sql 与 test-data-enhanced.sql 之后执行
-- 密码: 沿用现有用户，统一 123456 (BCrypt)
-- 图片: 采用 Unsplash 真实宠物图片 URL，与宠物/服务/领养场景匹配
-- 补全表: care_record / address / complaint / ticket / ticket_message /
--         notification / wallet_transaction / withdrawal / favorite /
--         notice / notice_read / content_review /
--         refund / tip / chat_message / file_record / pet_service_media
-- ===================================================================

USE `pet_service`;

-- 常用 Unsplash 宠物图片(狗)
--   DOG1=https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=600  (金毛)
--   DOG2=https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=600  (犬)
--   DOG3=https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=600  (幼犬)
--   DOG4=https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=600  (犬)
--   DOG5=https://images.unsplash.com/photo-1558788353-f76d92427f16?w=600  (中华田园犬)
--   DOG6=https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=600 (德牧)
--   CAT1=https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=600  (橘猫)
--   CAT2=https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=600  (幼猫)
--   CAT3=https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?w=600  (白猫)

-- ===================================================================
-- 1. care_record_wsh — 照护记录 (关联已完成/进行中订单)
-- ===================================================================
INSERT INTO `care_record_wsh` (`order_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `type_wsh`, `content_wsh`, `images_wsh`, `record_time_wsh`)
SELECT o.id_wsh, o.pet_id_wsh, o.keeper_id_wsh, 'feed', '早上已喂食皇家狗粮200g，饮水充足，精神状态良好', 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=600', '2026-07-28 08:30:00'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815005';

INSERT INTO `care_record_wsh` (`order_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `type_wsh`, `content_wsh`, `images_wsh`, `record_time_wsh`)
SELECT o.id_wsh, o.pet_id_wsh, o.keeper_id_wsh, 'activity', '傍晚带宠物在园区散步40分钟，做了抛球游戏，非常活跃', 'https://images.unsplash.com/photo-1558788353-f76d92427f16?w=600', '2026-07-29 18:00:00'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815005';

INSERT INTO `care_record_wsh` (`order_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `type_wsh`, `content_wsh`, `images_wsh`, `record_time_wsh`)
SELECT o.id_wsh, o.pet_id_wsh, o.keeper_id_wsh, 'medication', '按医嘱喂服体内驱虫药半片，无不良反应', 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=600', '2026-07-20 11:00:00'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815008';

INSERT INTO `care_record_wsh` (`order_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `type_wsh`, `content_wsh`, `images_wsh`, `record_time_wsh`)
SELECT o.id_wsh, o.pet_id_wsh, o.keeper_id_wsh, 'health', '体检结果良好，体重稳定，毛发有光泽，建议保持现喂养方案', 'https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=600', '2026-07-15 10:30:00'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815011';

INSERT INTO `care_record_wsh` (`order_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `type_wsh`, `content_wsh`, `images_wsh`, `record_time_wsh`)
SELECT o.id_wsh, o.pet_id_wsh, o.keeper_id_wsh, 'feed', '下午加餐一次，食欲正常，并完成遛弯', 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=600', '2026-07-10 16:00:00'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815013';

INSERT INTO `care_record_wsh` (`order_id_wsh`, `pet_id_wsh`, `keeper_id_wsh`, `type_wsh`, `content_wsh`, `images_wsh`, `record_time_wsh`)
SELECT o.id_wsh, o.pet_id_wsh, o.keeper_id_wsh, 'activity', '进行基础坐、卧指令复习训练，配合度高', 'https://images.unsplash.com/photo-1598133894008-61f7fdb8cc3a?w=600', '2026-08-14 09:30:00'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815010';

-- ===================================================================
-- 2. address_wsh — 收货/送养地址 (领养员常用地址)
-- ===================================================================
INSERT INTO `address_wsh` (`user_id_wsh`, `label_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `detail_wsh`, `latitude_wsh`, `longitude_wsh`, `is_default_wsh`)
SELECT u.id_wsh, '家', '张三', '13900000006', '上海市闵行区莘庄镇春申路200号', '3号楼1202室', 31.1117000, 121.3825000, 1
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_001';

INSERT INTO `address_wsh` (`user_id_wsh`, `label_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `detail_wsh`, `latitude_wsh`, `longitude_wsh`, `is_default_wsh`)
SELECT u.id_wsh, '公司', '李四', '13900000007', '北京市海淀区中关村软件园二期', 'E座8层', 40.0410000, 116.2960000, 0
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_002';

INSERT INTO `address_wsh` (`user_id_wsh`, `label_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `detail_wsh`, `latitude_wsh`, `longitude_wsh`, `is_default_wsh`)
SELECT u.id_wsh, '家', '王五', '13900000008', '广州市越秀区中山五路100号', '一栋502', 23.1353000, 113.2620000, 1
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_003';

INSERT INTO `address_wsh` (`user_id_wsh`, `label_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `detail_wsh`, `latitude_wsh`, `longitude_wsh`, `is_default_wsh`)
SELECT u.id_wsh, '家', '赵六', '13900000009', '成都市锦江区春熙路IFS国际金融中心', '1栋2601', 30.6570000, 104.0810000, 1
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_004';

INSERT INTO `address_wsh` (`user_id_wsh`, `label_wsh`, `name_wsh`, `phone_wsh`, `address_wsh`, `detail_wsh`, `latitude_wsh`, `longitude_wsh`, `is_default_wsh`)
SELECT u.id_wsh, '家', '孙七', '13900000010', '深圳市福田区华强北商业街8号', 'B座2103', 22.5410000, 114.0583000, 1
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

-- ===================================================================
-- 3. complaint_wsh — 投诉 (多状态)
-- ===================================================================
INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `images_wsh`, `status_wsh`, `result_wsh`)
SELECT o.id_wsh, o.merchant_id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', '昨日喂食未按约定', '预约了定点喂食，看护人晚到1小时，宠物饿了一段时间，希望改进', NULL, 'pending', NULL
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815005';

INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `images_wsh`, `status_wsh`, `result_wsh`)
SELECT o.id_wsh, o.merchant_id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', '遛弯时间太短', '约定40分钟遛弯，实际只有20分钟就返回', 'https://images.unsplash.com/photo-1558788353-f76d92427f16?w=600', 'processing', '正在核实看护记录，将尽快处理'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815013';

INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `result_wsh`)
SELECT o.id_wsh, o.merchant_id_wsh, o.owner_id_wsh, o.keeper_id_wsh, 'keeper', '洗澡后毛未吹干', '基础美容后毛发未完全吹干，担心宠物受凉', 'resolved', '已向服务人员反馈，已补偿一次免费护理，深表歉意'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815008';

INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `result_wsh`)
SELECT o.id_wsh, o.merchant_id_wsh, o.owner_id_wsh, o.merchant_id_wsh, 'merchant', '园区环境噪音过大', '寄养期间隔壁施工噪音较大，宠物有些紧张', 'rejected', '经核实噪音为临时市政施工，已与物业协调，非商家可控'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815011';

-- ===================================================================
-- 4. ticket_wsh — 客服工单
-- ===================================================================
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `result_wsh`, `assignee_id_wsh`)
SELECT o.merchant_id_wsh, o.id_wsh, o.owner_id_wsh, '寄养期间如何补充见宠', '想了解寄养期间是否可以视频探访宠物', 'question', 'low', 'resolved', '可以，每日10:00-18:00可预约视频探访，需提前联系客服', NULL
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815005';

INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `result_wsh`, `assignee_id_wsh`)
SELECT o.merchant_id_wsh, o.id_wsh, o.owner_id_wsh, '美容产品是否过敏', '我的猫皮肤敏感，美容用的是哪款洗护产品', 'consultation', 'medium', 'processing', '正在查阅订单使用的洗护产品信息，将在1个工作日内回复', NULL
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815011';

INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `result_wsh`, `assignee_id_wsh`)
SELECT o.merchant_id_wsh, o.id_wsh, o.owner_id_wsh, '建议增加24小时监控', '建议寄养区加装摄像头实时监控，让主人更放心', 'suggestion', 'low', 'pending', NULL, NULL
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815010';

-- ticket_message_wsh — 工单回复
INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`)
SELECT t.id_wsh, t.user_id_wsh, '还想问下探访是否需要额外收费？'
FROM `ticket_wsh` t WHERE t.title_wsh = '寄养期间如何补充见宠';

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`)
SELECT t.id_wsh, (SELECT id_wsh FROM `user_wsh` WHERE username_wsh = 'admin' LIMIT 1), '每日两次免费视频探访，超时按10元/次计费'
FROM `ticket_wsh` t WHERE t.title_wsh = '寄养期间如何补充见宠';

-- ===================================================================
-- 5. notification_wsh — 通知 (多类型/已读未读)
-- ===================================================================
INSERT INTO `notification_wsh` (`user_id_wsh`, `title_wsh`, `content_wsh`, `type_wsh`, `is_read_wsh`, `related_id_wsh`)
SELECT u.id_wsh, '订单支付成功', '您的订单已支付成功，寄养员正在准备', 'payment', 1, NULL
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

INSERT INTO `notification_wsh` (`user_id_wsh`, `title_wsh`, `content_wsh`, `type_wsh`, `is_read_wsh`, `related_id_wsh`)
SELECT u.id_wsh, '工单已回复', '您提交的客服工单已有新回复，请查收', 'ticket', 0, NULL
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_008';

INSERT INTO `notification_wsh` (`user_id_wsh`, `title_wsh`, `content_wsh`, `type_wsh`, `is_read_wsh`, `related_id_wsh`)
SELECT u.id_wsh, '平台公告', '夏季高温，宠物出行请注意防暑降温', 'system', 1, NULL
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_001';

INSERT INTO `notification_wsh` (`user_id_wsh`, `title_wsh`, `content_wsh`, `type_wsh`, `is_read_wsh`)
SELECT u.id_wsh, '订单状态更新', '您的订单已开始服务，可在订单详情查看实时照护记录', 'order', 0
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_010';

-- ===================================================================
-- 6. wallet_transaction_wsh — 钱包流水 (关联钱包)
-- ===================================================================
INSERT INTO `wallet_transaction_wsh` (`wallet_id_wsh`, `user_id_wsh`, `type_wsh`, `amount_wsh`, `balance_before_wsh`, `balance_after_wsh`, `direction_wsh`, `status_wsh`, `business_type_wsh`, `business_id_wsh`, `request_id_wsh`, `description_wsh`)
SELECT w.id_wsh, u.id_wsh, 'refund', 260.00, 5000.00, 5260.00, 'in', 'success', 'refund', 'ORDTEST20260815005', 'TX_TEST_REFUND_001', '订单退款'
FROM `user_wsh` u JOIN `wallet_wsh` w ON w.user_id_wsh = u.id_wsh WHERE u.username_wsh = 'adopter_test_005';

INSERT INTO `wallet_transaction_wsh` (`wallet_id_wsh`, `user_id_wsh`, `type_wsh`, `amount_wsh`, `balance_before_wsh`, `balance_after_wsh`, `direction_wsh`, `status_wsh`, `business_type_wsh`, `business_id_wsh`, `request_id_wsh`, `description_wsh`)
SELECT w.id_wsh, u.id_wsh, 'expense', 200.00, 8000.00, 7800.00, 'out', 'success', 'payment', 'ORDTEST20260815013', 'TX_TEST_PAY_001', '订单支付扣款'
FROM `user_wsh` u JOIN `wallet_wsh` w ON w.user_id_wsh = u.id_wsh WHERE u.username_wsh = 'adopter_test_003';

INSERT INTO `wallet_transaction_wsh` (`wallet_id_wsh`, `user_id_wsh`, `type_wsh`, `amount_wsh`, `balance_before_wsh`, `balance_after_wsh`, `direction_wsh`, `status_wsh`, `business_type_wsh`, `request_id_wsh`, `description_wsh`)
SELECT w.id_wsh, u.id_wsh, 'income', 500.00, 2000.00, 2500.00, 'in', 'success', 'tip', 'TX_TEST_TIP_001', '收到打赏'
FROM `user_wsh` u JOIN `wallet_wsh` w ON w.user_id_wsh = u.id_wsh WHERE u.username_wsh = 'adopter_test_004';

INSERT INTO `wallet_transaction_wsh` (`wallet_id_wsh`, `user_id_wsh`, `type_wsh`, `amount_wsh`, `balance_before_wsh`, `balance_after_wsh`, `frozen_before_wsh`, `frozen_after_wsh`, `direction_wsh`, `status_wsh`, `business_type_wsh`, `request_id_wsh`, `description_wsh`)
SELECT w.id_wsh, u.id_wsh, 'expense', 280.00, 9000.00, 8720.00, 1000.00, 720.00, 'out-freeze', 'success', 'payment', 'TX_TEST_PAY_002', '下单占用冻结额度'
FROM `user_wsh` u JOIN `wallet_wsh` w ON w.user_id_wsh = u.id_wsh WHERE u.username_wsh = 'adopter_test_010';

-- ===================================================================
-- 7. withdrawal_wsh — 提现记录 (多状态)
-- ===================================================================
INSERT INTO `withdrawal_wsh` (`user_id_wsh`, `amount_wsh`, `fee_wsh`, `actual_amount_wsh`, `bank_name_wsh`, `bank_card_wsh`, `account_name_wsh`, `status_wsh`, `remark_wsh`)
SELECT u.id_wsh, 1000.00, 1.00, 999.00, '招商银行', '622588****8888', '孙七', 'completed', '测试提现完成'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

INSERT INTO `withdrawal_wsh` (`user_id_wsh`, `amount_wsh`, `fee_wsh`, `actual_amount_wsh`, `bank_name_wsh`, `bank_card_wsh`, `account_name_wsh`, `status_wsh`, `remark_wsh`)
SELECT u.id_wsh, 500.00, 0.50, 499.50, '工商银行', '622200****6666', '赵六', 'pending', '等待审核'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_004';

INSERT INTO `withdrawal_wsh` (`user_id_wsh`, `amount_wsh`, `fee_wsh`, `actual_amount_wsh`, `bank_name_wsh`, `bank_card_wsh`, `account_name_wsh`, `status_wsh`, `remark_wsh`)
SELECT u.id_wsh, 2000.00, 2.00, 1998.00, '建设银行', '621700****2222', '李四', 'approved', '审核通过，待打款'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_002';

-- ===================================================================
-- 8. favorite_wsh — 收藏 (商家/看护人, 用户+目标唯一)
-- ===================================================================
INSERT INTO `favorite_wsh` (`user_id_wsh`, `target_id_wsh`, `target_type_wsh`)
SELECT u.id_wsh, m.id_wsh, 'merchant'
FROM `user_wsh` u, `merchant_wsh` m WHERE u.username_wsh = 'adopter_test_001' AND m.name_wsh = '安心宠物生活馆';

INSERT INTO `favorite_wsh` (`user_id_wsh`, `target_id_wsh`, `target_type_wsh`)
SELECT u.id_wsh, m.id_wsh, 'merchant'
FROM `user_wsh` u, `merchant_wsh` m WHERE u.username_wsh = 'adopter_test_002' AND m.name_wsh = '萌宠驿站';

INSERT INTO `favorite_wsh` (`user_id_wsh`, `target_id_wsh`, `target_type_wsh`)
SELECT u.id_wsh, (SELECT k.id_wsh FROM `keeper_wsh` k, `merchant_wsh` m WHERE k.name_wsh = '张师傅' AND m.name_wsh = '安心宠物生活馆' AND k.merchant_id_wsh = m.id_wsh LIMIT 1), 'keeper'
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_003';

INSERT INTO `favorite_wsh` (`user_id_wsh`, `target_id_wsh`, `target_type_wsh`)
SELECT u.id_wsh, m.id_wsh, 'merchant'
FROM `user_wsh` u, `merchant_wsh` m WHERE u.username_wsh = 'adopter_test_005' AND m.name_wsh = '宠乐时光寄养中心';

-- ===================================================================
-- 9. notice_wsh — 公告/Banner + notice_read_wsh
-- ===================================================================
INSERT INTO `notice_wsh` (`title_wsh`, `content_wsh`, `type_wsh`, `image_url_wsh`, `link_url_wsh`, `sort_order_wsh`, `status_wsh`)
VALUES
('夏季宠物防暑指南', '夏季高温天气请注意宠物防暑降温，避免正午外出，保证充足饮水，如有异常及时就医。', 'notice', 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=600', '', 1, 1),
('中秋寄养预订开放', '中秋假期寄养服务现已开放预订，热门档期有限，请提前安排。', 'notice', 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=600', '', 2, 1);

INSERT INTO `notice_read_wsh` (`notice_id_wsh`, `user_id_wsh`)
SELECT n.id_wsh, u.id_wsh FROM `notice_wsh` n, `user_wsh` u WHERE u.username_wsh = 'adopter_test_001' AND n.title_wsh = '夏季宠物防暑指南';

INSERT INTO `notice_read_wsh` (`notice_id_wsh`, `user_id_wsh`)
SELECT n.id_wsh, u.id_wsh FROM `notice_wsh` n, `user_wsh` u WHERE u.username_wsh = 'adopter_test_002' AND n.title_wsh = '夏季宠物防暑指南';

-- ===================================================================
-- 11. content_review_wsh — 内容审核
-- ===================================================================
INSERT INTO `content_review_wsh` (`target_type_wsh`, `target_id_wsh`, `reporter_id_wsh`, `reason_wsh`, `status_wsh`, `reviewer_id_wsh`, `review_remark_wsh`)
SELECT 'rating',
       (SELECT r.id_wsh FROM `rating_wsh` r WHERE r.order_id_wsh = (SELECT id_wsh FROM `pet_order_wsh` WHERE order_no_wsh = 'ORDTEST20260815004') AND r.target_type_wsh = 'keeper' LIMIT 1),
       (SELECT id_wsh FROM `user_wsh` WHERE username_wsh = 'adopter_test_002' LIMIT 1),
       '评价内容疑似与事实不符', 'pending', NULL, NULL;

INSERT INTO `content_review_wsh` (`target_type_wsh`, `target_id_wsh`, `reporter_id_wsh`, `reason_wsh`, `status_wsh`, `reviewer_id_wsh`, `review_remark_wsh`)
SELECT 'complaint',
       (SELECT id_wsh FROM `complaint_wsh` WHERE title_wsh = '遛弯时间太短' LIMIT 1),
       (SELECT id_wsh FROM `user_wsh` WHERE username_wsh = 'adopter_test_001' LIMIT 1),
       '投诉内容待平台审核确认', 'approved', (SELECT id_wsh FROM `user_wsh` WHERE username_wsh = 'admin' LIMIT 1), '审核通过，投诉有效';

-- ===================================================================
-- 12. refund_wsh — 退款 (关联退款中/已退款订单)
-- ===================================================================
INSERT INTO `refund_wsh` (`order_id_wsh`, `order_no_wsh`, `amount_wsh`, `reason_wsh`, `status_wsh`, `order_status_before_refund_wsh`)
SELECT o.id_wsh, o.order_no_wsh, o.final_amount_wsh, '行程有变，申请退款', 'completed', 'paid'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815006';

INSERT INTO `refund_wsh` (`order_id_wsh`, `order_no_wsh`, `amount_wsh`, `reason_wsh`, `status_wsh`, `order_status_before_refund_wsh`)
SELECT o.id_wsh, o.order_no_wsh, o.final_amount_wsh, '对服务不满意申请部分退款', 'pending', 'completed'
FROM `pet_order_wsh` o WHERE o.order_no_wsh = 'ORDTEST20260815008';

-- ===================================================================
-- 13. tip_wsh — 打赏
-- ===================================================================
INSERT INTO `tip_wsh` (`order_id_wsh`, `from_user_id_wsh`, `to_user_id_wsh`, `amount_wsh`, `message_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, k.user_id_wsh, 50.00, '辛苦了，感谢细心照顾！'
FROM `pet_order_wsh` o JOIN `keeper_wsh` k ON k.id_wsh = o.keeper_id_wsh WHERE o.order_no_wsh = 'ORDTEST20260815004';

INSERT INTO `tip_wsh` (`order_id_wsh`, `from_user_id_wsh`, `to_user_id_wsh`, `amount_wsh`, `message_wsh`)
SELECT o.id_wsh, o.owner_id_wsh, k.user_id_wsh, 30.00, '狗狗很黏你，看得出很有爱心'
FROM `pet_order_wsh` o JOIN `keeper_wsh` k ON k.id_wsh = o.keeper_id_wsh WHERE o.order_no_wsh = 'ORDTEST20260815013';

-- ===================================================================
-- 14. chat_message_wsh — 聊天消息 (订单沟通)
-- ===================================================================
INSERT INTO `chat_message_wsh` (`from_user_id_wsh`, `to_user_id_wsh`, `order_id_wsh`, `content_wsh`, `type_wsh`, `read_wsh`)
SELECT o.owner_id_wsh, k.user_id_wsh, o.id_wsh, '麻烦今日多带它跑跑，它精力比较旺盛', 'text', 1
FROM `pet_order_wsh` o JOIN `keeper_wsh` k ON k.id_wsh = o.keeper_id_wsh WHERE o.order_no_wsh = 'ORDTEST20260815005';

INSERT INTO `chat_message_wsh` (`from_user_id_wsh`, `to_user_id_wsh`, `order_id_wsh`, `content_wsh`, `type_wsh`, `read_wsh`)
SELECT k.user_id_wsh, o.owner_id_wsh, o.id_wsh, '好的，已收到，今天安排了两次遛弯。', 'text', 1
FROM `pet_order_wsh` o JOIN `keeper_wsh` k ON k.id_wsh = o.keeper_id_wsh WHERE o.order_no_wsh = 'ORDTEST20260815005';

INSERT INTO `chat_message_wsh` (`from_user_id_wsh`, `to_user_id_wsh`, `order_id_wsh`, `content_wsh`, `type_wsh`, `read_wsh`)
SELECT k.user_id_wsh, o.owner_id_wsh, o.id_wsh, 'https://images.unsplash.com/photo-1558788353-f76d92427f16?w=600', 'image', 0
FROM `pet_order_wsh` o JOIN `keeper_wsh` k ON k.id_wsh = o.keeper_id_wsh WHERE o.order_no_wsh = 'ORDTEST20260815010';

-- ===================================================================
-- 15. file_record_wsh — 文件记录 (服务图片/头像/评价图, 供页面显示真实宠物图)
-- ===================================================================
INSERT INTO `file_record_wsh` (`original_name_wsh`, `object_name_wsh`, `size_wsh`, `content_type_wsh`, `purpose_wsh`, `merchant_id_wsh`, `user_id_wsh`)
SELECT 'golden-retriever.jpg', '/minio/pet-service/seed/golden-retriever.jpg', 204800, 'image/jpeg', 'product', m.id_wsh, NULL
FROM `merchant_wsh` m WHERE m.name_wsh = '安心宠物生活馆';

INSERT INTO `file_record_wsh` (`original_name_wsh`, `object_name_wsh`, `size_wsh`, `content_type_wsh`, `purpose_wsh`, `merchant_id_wsh`, `user_id_wsh`)
SELECT 'orange-cat.jpg', '/minio/pet-service/seed/orange-cat.jpg', 182400, 'image/jpeg', 'product', m.id_wsh, NULL
FROM `merchant_wsh` m WHERE m.name_wsh = '萌宠驿站';

INSERT INTO `file_record_wsh` (`original_name_wsh`, `object_name_wsh`, `size_wsh`, `content_type_wsh`, `purpose_wsh`, `merchant_id_wsh`, `user_id_wsh`)
SELECT 'ratings-pet-care.jpg', '/minio/pet-service/seed/ratings-pet-care.jpg', 156800, 'image/jpeg', 'product', m.id_wsh, NULL
FROM `merchant_wsh` m WHERE m.name_wsh = '宠乐时光寄养中心';

-- ===================================================================
-- 16. operation_log_wsh — 操作日志 (少量示例)
-- ===================================================================
INSERT INTO `operation_log_wsh` (`user_id_wsh`, `username_wsh`, `module_wsh`, `operation_wsh`, `description_wsh`, `method_wsh`, `request_url_wsh`, `ip_address_wsh`, `duration_wsh`, `status_wsh`)
SELECT u.id_wsh, u.username_wsh, 'order', 'CREATE', '创建测试订单', 'POST', '/api/order', '127.0.0.1', 120, 1
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_001';

INSERT INTO `operation_log_wsh` (`user_id_wsh`, `username_wsh`, `module_wsh`, `operation_wsh`, `description_wsh`, `method_wsh`, `request_url_wsh`, `ip_address_wsh`, `duration_wsh`, `status_wsh`)
SELECT u.id_wsh, u.username_wsh, 'wallet', 'RECHARGE', '余额充值', 'POST', '/api/wallet/recharge', '127.0.0.1', 80, 1
FROM `user_wsh` u WHERE u.username_wsh = 'adopter_test_005';

-- ===================================================================
-- 完成
-- ===================================================================