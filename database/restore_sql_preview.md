# 恢复SQL预览

## Binlog 回放SQL预览

以下是从 binlog 提取的数据样本，展示了即将恢复的数据内容。

### 用户数据 (user_wsh)

从 binlog.000012 提取：

```sql
INSERT INTO user_wsh (id_wsh, username_wsh, password_wsh, nickname_wsh, ...)
VALUES
  (113, 'testY',          '$2a$10$F4...', ...),
  (114, 'otest_1782403370', '$2a$10$wr...', ...),
  (115, 'otest_1782403421', '$2a$10$DR...', ...);
```

### 宠物数据 (pet_wsh)

```sql
INSERT INTO pet_wsh (id_wsh, owner_id_wsh, name_wsh, ...)
VALUES
  (108, 6,  '黄毛', ...),
  (112, 114, 'TestDog', ...),
  (113, 115, 'Dog', ...);
```

### 文件记录 (file_record_wsh)

```sql
INSERT INTO file_record_wsh (id_wsh, filename_wsh, path_wsh, ...)
VALUES
  (1005, '宠物领域.png', 'pets/caf86c65-...png', ...),
  (1006, '宠物领域.png', 'pets/dcf7cfec-...png', ...);
```

### 订单数据 (pet_order_wsh)

```sql
INSERT INTO pet_order_wsh (id_wsh, order_no_wsh, owner_id_wsh, ...)
VALUES
  (108, 'ORD20260624AB6344A4', 6, ...);
```

### 操作日志 (operation_log_wsh)

```sql
INSERT INTO operation_log_wsh (id_wsh, operator_id_wsh, ...)
VALUES
  (1015, 6, ...),
  (1072, 114, ...);
```

---

## 数据范围汇总

| 业务领域 | 涉及表 | 预计数据量 |
|----------|--------|-----------|
| 用户管理 | user_wsh, user_role_wsh, role_wsh | ~50行 |
| 宠物管理 | pet_wsh, category_wsh | ~30行 |
| 商家/看护 | merchant_wsh, keeper_wsh, business_hours_wsh | ~20行 |
| 服务 | pet_service_wsh | ~10行 |
| 订单 | pet_order_wsh, payment_wsh, refund_wsh | ~200行 |
| 评价 | rating_wsh, complaint_wsh | ~10行 |
| 钱包 | wallet_wsh, wallet_transaction_wsh, withdrawal_wsh | ~20行 |
| 通知 | notification_wsh, notice_wsh, notice_read_wsh | ~30行 |
| 聊天 | chat_message_wsh | ~10行 |
| 工单 | ticket_wsh, ticket_message_wsh | ~10行 |
| 文件 | file_record_wsh | ~50行 |
| 日志 | operation_log_wsh | ~300行 |
| 知识库 | knowledge_document_wsh, document_embedding_wsh | ~10行 |
| 看护记录 | care_record_wsh | ~10行 |
| 收养 | adoption_pet_wsh, adoption_application_wsh | ~10行 |
| 其他 | address_wsh, favorite_wsh, tip_wsh, ai_report_wsh, content_review_wsh | ~30行 |
| **总计** | **35张表** | **~800行** |

---

## 执行说明

实际执行时不会生成单独的 SQL 文件。恢复命令为：

```bash
mysqlbinlog --no-defaults --stop-datetime="2026-06-28 00:33:38" ^
  binlog.000001 binlog.000002 binlog.000003 binlog.000004 ^
  binlog.000005 binlog.000006 binlog.000007 binlog.000008 ^
  binlog.000009 binlog.000010 binlog.000011 binlog.000012 ^
  binlog.000013 binlog.000014 binlog.000015 ^
  | mysql -f -h localhost -P 3308 -u root -p1234 pet_service
```

参数说明：
- `--stop-datetime="2026-06-28 00:33:38"`：在 DROP TABLE 前停止
- `-f`：强制跳过 ALTER TABLE 等可忽略的错误
- 所有 binlog 文件位于 `database/` 目录下

**预计执行时间：5-10分钟**
**预计恢复数据：~800行，35张表**
