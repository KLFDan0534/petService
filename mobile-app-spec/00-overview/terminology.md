# Terminology（术语表）

业务与字段术语（EXISTING，来自前端 statusMaps.js / 后端枚举 / 页面文案）：

| 术语 | 代码值 | 含义 |
|---|---|---|
| 宠物主人 | OWNER | 下单用户 |
| 寄养师/照护师 | KEEPER | 执行寄养照护的人员 |
| 商家 | MERCHANT | 门店经营方，雇佣寄养师 |
| 客服 | CUSTOMER_SERVICE | 平台/商家客服坐席 |
| 管理员 | ADMIN | 平台运营管理 |
| 寄养 | Boarding | 宠物入住照护服务 |
| 服务项 | ServiceItem（/api/services） | 可购买的服务 SKU |
| 服务分类 | Category（两级，parent/parentId） | 服务目录树 |
| 营业时间 | BusinessHours | 商家营业时段/店铺状态 |
| 履约 | OrderFulfillment | 订单执行域（时间线/日报/对对话） |
| 日报 | DailyStatus/CareRecord | 每日照护记录（可传图） |
| 考勤 | KeeperAttendance | check-in/check-out |
| 请假 | KeeperLeave | 寄养师请假审批 |
| 资质 | Qualification | 寄养师/商家资质审核材料 |
| 实名 | RealNameReview | 实名认证审核（0 未实/1 审核中/2 通过/3 未通过） |
| 小费 | Tip | 订单打赏 |
| 优惠券 | Coupon（模板/领取/可用/报价 quote） | 营销券 |
| 会员 | Membership（计划/权益/使用次数） | 会员订阅 |
| 工单 | Ticket（分类 complaint/question/suggestion/other；优先级 low→urgent） | 售后请求 |
| 投诉 | Complaint | 争议处理（受理/解决/驳回） |
| 会话 | Conversation | 用户↔商家 IM |
| 评价 | Rating（商家回复 reply） | 订单评价 |
| 信用 | CreditReputation / /statistics/reputation | 信用分 |
| 异常 | Anomaly | 异常记录页 |
| 回收站 | RecycleBin | 软删除恢复 |
| 内容审核 | ContentReview | UGC 审核流 |
| 操作日志 | OperationLog | 后台审计 |

**字段命名**：所有 API 字段带 `_wsh` 后缀（如 `user_id_wsh`、`access_token_wsh`、`roles_wsh`、`status_label_wsh` 前端合成字段）。
**状态徽章**：`statusMaps.js` 中 `{label, badge}` 映射 → 移动端用 StatusBadge 组件渲染（色+文，见 02-components）。
