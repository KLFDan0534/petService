# 数据库字典 (Database Dictionary)

> 生成日期: 2026-06-28
> 数据库: pet_service (MySQL 8.0.46)
> 字符集: utf8mb4 | 排序规则: utf8mb4_0900_ai_ci (默认)
> 总表数: 35 | 总字段数: 368 | 总索引数: 63 | 总唯一索引: 10 | 总外键: 1

---

## 统计概览

| 指标 | 数值 |
|------|------|
| 总表数 | 35 |
| 总字段数 | 368 |
| 总索引数 | 63 (主键35 + 唯一10 + 普通18) |
| 总唯一索引 | 10 |
| 总外键 | 1 |
| 存在问题数量 | 12 |
| 建议优化数量 | 8 |
| 业务模块数 | 7 |
| Entity文件数 | 34 |
| Mapper文件数 | 35 |
| Service文件数 | 33 |
| Controller文件数 | 33 |

---

## 模块: pet-system (系统模块) — 3张表

---

### 1. user_wsh (用户表)

| 属性 | 值 |
|------|-----|
| **表说明** | 系统用户账户表，存储用户登录、个人信息和实名认证数据 |
| **业务模块** | pet-system (系统模块) |
| **引擎** | InnoDB |
| **字符集** | utf8mb4 |
| **排序规则** | utf8mb4_0900_ai_ci |
| **自增起始** | 119 |
| **记录数** | ~38 |
| **Entity** | `User.java` (com.pet.system.entity) |
| **Mapper** | `UserMapper.java` (extends BaseMapper<User>) |
| **Service** | `UserService.java` → `UserServiceImpl.java` |
| **Controller** | `AuthController.java`, `UserController.java` |

#### 字段说明

| # | 字段名 | 类型 | 空 | 默认值 | 主键 | 唯一 | 索引 | 说明 |
|---|--------|------|----|--------|------|------|------|------|
| 1 | id_wsh | bigint | NO | AUTO_INC | ✅ | - | PRIMARY | 用户ID |
| 2 | username_wsh | varchar(50) | NO | - | - | ✅ | UNIQUE | 用户名 |
| 3 | password_wsh | varchar(255) | NO | - | - | - | - | 密码(BCrypt加密) |
| 4 | nickname_wsh | varchar(50) | YES | NULL | - | - | - | 昵称 |
| 5 | phone_wsh | varchar(20) | YES | NULL | - | - | - | 手机号 |
| 6 | avatar_wsh | varchar(500) | YES | NULL | - | - | - | 头像URL |
| 7 | email_wsh | varchar(100) | YES | NULL | - | - | - | 邮箱 |
| 8 | address_wsh | varchar(255) | YES | NULL | - | - | - | 地址 |
| 9 | latitude_wsh | decimal(10,6) | YES | NULL | - | - | - | 纬度 |
| 10 | longitude_wsh | decimal(10,6) | YES | NULL | - | - | - | 经度 |
| 11 | status_wsh | tinyint | YES | 1 | - | - | - | 状态: 0-禁用 1-启用 |
| 12 | deleted_wsh | tinyint | YES | 0 | - | - | - | 逻辑删除 |
| 13 | created_at_wsh | datetime | YES | CURRENT_TIMESTAMP | - | - | - | 创建时间 |
| 14 | updated_at_wsh | datetime | YES | CURRENT_TIMESTAMP ON UPDATE | - | - | - | 更新时间 |
| 15 | gender_wsh | tinyint | YES | 0 | - | - | - | 性别: 0-未知 1-男 2-女 |
| 16 | real_name_wsh | varchar(50) | YES | NULL | - | - | - | 真实姓名 |
| 17 | id_card_no_wsh | varchar(32) | YES | NULL | - | - | - | 身份证号 |
| 18 | real_name_status_wsh | tinyint | YES | 0 | - | - | - | 实名状态: 0-未认证 1-待审核 2-已认证 3-已驳回 |

#### 索引

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| PRIMARY | PRIMARY KEY | id_wsh | 主键 |
| username_wsh | UNIQUE | username_wsh | 用户名唯一 |

#### 废弃字段
无

#### 优化建议
- `password_wsh`使用BCrypt加密，长度255合理
- 建议对`phone_wsh`添加唯一索引(业务上手机号应唯一)

---

### 2. role_wsh (角色表)

| 属性 | 值 |
|------|-----|
| **表说明** | 系统角色定义表，支持RBAC权限模型 |
| **业务模块** | pet-system (系统模块) |
| **自增起始** | 7 |
| **记录数** | ~5 |
| **Entity** | `Role.java` |
| **Mapper** | `RoleMapper.java` |
| **Controller** | `RoleController.java` |

#### 字段说明

| # | 字段名 | 类型 | 空 | 默认值 | 主键 | 唯一 | 说明 |
|---|--------|------|----|--------|------|------|------|
| 1 | id_wsh | bigint | NO | AUTO_INC | ✅ | - | 角色ID |
| 2 | name_wsh | varchar(50) | NO | - | - | ✅ | 角色名称(如: 管理员) |
| 3 | code_wsh | varchar(50) | NO | - | - | ✅ | 角色编码(如: ADMIN) |
| 4 | description_wsh | varchar(255) | YES | NULL | - | - | 角色描述 |
| 5 | deleted_wsh | tinyint | YES | 0 | - | - | 逻辑删除 |
| 6 | created_at_wsh | datetime | YES | CURRENT_TIMESTAMP | - | - | 创建时间 |

#### 废弃字段
无

---

### 3. user_role_wsh (用户角色关联表)

| 属性 | 值 |
|------|-----|
| **表说明** | 用户与角色的多对多关联表 |
| **业务模块** | pet-system (系统模块) |
| **自增起始** | 94 |
| **记录数** | ~35 |
| **Entity** | `UserRole.java` |
| **Mapper** | `UserRoleMapper.java` |

#### 字段说明

| # | 字段名 | 类型 | 空 | 默认值 | 主键 | 唯一 | 说明 |
|---|--------|------|----|--------|------|------|------|
| 1 | id_wsh | bigint | NO | AUTO_INC | ✅ | - | 关联ID |
| 2 | user_id_wsh | bigint | NO | - | - | ✅(联合) | 用户ID |
| 3 | role_id_wsh | bigint | NO | - | - | ✅(联合) | 角色ID |
| 4 | created_at_wsh | datetime | YES | CURRENT_TIMESTAMP | - | - | 创建时间 |
| 5 | deleted_wsh | int | YES | 0 | - | - | 逻辑删除(⚠️ 类型为int，其他表为tinyint) |

#### 废弃字段
无

#### 优化建议
- `deleted_wsh`类型改为`tinyint`以保持全表一致

---

## 模块: pet-business/pet (宠物模块) — 3张表

---

### 4. pet_wsh (宠物表)

| 属性 | 值 |
|------|-----|
| **表说明** | 宠物基本信息表，存储宠物档案数据 |
| **业务模块** | pet-business → pet (宠物模块) |
| **自增起始** | 116 |
| **记录数** | ~19 |
| **Entity** | `Pet.java` (com.pet.pet.entity) |
| **Mapper** | `PetMapper.java` |
| **Service** | `PetService.java` |
| **Controller** | `PetController.java` |

#### 字段说明

| # | 字段名 | 类型 | 空 | 默认值 | 说明 |
|---|--------|------|----|--------|------|
| 1 | id_wsh | bigint | NO | AUTO_INC | 宠物ID |
| 2 | owner_id_wsh | bigint | NO | - | 主人ID |
| 3 | name_wsh | varchar(50) | NO | - | 宠物名称 |
| 4 | type_wsh | varchar(50) | NO | - | 类型: dog/cat/other |
| 5 | breed_wsh | varchar(100) | YES | NULL | 品种 |
| 6 | age_wsh | int | YES | NULL | 年龄(月) |
| 7 | weight_wsh | decimal(10,2) | YES | NULL | 体重(kg) |
| 8 | gender_wsh | tinyint | YES | NULL | 性别: 0-母 1-公 |
| 9 | sterilized_wsh | tinyint | YES | 0 | 是否绝育 |
| 10 | vaccinated_wsh | tinyint | YES | 0 | 是否接种疫苗 |
| 11 | avatar_wsh | varchar(500) | YES | NULL | 头像URL |
| 12 | description_wsh | text | YES | NULL | 描述 |
| 13 | allergies_wsh | text | YES | NULL | 过敏信息 |
| 14 | habits_wsh | text | YES | NULL | 生活习惯 |
| 15 | deleted_wsh | tinyint | YES | 0 | 逻辑删除 |
| 16 | created_at_wsh | datetime | YES | CURRENT_TIMESTAMP | 创建时间 |
| 17 | updated_at_wsh | datetime | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

### 5. category_wsh (分类表)

| 属性 | 值 |
|------|-----|
| **表说明** | 宠物分类/品种分类，支持父子层级 |
| **业务模块** | pet-business → pet (宠物模块) |
| **自增起始** | 9 |
| **Entity** | `Category.java` |
| **Mapper** | `CategoryMapper.java` |
| **Service** | `CategoryService.java` |
| **Controller** | `CategoryController.java` |

#### 字段说明

| # | 字段名 | 类型 | 空 | 默认值 | 说明 |
|---|--------|------|----|--------|------|
| 1 | id_wsh | bigint | NO | AUTO_INC | 分类ID |
| 2 | name_wsh | varchar(50) | NO | - | 分类名称 |
| 3 | parent_id_wsh | bigint | YES | 0 | 父分类ID |
| 4 | sort_order_wsh | int | YES | 0 | 排序 |
| 5 | deleted_wsh | tinyint | YES | 0 | 逻辑删除 |
| 6 | created_at_wsh | datetime | YES | CURRENT_TIMESTAMP | 创建时间 |
| 7 | updated_at_wsh | datetime | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

### 6. care_record_wsh (照护记录表)

| 属性 | 值 |
|------|-----|
| **表说明** | 宠物寄养期间的照护活动记录 |
| **业务模块** | pet-business → pet (宠物模块) |
| **自增起始** | 26 |
| **Entity** | `CareRecord.java` |
| **Mapper** | `CareRecordMapper.java` |
| **Service** | `CareRecordService.java` |
| **Controller** | `CareRecordController.java` |

#### 字段说明

| # | 字段名 | 类型 | 空 | 默认值 | 说明 |
|---|--------|------|----|--------|------|
| 1 | id_wsh | bigint | NO | AUTO_INC | 记录ID |
| 2 | order_id_wsh | bigint | NO | - | 订单ID |
| 3 | pet_id_wsh | bigint | YES | NULL | 宠物ID |
| 4 | keeper_id_wsh | bigint | YES | NULL | 寄养员ID |
| 5 | type_wsh | varchar(20) | YES | feed | 类型: feed-喂食 activity-活动 medication-用药 health-健康 |
| 6 | content_wsh | text | YES | NULL | 记录内容 |
| 7 | images_wsh | varchar(2000) | YES | NULL | 图片URL(逗号分隔) |
| 8 | record_time_wsh | datetime | YES | NULL | 记录时间 |
| 9 | deleted_wsh | tinyint | YES | 0 | 逻辑删除 |
| 10 | created_at_wsh | datetime | YES | CURRENT_TIMESTAMP | 创建时间 |

#### 优化建议
- 缺少`updated_at_wsh`字段

---

## 模块: pet-business/boarding (寄养模块) — 5张表

---

### 7. merchant_wsh (商家表)

| 属性 | 值 |
|------|-----|
| **表说明** | 寄养商家/店铺信息表 |
| **业务模块** | pet-business → boarding (寄养模块) |
| **自增起始** | 107 |
| **Entity** | `Merchant.java` |
| **Mapper** | `MerchantMapper.java` |
| **Service** | `MerchantService.java` |
| **Controller** | `MerchantController.java` |

#### 字段说明

| # | 字段名 | 类型 | 空 | 默认值 | 说明 |
|---|--------|------|----|--------|------|
| 1 | id_wsh | bigint | NO | AUTO_INC | 商家ID |
| 2 | user_id_wsh | bigint | NO | - | 用户ID |
| 3 | name_wsh | varchar(100) | NO | - | 商家名称 |
| 4 | phone_wsh | varchar(20) | YES | NULL | 联系电话 |
| 5 | address_wsh | varchar(255) | YES | NULL | 地址 |
| 6 | latitude_wsh | decimal(10,6) | YES | NULL | 纬度 |
| 7 | longitude_wsh | decimal(10,6) | YES | NULL | 经度 |
| 8 | description_wsh | text | YES | NULL | 描述 |
| 9 | business_license_wsh | varchar(500) | YES | NULL | 营业执照URL |
| 10 | rating_wsh | decimal(3,2) | YES | 5.00 | 评分 |
| 11 | status_wsh | tinyint | YES | 0 | 状态: 0-待审核 1-已通过 2-已驳回 |
| 12 | store_mode_wsh | tinyint | YES | 0 | 营业模式: 0-自动 1-手动开 2-手动关 |
| 13 | store_status_wsh | tinyint | YES | 0 | 实时营业状态: 0-休息 1-营业 |
| 14 | future_booking_enabled_wsh | tinyint | NO | 1 | 是否接受未来预约: 0-关闭 1-开启(默认开启) |
| 15 | deleted_wsh | tinyint | YES | 0 | 逻辑删除 |
| 16 | created_at_wsh | datetime | YES | CURRENT_TIMESTAMP | 创建时间 |
| 17 | updated_at_wsh | datetime | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| 18 | avatar_wsh | varchar(500) | YES | NULL | 商家头像 |

---

### 8. keeper_wsh (看护人表)

| 属性 | 值 |
|------|-----|
| **表说明** | 宠物看护人/寄养员信息表 |
| **业务模块** | pet-business → boarding (寄养模块) |
| **自增起始** | 108 |
| **Entity** | `Keeper.java` |
| **Mapper** | `KeeperMapper.java` |
| **Service** | `KeeperService.java` |
| **Controller** | `KeeperController.java` |

字段省略(17个字段) - 详见基线SQL
- **`offline_source_wsh`** tinyint DEFAULT 0：离线来源，0-店铺同步/系统，1-看护员主动离线（关店同步禁止覆盖主动离线）

---

### 9. pet_service_wsh (服务项目表)

| 属性 | 值 |
|------|-----|
| **表说明** | 商家提供的寄养服务项目 |
| **Entity** | `ServiceItem.java` (表名: pet_service_wsh) |
| **Mapper** | `ServiceItemMapper.java` |
| **Service** | `ServiceItemService.java` |
| **Controller** | `ServiceItemController.java` |

**注意**: Entity类名为`ServiceItem`，表名为`pet_service_wsh`

---

### 10. address_wsh (地址表)

| 属性 | 值 |
|------|-----|
| **表说明** | 用户收货地址管理 |
| **自增起始** | 105 |
| **字段数** | 13 |
| **Entity** | `Address.java` |
| **Mapper** | `AddressMapper.java` |
| **Service** | `AddressService.java` |
| **Controller** | `AddressController.java` |

---

### 11. business_hours_wsh (营业时间表)

| 属性 | 值 |
|------|-----|
| **表说明** | 商家每周营业时间配置 |
| **自增起始** | 122 |
| **Entity** | `BusinessHours.java` |
| **Mapper** | `BusinessHoursMapper.java` |
| **Service** | `BusinessHoursService.java` |
| **Controller** | `BusinessHoursController.java` |

**唯一索引**: `uk_merchant_day` (merchant_id_wsh, day_of_week_wsh)

---

## 模块: pet-business/order (订单模块) — 4张表

---

### 12. pet_order_wsh (订单表)

| 属性 | 值 |
|------|-----|
| **表说明** | 寄养服务订单主表，包含完整的地理位置追踪字段 |
| **字段数** | 48 (最大表) |
| **业务模块** | pet-business → order |
| **自增起始** | 133 |
| **Entity** | `PetOrder.java` |
| **Mapper** | `OrderMapper.java` |
| **Service** | `OrderService.java` |
| **Controller** | `OrderController.java` |

**唯一索引**: `order_no_wsh` (订单号)

**⚠️ Entity缺少映射字段**: `emergency_contact_relation_wsh`

---

### 13~15. payment_wsh / refund_wsh / tip_wsh

三张关联订单的支付/退款/打赏表，结构清晰，字段完整。

---

## 模块: pet-business/customer (客服模块) — 5张表

### 16~20. chat_message_wsh / complaint_wsh / rating_wsh / ticket_wsh / ticket_message_wsh

完整的客服体系表结构。

---

## 模块: pet-business/finance (财务模块) — 3张表

### 21~23. wallet_wsh / wallet_transaction_wsh / withdrawal_wsh

**⚠️ [P0] wallet_wsh缺少version_wsh字段**: Entity中定义了@Version乐观锁，但数据库无此列。

---

## 模块: pet-business/operation (运营模块) — 7张表

### 24~30. notice_wsh / notice_read_wsh / notification_wsh / operation_log_wsh / file_record_wsh / favorite_wsh / content_review_wsh

完整的运营支撑表体系。

---

## 模块: pet-ai (AI模块) — 3张表

### 33~35. ai_report_wsh / knowledge_document_wsh / document_embedding_wsh

**外键**: `document_embedding_wsh.document_id_wsh` → `knowledge_document_wsh.id_wsh` (项目中唯一的外键)

---

## 全表交叉引用关系图

```
┌─────────────────────────────────────────────────────────────┐
│ pet-system                                                  │
│  user_wsh ──< user_role_wsh >── role_wsh                    │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ pet-business                                                │
│                                                              │
│  [宠物] pet_wsh ──< category_wsh                             │
│  [看护] merchant_wsh ──< keeper_wsh                          │
│         ├── pet_service_wsh                                 │
│         └── business_hours_wsh                              │
│  [地址] address_wsh                                          │
│  [订单] pet_order_wsh ──< payment_wsh                        │
│         ├── refund_wsh                                      │
│         ├── tip_wsh                                         │
│         └── care_record_wsh                                 │
│  [客服] ticket_wsh ──< ticket_message_wsh                   │
│         ├── chat_message_wsh                                │
│         ├── complaint_wsh                                   │
│         └── rating_wsh                                      │
│  [财务] wallet_wsh ──< wallet_transaction_wsh               │
│         └── withdrawal_wsh                                  │
│  [运营] notice_wsh ──< notice_read_wsh                     │
│         └── notification_wsh                                │
│         ├── operation_log_wsh                               │
│         ├── file_record_wsh                                 │
│         ├── favorite_wsh                                    │
│         └── content_review_wsh                              │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ pet-ai                                                      │
│  ai_report_wsh                                              │
│  knowledge_document_wsh ═══[FK]══> document_embedding_wsh  │
└─────────────────────────────────────────────────────────────┘
```
