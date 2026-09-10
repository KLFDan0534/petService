# page-005 · 创建订单（CreateOrder）

- **Purpose**：Web `CreateOrderDialog` 弹窗 → 移动端全屏流程页 [PLATFORM_ADAPTATION]
- **Role**：OWNER（用户区角色）
- **Entry**：ServiceDetail「立即预约」；**Exit**：支付页 / 返回

## Layout（分段表单，单列）

```
App Bar：返回 + 标题「创建订单」
1 服务摘要卡（服务名/商家/单价/时段）
2 预约日期：日期范围选择（BottomSheet 月历；禁用非营业日；展示 availability [E]）
3 选择宠物：PetCard 单选列表（无宠物 → 引导建档）
4 服务地址：AddressPicker（地图选点/地址簿 [E AmapAddressPicker→移动地图页]）
5 备注：textarea
6 优惠券：可选券列表（BottomSheet；quote 实时计算）
7 费用明细：单价×数量 + 券抵扣 + 合计（stat 衬线 tabular）
底部操作栏：合计 + 「提交订单」primary lg（Submitting→spinner）
```

## API（EXISTING）

券报价 `POST /api/coupons/quote` · 创建 `POST /api/orders/batch`（多宠物可批量 [E]）· 时段 `GET /api/services/{serviceId}/availability` · 会员权益抵扣 `POST /api/membership/benefits/order/quote`。

## 校验

日期必填且合法；宠物 ≥1；地址必填；金额 >0。提交成功 → 订单 `pending` → 跳支付页（page-008）。

## States

Submitting 全局禁点；失败 Snackbar（业务错误文案来自 `message`）；券过期/库存失败 → 刷新 quote 重试。
