# 数据库设计文档

## 概述

- 数据库：MySQL 8.0（生产）/ H2（测试）
- 字符集：`utf8mb4` / `utf8mb4_unicode_ci`
- 引擎：InnoDB
- 表数量：27 张

## 表结构总览

| # | 表名 | 模块 | 说明 |
|---|------|------|------|
| 1 | `user` | pet-system | 用户表 |
| 2 | `role` | pet-system | 角色表 |
| 3 | `user_role` | pet-system | 用户-角色关联 |
| 4 | `pet` | pet-business/pet | 宠物表 |
| 5 | `merchant` | pet-business/boarding | 商家表 |
| 6 | `keeper` | pet-business/boarding | 寄养员表 |
| 7 | `pet_service` | pet-business/boarding | 服务项目表 |
| 8 | `address` | pet-business/boarding | 收货地址表 |
| 9 | `pet_order` | pet-business/order | 订单表 |
| 10 | `payment` | pet-business/order | 支付表 |
| 11 | `refund` | pet-business/order | 退款表 |
| 12 | `tip` | pet-business/order | 打赏记录表 |
| 13 | `complaint` | pet-business/customer | 投诉表 |
| 14 | `chat_message` | pet-business/customer | 聊天消息表 |
| 15 | `rating` | pet-business/customer | 评价表 |
| 16 | `ticket` | pet-business/customer | 客服工单表 |
| 17 | `ticket_message` | pet-business/customer | 工单消息表 |
| 18 | `favorite` | pet-business/operation | 收藏表 |
| 19 | `notice` | pet-business/operation | 公告/Banner表 |
| 20 | `content_review` | pet-business/operation | 内容审核表 |
| 21 | `care_record` | pet-business/pet | 寄养过程记录表 |
| 22 | `wallet` | pet-business/finance | 钱包表 |
| 23 | `wallet_transaction` | pet-business/finance | 钱包流水表 |
| 24 | `withdrawal` | pet-business/finance | 提现记录表 |
| 25 | `ai_report` | pet-ai | AI报告表 |
| 26 | `knowledge_document` | pet-ai | 知识库文档表 |
| 27 | `document_embedding` | pet-ai | 文档向量嵌入表 |

---

## 1. pet-system 模块

### user (用户表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(255) | 密码(BCrypt加密) |
| nickname | VARCHAR(50) | 昵称 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(500) | 头像URL |
| email | VARCHAR(100) | 邮箱 |
| address | VARCHAR(255) | 地址 |
| latitude | DECIMAL(10,6) | 纬度 |
| longitude | DECIMAL(10,6) | 经度 |
| status | TINYINT | 0-禁用 1-启用 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### role (角色表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(50) UNIQUE | 角色名称 |
| code | VARCHAR(50) UNIQUE | 角色编码(ADMIN/OWNER/KEEPER/MERCHANT/CUSTOMER_SERVICE) |
| description | VARCHAR(255) | 描述 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

### user_role (用户角色关联表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| role_id | BIGINT | 角色ID |

唯一索引：`(user_id, role_id)`

---

## 2. pet-business/pet 模块

### pet (宠物表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| owner_id | BIGINT | 主人ID |
| name | VARCHAR(50) | 宠物名 |
| type | VARCHAR(50) | 类型(dog/cat/other) |
| breed | VARCHAR(100) | 品种 |
| age | INT | 年龄 |
| weight | DECIMAL(10,2) | 体重(kg) |
| gender | TINYINT | 0-雌 1-雄 |
| sterilized | TINYINT | 是否绝育 |
| vaccinated | TINYINT | 是否免疫 |
| avatar | VARCHAR(500) | 头像 |
| description | TEXT | 描述 |
| allergies | TEXT | 过敏信息 |
| habits | TEXT | 习性 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### care_record (寄养过程记录表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单ID |
| pet_id | BIGINT | 宠物ID |
| keeper_id | BIGINT | 寄养员ID |
| type | VARCHAR(20) | feed-喂食 activity-活动 medication-用药 health-健康 |
| content | TEXT | 记录内容 |
| images | VARCHAR(2000) | 图片URL(逗号分隔) |
| record_time | DATETIME | 记录时间 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

---

## 3. pet-business/boarding 模块

### merchant (商家表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| name | VARCHAR(100) | 商家名 |
| phone | VARCHAR(20) | 电话 |
| address | VARCHAR(255) | 地址 |
| latitude | DECIMAL(10,6) | 纬度 |
| longitude | DECIMAL(10,6) | 经度 |
| description | TEXT | 描述 |
| business_license | VARCHAR(500) | 营业执照 |
| rating | DECIMAL(3,2) | 评分(默认5.00) |
| status | TINYINT | 0-待审核 1-已通过 2-已驳回 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### keeper (寄养员表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| merchant_id | BIGINT | 商家ID |
| user_id | BIGINT | 用户ID |
| name | VARCHAR(50) | 姓名 |
| phone | VARCHAR(20) | 电话 |
| avatar | VARCHAR(500) | 头像 |
| experience_years | INT | 经验年限 |
| rating | DECIMAL(3,2) | 评分(默认5.00) |
| completion_rate | DECIMAL(5,2) | 完成率(默认100.00) |
| complaint_rate | DECIMAL(5,2) | 投诉率(默认0.00) |
| price_per_day | DECIMAL(10,2) | 每日价格 |
| max_pets | INT | 最大接单数(默认5) |
| current_pets | INT | 当前接单数(默认0) |
| status | TINYINT | 0-离线 1-在线 |
| bio | TEXT | 简介 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### pet_service (服务项目表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| merchant_id | BIGINT | 商家ID |
| name | VARCHAR(100) | 服务名称 |
| type | VARCHAR(50) | 类型(boarding/grooming/training/walk) |
| description | TEXT | 描述 |
| price | DECIMAL(10,2) | 价格 |
| unit | VARCHAR(20) | 单位(默认day) |
| images | TEXT | 图片URL |
| status | TINYINT | 0-下架 1-上架 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### address (收货地址表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| label | VARCHAR(50) | 标签(家/公司/学校) |
| name | VARCHAR(50) | 收货人姓名 |
| phone | VARCHAR(20) | 收货人电话 |
| address | VARCHAR(500) | 详细地址 |
| detail | VARCHAR(500) | 门牌号补充信息 |
| latitude | DECIMAL(10,7) | 纬度 |
| longitude | DECIMAL(10,7) | 经度 |
| is_default | TINYINT | 0-否 1-是 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

## 4. pet-business/order 模块

### pet_order (订单表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_no | VARCHAR(50) UNIQUE | 订单号 |
| owner_id | BIGINT | 主人ID |
| pet_id | BIGINT | 宠物ID |
| keeper_id | BIGINT | 寄养员ID |
| merchant_id | BIGINT | 商家ID |
| service_id | BIGINT | 服务项目ID |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期 |
| days | INT | 天数 |
| price_per_day | DECIMAL(10,2) | 每日价格 |
| total_amount | DECIMAL(10,2) | 总金额 |
| discount | DECIMAL(10,2) | 折扣 |
| final_amount | DECIMAL(10,2) | 实付金额 |
| status | VARCHAR(20) | pending/paid/in_progress/completed/cancelled/refunding/refunded |
| remark | TEXT | 备注 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### payment (支付表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单ID |
| order_no | VARCHAR(50) | 订单号 |
| pay_no | VARCHAR(100) | 支付号 |
| amount | DECIMAL(10,2) | 金额 |
| method | VARCHAR(20) | wechat/alipay/balance |
| status | VARCHAR(20) | pending/success/failed |
| paid_at | DATETIME | 支付时间 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### refund (退款表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单ID |
| order_no | VARCHAR(50) | 订单号 |
| amount | DECIMAL(10,2) | 退款金额 |
| reason | TEXT | 退款原因 |
| status | VARCHAR(20) | pending/approved/rejected/completed |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### tip (打赏记录表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单ID |
| from_user_id | BIGINT | 打赏用户 |
| to_user_id | BIGINT | 接收用户 |
| amount | DECIMAL(10,2) | 打赏金额 |
| message | VARCHAR(200) | 打赏留言 |
| created_at | DATETIME | 创建时间 |

---

## 5. pet-business/customer 模块

### complaint (投诉表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单ID |
| owner_id | BIGINT | 投诉用户 |
| target_id | BIGINT | 被投诉方ID |
| target_type | VARCHAR(20) | merchant/keeper/service |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 内容 |
| images | TEXT | 图片 |
| status | VARCHAR(20) | pending/processing/resolved/rejected |
| result | TEXT | 处理结果 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### chat_message (聊天消息表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| from_user_id | BIGINT | 发送人 |
| to_user_id | BIGINT | 接收人 |
| order_id | BIGINT | 关联订单 |
| content | TEXT | 消息内容 |
| type | VARCHAR(20) | text/image/video/file |
| file_url | VARCHAR(500) | 文件URL |
| read | TINYINT | 0-未读 1-已读 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

### rating (评价表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单ID |
| user_id | BIGINT | 评价用户 |
| target_id | BIGINT | 评价对象ID |
| target_type | VARCHAR(20) | merchant/keeper/service |
| score | TINYINT | 评分(1-5) |
| content | TEXT | 评价内容 |
| images | TEXT | 图片URL |
| reply | TEXT | 商家回复 |
| reply_at | DATETIME | 回复时间 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

### ticket (客服工单表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 创建用户ID |
| title | VARCHAR(200) | 工单标题 |
| content | TEXT | 工单内容 |
| category | VARCHAR(50) | complaint/question/suggestion/other |
| priority | VARCHAR(20) | low/medium/high/urgent |
| status | VARCHAR(20) | pending/processing/resolved/closed |
| assignee_id | BIGINT | 处理人ID |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### ticket_message (工单消息表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| ticket_id | BIGINT | 工单ID |
| user_id | BIGINT | 发送人ID |
| content | TEXT | 消息内容 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

---

## 6. pet-business/operation 模块

### favorite (收藏表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| target_id | BIGINT | 目标ID |
| target_type | VARCHAR(20) | merchant/keeper/service |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

唯一索引：`(user_id, target_id, target_type)`

### notice (公告/Banner表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 内容 |
| type | VARCHAR(20) | notice-公告 banner-Banner |
| image_url | VARCHAR(500) | 图片URL |
| link_url | VARCHAR(500) | 跳转链接 |
| sort_order | INT | 排序 |
| status | TINYINT | 1-显示 0-隐藏 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### content_review (内容审核表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| target_type | VARCHAR(50) | rating/complaint/chat |
| target_id | BIGINT | 目标ID |
| reporter_id | BIGINT | 举报人 |
| reason | VARCHAR(500) | 举报原因 |
| status | VARCHAR(20) | pending/approved/rejected |
| reviewer_id | BIGINT | 审核人 |
| review_remark | VARCHAR(500) | 审核备注 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

## 7. pet-business/finance 模块

### wallet (钱包表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| balance | DECIMAL(12,2) | 余额(默认0.00) |
| frozen_amount | DECIMAL(12,2) | 冻结金额(默认0.00) |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### wallet_transaction (钱包流水表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| wallet_id | BIGINT | 钱包ID |
| user_id | BIGINT | 用户ID |
| type | VARCHAR(20) | income/expense/withdraw/refund |
| amount | DECIMAL(12,2) | 金额 |
| balance_after | DECIMAL(12,2) | 变动后余额 |
| order_id | BIGINT | 关联订单 |
| description | VARCHAR(500) | 描述 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

### withdrawal (提现记录表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| amount | DECIMAL(12,2) | 提现金额 |
| fee | DECIMAL(12,2) | 手续费(默认0.00) |
| actual_amount | DECIMAL(12,2) | 实际到账 |
| bank_name | VARCHAR(100) | 银行名称 |
| bank_card | VARCHAR(100) | 银行卡号 |
| account_name | VARCHAR(100) | 持卡人 |
| status | VARCHAR(20) | pending/approved/rejected/completed |
| remark | VARCHAR(500) | 备注 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

## 8. pet-ai 模块

### ai_report (AI报告表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT | 订单ID |
| pet_id | BIGINT | 宠物ID |
| keeper_id | BIGINT | 寄养员ID |
| content | TEXT | 报告内容 |
| type | VARCHAR(50) | daily/final/health |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

### knowledge_document (知识库文档表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 内容 |
| category | VARCHAR(50) | boarding/refund/complaint/care/vaccine/agreement/rule |
| source_type | VARCHAR(20) | pdf/word/markdown/txt |
| source_path | VARCHAR(500) | 源文件路径 |
| word_count | INT | 字数(默认0) |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### document_embedding (文档向量嵌入表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| document_id | BIGINT FK | 文档ID |
| embedding | LONGTEXT | 向量(JSON数组) |
| dimension | INT | 向量维度(默认0) |
| chunk_index | INT | 分块序号 |
| chunk_text | TEXT | 分块文本 |
| deleted | TINYINT | 逻辑删除 |
| created_at | DATETIME | 创建时间 |

---

## 索引设计

| 表名 | 索引名 | 字段 |
|------|--------|------|
| user | `uk_username` | username |
| role | `uk_name` | name |
| role | `uk_code` | code |
| user_role | `uk_user_role` | (user_id, role_id) |
| pet_order | `uk_order_no` | order_no |
| favorite | `uk_user_target` | (user_id, target_id, target_type) |
| document_embedding | FK | document_id → knowledge_document.id |
