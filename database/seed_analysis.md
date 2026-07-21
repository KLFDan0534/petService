# 种子数据分析报告

## 文件清单

| 文件 | 位置 | 行数 | 大小 |
|------|------|------|------|
| seed.sql | pet-admin/src/main/resources/db/seed.sql | 72行 | 3.1KB |
| seed-data.sql | pet-admin/src/main/resources/db/seed-data.sql | 183行 | 6.7KB |

---

## seed.sql 分析

### 恢复范围

| 表名 | 插入行数 | 说明 |
|------|----------|------|
| role_wsh | 4 | 系统管理员/宠物主人/上门喂养/商家 |
| user_wsh | 4 | admin/owner/merchant1/keeper1 |
| user_role_wsh | 4 | 4个用户角色分配 |
| category_wsh | 8 | 狗/猫/鸟类/爬宠及子分类 |
| pet_wsh | 3 | 旺财/咪咪/AdminPet |
| merchant_wsh | 1 | 阳光宠物中心 |
| keeper_wsh | 3 | 李阿姨/张叔叔/王姐 |
| pet_service_wsh | 6 | 寄养/美容/训练/遛弯/医疗 |
| pet_order_wsh | 7 | 包含所有状态(pending→completed) |
| payment_wsh | 2 | 微信+余额支付 |
| rating_wsh | 2 | 评价 |
| knowledge_document_wsh | 2 | 寄养协议+常见问题 |
| **合计** | **~52行** | **覆盖13张表** |

### 特点

- 中文数据
- 硬编码ID（1-7）
- 时间使用 NOW() 和 CURDATE()
- 密码哈希与 seed-data.sql 不同

### 风险

- ⚠️ 与 seed-data.sql 使用不同密码哈希
- ⚠️ 与 seed-data.sql 使用不同字段值（如宠物英文名 vs 中文名）
- 执行两次可能因 INSERT IGNORE 导致部分数据不完整

---

## seed-data.sql 分析

### 恢复范围

| 表名 | 插入行数 | 业务说明 |
|------|----------|----------|
| role_wsh | 5 | 增加 Customer Service |
| user_wsh | 5 | 增加 cs1 |
| user_role_wsh | 6 | 含多种角色组合 |
| category_wsh | 8 | 英文分类名 |
| pet_wsh | 3 | Fluffy/Buddy/AdminPet |
| merchant_wsh | 1 | Happy Pet Store |
| keeper_wsh | 3 | Lucy/Zhang/Wang |
| pet_service_wsh | 6 | 6种服务 |
| pet_order_wsh | 7 | 7种订单状态 |
| payment_wsh | 2 | 微信+支付宝 |
| rating_wsh | 2 | 英文评价 |
| address_wsh | 3 | Tom/Mike/Lucy地址 |
| wallet_wsh | 5 | 5个用户钱包 |
| wallet_transaction_wsh | 2 | 收支记录 |
| notice_wsh | 1 | 欢迎通知 |
| favorite_wsh | 2 | 收藏 |
| chat_message_wsh | 3 | 聊天记录 |
| complaint_wsh | 1 | 投诉 |
| ticket_wsh | 2 | 工单 |
| ticket_message_wsh | 4 | 工单消息 |
| content_review_wsh | 1 | 内容审核 |
| care_record_wsh | 3 | 护理记录 |
| tip_wsh | 1 | 小费 |
| notification_wsh | 5 | 通知 |
| business_hours_wsh | 7 | 营业时间 |
| adoption_pet_wsh | 2 | 领养宠物 |
| adoption_application_wsh | 1 | 领养申请 |
| knowledge_document_wsh | 5 | 知识文档 |
| **合计** | **~106行** | **覆盖28张表** |

### 特点

- 英文数据（国际化设计）
- 完整业务闭环：用户→订单→支付→评价→工单→投诉
- 时间使用 NOW()
- 与 seed.sql 不同的密码哈希
- 统一 BCrypt("123456") 密码

### 风险

- ⚠️ 使用 INSERT IGNORE，如果主键冲突会静默跳过
- ⚠️ 仅覆盖 28/35 张表（缺少 ai_report_wsh, document_embedding_wsh 等）

---

## 综合分析

| 对比项 | Binlog恢复 | seed-data.sql | seed.sql |
|--------|-----------|---------------|----------|
| 数据真实性 | **真实业务数据** | ❌ 演示数据 | ❌ 演示数据 |
| 数据完整性 | 完整 34+张表 | 28张表 | 13张表 |
| 数据量 | **951次操作** | ~106行 | ~52行 |
| 密码兼容 | ✅ 原密码 | ⚠️ 123456 | ⚠️ 不同哈希 |
| 执行风险 | 无（跳过已存在） | 低（IGNORE） | 低（IGNORE） |
| 推荐等级 | **★★★★★** | ★★★☆☆ | ★★☆☆☆ |

**结论：Seed SQL 只能恢复演示数据，无法恢复真实业务数据。建议仅在 Binlog 恢复不可用时作为备选方案。**
