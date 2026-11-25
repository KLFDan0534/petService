# V20261119_001 执行记录：加宽 wallet_transaction_wsh.type_wsh（20 → 32）

- **执行时间**：2026-11-19 20:18（GMT+8）
- **目标库**：`pet_service` @ `127.0.0.1:3308`（容器 `pet-mysql`，MySQL 8.0.46，本地开发库）
- **触发问题**：`OrderTimeoutScheduler` 每分钟报
  `Data truncation: Data too long for column 'type_wsh'`，导致退款事务整体回滚。

## 根因

调用链：

```
OrderTimeoutScheduler.acceptTimeoutPaidOrders
 → OrderServiceImpl.autoAcceptPaidOrdersIfTimeout
 → cancelPaidOrderForInvalidKeeper(order)
 → refundPaidOrder(order, "keeper_invalid", ...)
 → accountingService.debit(systemUserId, subsidy, reason + "_subsidy", ...)
```

`reason="keeper_invalid"` → `type_wsh = "keeper_invalid_subsidy"`，**22 字符**，
而列定义为 `VARCHAR(20)`，严格模式下抛 `Data too long`。

同时发现另一个**尚未触发但同样会炸**的取值：`RefundServiceImpl:288` 的
`"coupon_subsidy_refund"` = **21 字符**，也在原列宽之外（退款回冲补贴路径）。

## 代码中 type_wsh 的全量取值

| 值 | 长度 | 来源 |
|---|---|---|
| `keeper_invalid_subsidy` | 22 | OrderServiceImpl:1092（本次故障） |
| `coupon_subsidy_refund` | 21 | RefundServiceImpl:288 |
| `unassigned_subsidy` | 19 | OrderServiceImpl:1092 |
| `membership_payment` | 18 | MembershipOrderServiceImpl:734/736 |
| `coupon_subsidy` | 14 | PaymentServiceImpl:314 |
| `keeper_invalid` / `reject_subsidy` | 14 | OrderServiceImpl:1084/1092 |
| `admin_adjust` | 12 | WalletController:150/156 |
| `unassigned` | 10 | OrderServiceImpl:1084 |
| `recharge` | 8 / `withdraw` 8 / `payment` 7 / `expense` 7 / `income` 6 / `reject` 6 / `tip` 3 | 各处 |

最长 22，故取 `VARCHAR(32)`（留 10 字符余量，仍可建索引）。

## 执行内容

```sql
ALTER TABLE wallet_transaction_wsh
  MODIFY COLUMN type_wsh VARCHAR(32) DEFAULT 'income'
  COMMENT '流水类型: income/expense/withdraw/refund/payment/recharge/tip/membership_payment/admin_adjust/coupon_subsidy*/{reason}_subsidy';
```

同步更新：`pet-admin/src/main/resources/db/schema.sql`、
`pet-admin/src/main/resources/h2-schema.sql`、`docs/database/complete_schema.sql`。

## 验证

事务内插入一条 `type_wsh='keeper_invalid_subsidy'`（22 字符）后回滚：

```
type_wsh                len
keeper_invalid_subsidy   22      ← 写入成功
leftover = 0                     ← 已回滚，未污染数据
```

修复后下一次调度（20:18:12）不再出现 `Data too long`。

## 回滚方式

`mysql -D pet_service < V20261119_001__rollback.sql`
（先校验 `MAX(CHAR_LENGTH(type_wsh)) <= 20` 才收窄，否则跳过）

## 遗留问题（不在本迁移范围）

1. **系统钱包余额不足**：修完列宽后，同一调度批次改为报
   `BusinessException: 可用余额不足`。原因是 69 笔 `paid` 订单合计需退 23183.00，
   而 `user_id_wsh=0` 的系统钱包只有 240.00（历史演示单从未走过钱包入账，只有 3 条 payment 流水）。
2. **整批一个事务**：`autoAcceptPaidOrdersIfTimeout` 把整批订单放在同一个事务里，
   只要有一笔失败，前面已成功的退款也会一起回滚（日志里 DEMO00402 显示"已退款"但库中仍是 paid）。
   建议改为逐单独立事务（`REQUIRES_NEW` 或循环内 try-catch），否则一条脏数据会永久拖死兜底任务。
