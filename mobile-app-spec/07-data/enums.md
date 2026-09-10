# Local Enums & Status Maps（App 端枚举落地）

> 与 06-api/enums.md 同源；此文件规定 App 内代码组织。

## Kotlin 示例（RECOMMENDATION）

```kotlin
enum class OrderStatus(val label: String, val badge: Badge) {
    PENDING("待付款", Badge.WARNING), PAID("已支付", Badge.INFO),
    CONFIRMED("待送达", Badge.INFO), DELIVERED("已送达", Badge.PRIMARY),
    RECEIVED("已接收", Badge.PRIMARY), IN_PROGRESS("服务中", Badge.INFO),
    COMPLETED("已完成", Badge.SUCCESS), CANCELLED("已取消", Badge.DANGER),
    REFUNDING("退款中", Badge.WARNING), REFUNDED("已退款", Badge.SUCCESS);
    companion object { fun from(raw: String?) = entries.firstOrNull { it.name.equals(raw, true) } }
}
```

- 反序列化容忍：未知值 → `UNKNOWN(label=原值, badge=INFO)`（对齐 Web fallback [E]）
- 数值态（0/1/2）用 `sealed`/常量表映射（MerchantStatus 等）
- Badge → 颜色映射走 Design Token（01-design-system/tokens.md 状态 wash 配方）

## 前端合成字段替代

Web 的 `status_label_wsh` 是前端合成 [E] → App 一律本地映射，不依赖后端合成字段。
