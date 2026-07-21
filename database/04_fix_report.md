# 数据库修复建议报告

> 生成日期: 2026-06-28
> 严重程度: P0-阻塞 | P1-严重 | P2-一般 | P3-建议

---

## 一、P0 - 阻塞级问题

### [P0-001] Wallet实体乐观锁字段缺失
- **位置**: `Wallet.java` → `wallet_wsh`表
- **问题**: Entity中定义了`@Version private Integer versionWsh;`，但数据库中`wallet_wsh`表没有`version_wsh`列
- **影响**: 乐观锁不生效，并发更新钱包可能产生数据不一致
- **建议修复**: 在`wallet_wsh`表添加`version_wsh`列

```sql
ALTER TABLE `wallet_wsh`
  ADD COLUMN `version_wsh` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号' AFTER `frozen_amount_wsh`;
```

---

## 二、P1 - 严重级问题

### [P1-001] 字符集排序规则不一致
- **位置**: 34张数据库表
- **问题**: 数据库实际排序规则为`utf8mb4_0900_ai_ci`，但SQL脚本(SQL脚本)定义为`utf8mb4_unicode_ci`
- **影响**: DDL管理混乱，不同环境可能出现字符集不一致
- **建议**: 统一使用`utf8mb4_0900_ai_ci`(MySQL 8.0默认)

### [P1-002] merchant_wsh缺少avatar字段映射
- **位置**: `Merchant.java` Entity
- **问题**: 数据库`merchant_wsh`有`avatar_wsh`列(商家头像)，但Entity中无对应字段
- **影响**: 商家头像无法通过MyBatis-Plus自动映射，需要手动处理
- **建议**: 在`Merchant.java`中添加`avatar_wsh`字段

### [P1-003] pet_order_wsh缺少emergency_contact_relation字段映射
- **位置**: `PetOrder.java` Entity
- **问题**: 数据库`pet_order_wsh`有`emergency_contact_relation_wsh`列，但Entity中无对应字段
- **影响**: 紧急联系人关系数据无法通过自动映射读取
- **建议**: 在`PetOrder.java`中添加`emergency_contact_relation_wsh`字段

### [P1-004] user_role_wsh.deleted_wsh类型不一致
- **位置**: `user_role_wsh`表
- **问题**: `deleted_wsh`字段类型为`int`，其他所有表的`deleted_wsh`均为`tinyint`
- **影响**: 类型不一致，建议统一
- **建议**: 
```sql
ALTER TABLE `user_role_wsh`
  MODIFY COLUMN `deleted_wsh` tinyint DEFAULT '0' COMMENT '逻辑删除';
```

---

## 三、P2 - 一般级问题

### [P2-001] 缺少updated_at_wsh字段的表
- **位置**: 16张表
- **问题**: `ai_report_wsh`, `care_record_wsh`, `chat_message_wsh`, `complaint_wsh`, `content_review_wsh`, `favorite_wsh`, `notification_wsh`, `operation_log_wsh`, `rating_wsh`, `ticket_message_wsh`, `tip_wsh`, `wallet_transaction_wsh`, `role_wsh`, `user_role_wsh`, `notice_read_wsh`, `document_embedding_wsh` 缺少`updated_at_wsh`字段
- **影响**: 无法自动追踪记录更新时间
- **建议**: 对有业务意义的表添加`updated_at_wsh`字段

### [P2-002] OperationLog/FileRecord未启用逻辑删除
- **位置**: `OperationLog.java`, `FileRecord.java`
- **问题**: 数据库有`deleted_wsh`字段，但Entity未标注`@TableLogic`
- **影响**: MyBatis-Plus自动SQL不会带deleted条件

### [P2-003] Wallet实体使用camelCase命名
- **位置**: `Wallet.java`
- **问题**: `Wallet`实体使用`idWsh`/`userIdWsh`等camelCase命名，其他所有Entity使用`id_wsh`/`user_id_wsh`等snake_case
- **影响**: 编码风格不一致，但通过`@TableField(value="...")`正确映射，不影响运行

---

## 四、P3 - 建议级问题

### [P3-001] 代码中的@Deprecated类
- **位置**: `LoginRequest.java`, `RegisterRequest.java`, `CreateOrderRequest.java`, `LoginResponse.java`, `LoginResponseDTO.java`
- **问题**: 这些类标记为`@Deprecated`，应统一清理
- **建议**: 在下次重构时删除这些废弃类

### [P3-002] 空DTO文件
- **位置**: `TicketMessageDTO.java`, `TicketDTO.java`
- **问题**: 两个DTO文件为0字节空文件
- **建议**: 删除或补充内容

### [P3-003] 排序规则差异
- **位置**: `notice_read_wsh`表
- **问题**: 此表使用`utf8mb4_unicode_ci`，其他表使用`utf8mb4_0900_ai_ci`
- **建议**: 统一字符集排序规则

### [P3-004] adoption_pet_wsh.gender_wsh类型
- **位置**: `adoption_pet_wsh`表
- **问题**: gender字段为`varchar(10)`，而`pet_wsh`的gender为`tinyint`
- **建议**: 统一性别字段类型

---

## 五、问题汇总

| 严重级别 | 数量 | 说明 |
|---------|------|------|
| P0-阻塞 | 1 | Wallet乐观锁字段缺失 |
| P1-严重 | 4 | 字符集不一致、缺少字段映射 |
| P2-一般 | 3 | 缺少updated_at、逻辑删除未启用、命名不一致 |
| P3-建议 | 4 | 废弃类清理、空文件、排序规则差异 |
| **总计** | **12** | |

---

## 六、异常模式总结

### 6.1 数据库 < Entity 不匹配
| 模式 | 计数 | 说明 |
|------|------|------|
| Entity有字段但数据库无 | 1 | Wallet.versionWsh |
| 数据库有字段但Entity无 | 2 | merchant.avatar, pet_order.emergency_contact_relation |

### 6.2 类型不一致
| 模式 | 计数 | 说明 |
|------|------|------|
| int vs tinyint | 1 | user_role_wsh.deleted_wsh |
| varchar vs tinyint | 1 | adoption_pet_wsh.gender_wsh |

### 6.3 排序规则不一致
| 模式 | 计数 | 说明 |
|------|------|------|
| 表间不一致 | 1 | notice_read_wsh使用unicode_ci，其他用0900_ai_ci |
| 脚本与实际不一致 | 34 | SQL脚本指定unicode_ci，实际为0900_ai_ci |
