-- ===================================================================
-- test-cs-data.sql
-- 客服模块测试数据（为 testcs 账号准备）
-- 日期: 2026-08-16
-- 说明: 仅包含 INSERT 语句，可重复执行（基于标题+创建人去重约束前先检查）
-- 范围: 工单(9条) + 工单消息 + 投诉(6条) + 客服申请记录(2条)
-- 覆盖: 工单 4状态×4分类×4优先级; 投诉 4状态; 关联订单/证据/消息
-- ===================================================================

USE `pet_service`;

-- ===================================================================
-- 1. 客服申请记录: testcs 关联第二商家(101), 验证多商家场景
-- ===================================================================
INSERT INTO `merchant_customer_service_wsh` (`merchant_id_wsh`, `user_id_wsh`, `applicant_note_wsh`, `review_note_wsh`, `status_wsh`, `reviewer_id_wsh`, `reviewed_at_wsh`)
SELECT 101, u.id_wsh, 'Second merchant for multi-merchant testing', 'approved', 'approved', 101, NOW()
FROM `user_wsh` u WHERE u.username_wsh = 'testcs'
  AND NOT EXISTS (SELECT 1 FROM `merchant_customer_service_wsh` m WHERE m.merchant_id_wsh = 101 AND m.user_id_wsh = u.id_wsh);

-- 另一用户向商家1申请客服(pending), 测试待审核列表
INSERT INTO `merchant_customer_service_wsh` (`merchant_id_wsh`, `user_id_wsh`, `applicant_note_wsh`, `status_wsh`)
SELECT 1, u.id_wsh, 'I would like to join the CS team', 'pending'
FROM `user_wsh` u WHERE u.username_wsh = 'testuser'
  AND NOT EXISTS (SELECT 1 FROM `merchant_customer_service_wsh` m WHERE m.merchant_id_wsh = 1 AND m.user_id_wsh = u.id_wsh);

-- ===================================================================
-- 2. 工单测试数据 (商家1: 8条, 商家101: 1条)
--    id_wsh 由自增分配, 用 @ticket_* 变量记录以便插入消息
-- ===================================================================

-- 2.1 商家1 - 待处理(pending) 投诉类 urgent (owner: user 2)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `created_at_wsh`)
SELECT 1, NULL, 2, '寄养期间宠物受伤未及时通知', '我的金毛在寄养期间腿上发现伤口，商家没有第一时间通知我，直到接回宠物才发现。', 'complaint', 'urgent', 'pending', NOW() - INTERVAL 30 MINUTE
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '寄养期间宠物受伤未及时通知');

-- 2.2 商家1 - 待处理(pending) 咨询类 low (owner: user 107)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `created_at_wsh`)
SELECT 1, NULL, 107, '如何查看每日照护记录', '请问在哪里可以查看宠物每天的照护记录和照片？', 'question', 'low', 'pending', NOW() - INTERVAL 2 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '如何查看每日照护记录');

-- 2.3 商家1 - 待处理(pending) 建议类 medium (owner: user 110)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `created_at_wsh`)
SELECT 1, NULL, 110, '建议增加夜间监控直播功能', '希望能提供寄养期间的夜间监控直播，让宠物主人更放心。', 'suggestion', 'medium', 'pending', NOW() - INTERVAL 5 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '建议增加夜间监控直播功能');

-- 2.4 商家1 - 处理中(processing) 其他类 high, 已指派 testcs (owner: user 2)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `assignee_id_wsh`, `created_at_wsh`)
SELECT 1, NULL, 2, '接回宠物时间需要更改', '原定周六接回宠物，临时有事想改到周日，请问可以调整吗？', 'other', 'high', 'processing', 165, NOW() - INTERVAL 1 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '接回宠物时间需要更改');

-- 2.5 商家1 - 处理中(processing) 投诉类 urgent, 已指派 testcs (owner: user 110)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `assignee_id_wsh`, `created_at_wsh`)
SELECT 1, NULL, 110, '寄养费用计算有误', '订单显示住了5天，费用却按7天计算，请尽快核实退款。', 'complaint', 'urgent', 'processing', 165, NOW() - INTERVAL 2 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '寄养费用计算有误');

-- 2.6 商家1 - 已解决(resolved) 咨询类 medium, testcs处理 (owner: user 2)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `result_wsh`, `assignee_id_wsh`, `created_at_wsh`)
SELECT 1, NULL, 2, '寄养期间需要喂药', '我的猫需要每天早晚各喂一次药，寄养期间能否帮忙按时喂药？', 'question', 'medium', 'resolved', '已确认商家会按时喂药并每日拍照反馈，问题已解决。', 165, NOW() - INTERVAL 3 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '寄养期间需要喂药');

-- 2.7 商家1 - 已关闭(closed) 投诉类 high, testcs处理 (owner: user 107)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `result_wsh`, `assignee_id_wsh`, `created_at_wsh`)
SELECT 1, NULL, 107, '送养延迟两小时无人解释', '约定的送养时间晚了两小时，期间联系商家无人回应，体验很差。', 'complaint', 'high', 'closed', '商家已致歉并赠送一次免费美容服务作为补偿，用户接受，工单关闭。', 165, NOW() - INTERVAL 5 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '送养延迟两小时无人解释');

-- 2.8 商家1 - 待处理(pending) 关联已完成订单1 (owner: user 2)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `created_at_wsh`)
SELECT 1, 1, 2, '订单结算金额疑问', '订单 ORD20260601001 的结算金额和我的实付金额不一致，请帮忙核对。', 'complaint', 'medium', 'pending', NOW() - INTERVAL 6 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '订单结算金额疑问');

-- 2.9 商家101 - 待处理(pending) 咨询类 medium, 验证多商家权限 (owner: user 107)
INSERT INTO `ticket_wsh` (`merchant_id_wsh`, `order_id_wsh`, `user_id_wsh`, `title_wsh`, `content_wsh`, `category_wsh`, `priority_wsh`, `status_wsh`, `created_at_wsh`)
SELECT 101, 101, 107, '北京门店地址咨询', '请问北京门店的具体地址和营业时间？', 'question', 'medium', 'pending', NOW() - INTERVAL 1 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ticket_wsh` t WHERE t.title_wsh = '北京门店地址咨询');

-- ===================================================================
-- 3. 工单消息 (为有沟通的工单补充消息记录)
--    工单: 2.1(pending投诉) 2.4(processing) 2.6(resolved) 2.7(closed)
-- ===================================================================
INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 2, '伤口照片已上传，希望商家给个说法。', NOW() - INTERVAL 25 MINUTE
FROM `ticket_wsh` t WHERE t.title_wsh = '寄养期间宠物受伤未及时通知' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '伤口照片已上传，希望商家给个说法。');

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 2, '请问周六几点可以接回？', NOW() - INTERVAL 23 HOUR
FROM `ticket_wsh` t WHERE t.title_wsh = '接回宠物时间需要更改' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '请问周六几点可以接回？');

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 165, '您好，我是客服小测，已为您核实接回时间，可改至周日上午10点。', NOW() - INTERVAL 20 HOUR
FROM `ticket_wsh` t WHERE t.title_wsh = '接回宠物时间需要更改' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '您好，我是客服小测，已为您核实接回时间，可改至周日上午10点。');

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 2, '可以的，麻烦改到周日10点，谢谢！', NOW() - INTERVAL 19 HOUR
FROM `ticket_wsh` t WHERE t.title_wsh = '接回宠物时间需要更改' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '可以的，麻烦改到周日10点，谢谢！');

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 2, '请问喂药需要额外收费吗？', NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR
FROM `ticket_wsh` t WHERE t.title_wsh = '寄养期间需要喂药' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '请问喂药需要额外收费吗？');

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 165, '喂药服务不额外收费，已备注到订单信息中。', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR
FROM `ticket_wsh` t WHERE t.title_wsh = '寄养期间需要喂药' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '喂药服务不额外收费，已备注到订单信息中。');

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 107, '等了两个小时都没人接电话，太失望了。', NOW() - INTERVAL 5 DAY + INTERVAL 3 HOUR
FROM `ticket_wsh` t WHERE t.title_wsh = '送养延迟两小时无人解释' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '等了两个小时都没人接电话，太失望了。');

INSERT INTO `ticket_message_wsh` (`ticket_id_wsh`, `user_id_wsh`, `content_wsh`, `created_at_wsh`)
SELECT t.id_wsh, 165, '非常抱歉给您带来不便，商家已承诺赠送一次美容服务，请您确认。', NOW() - INTERVAL 4 DAY
FROM `ticket_wsh` t WHERE t.title_wsh = '送养延迟两小时无人解释' AND NOT EXISTS (
  SELECT 1 FROM `ticket_message_wsh` m WHERE m.content_wsh = '非常抱歉给您带来不便，商家已承诺赠送一次美容服务，请您确认。');

-- ===================================================================
-- 4. 投诉测试数据 (商家1: 5条, 商家101: 1条)
-- ===================================================================

-- 4.1 商家1 - 待处理(pending) 关联订单1(有聊天+护理记录证据) (owner: user 2)
INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `created_at_wsh`)
SELECT 1, 1, 2, 1, 'merchant', '寄养环境不干净', '接回宠物时发现身上有跳蚤，怀疑寄养环境清洁不到位，要求商家整改。', 'pending', NOW() - INTERVAL 3 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `complaint_wsh` c WHERE c.title_wsh = '寄养环境不干净');

-- 4.2 商家1 - 处理中(processing) (owner: user 107)
INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `result_wsh`, `created_at_wsh`)
SELECT NULL, 1, 107, 1, 'merchant', '商家拒接电话', '多次致电商家无人接听，客服也无法联系上，请尽快处理。', 'processing', '正在联系商家核实情况', NOW() - INTERVAL 1 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `complaint_wsh` c WHERE c.title_wsh = '商家拒接电话');

-- 4.3 商家1 - 已解决(resolved) 关联订单1 (owner: user 2)
INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `result_wsh`, `created_at_wsh`)
SELECT 1, 1, 2, 4, 'keeper', '看护员未按时喂食', '查看照护记录发现有一天没有喂食记录，担心宠物饿着。', 'resolved', '经核实当天有喂食但漏记记录，已要求商家完善记录，并补偿一次免费遛宠服务。', NOW() - INTERVAL 4 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `complaint_wsh` c WHERE c.title_wsh = '看护员未按时喂食');

-- 4.4 商家1 - 已驳回(rejected) (owner: user 110)
INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `result_wsh`, `created_at_wsh`)
SELECT NULL, 1, 110, 1, 'merchant', '服务态度差', '商家前台态度不好，要求道歉。', 'rejected', '经调取监控核实，商家服务过程无态度问题，投诉不成立，予以驳回。', NOW() - INTERVAL 7 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `complaint_wsh` c WHERE c.title_wsh = '服务态度差');

-- 4.5 商家1 - 待处理(pending) 无订单直接投诉商家 (owner: user 126)
INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `created_at_wsh`)
SELECT NULL, 1, 126, 1, 'merchant', '商家虚假宣传', '网页宣传的寄养环境与实际情况不符，存在虚假宣传嫌疑。', 'pending', NOW() - INTERVAL 8 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `complaint_wsh` c WHERE c.title_wsh = '商家虚假宣传');

-- 4.6 商家101 - 待处理(pending) 验证多商家权限 (owner: user 107)
INSERT INTO `complaint_wsh` (`order_id_wsh`, `merchant_id_wsh`, `owner_id_wsh`, `target_id_wsh`, `target_type_wsh`, `title_wsh`, `content_wsh`, `status_wsh`, `created_at_wsh`)
SELECT 101, 101, 107, 101, 'merchant', '北京门店价格不透明', '结账时发现额外收费项目未提前告知。', 'pending', NOW() - INTERVAL 2 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `complaint_wsh` c WHERE c.title_wsh = '北京门店价格不透明');
