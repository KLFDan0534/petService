# Inputs & Forms（输入与表单规范）

## Input（EXISTING）

```
Field: h44 · radius 11 · border 1px line · bg surface · padding 0 14 · 字号 13.5
Search: 左图标 15px textTertiary(left 14) · 右清除 28px tile(right 8) · padding 0 40
focus: border ink35% + 3px primary12% 环（200ms）
disabled: bg surfaceVariant · 内容 75% · 不可点
error: border error + error 12% 洗色环 + 12px error 文案
```

变体：text / search / password（可见性切换图标）/ textarea（min 88px，顶部对齐）/ number（tabular-nums）。

## 表单布局（EXISTING form-group/form-row）

- 字段组：label 13 w600 · mb 6 → field 44 → helper/error 12（gap 4）
- 组间距 16；移动端单列（Web 768 断点行为 [E]）
- 必填：label 尾随 `*`（error 色）[R]
- 键盘：`adjustResize`/iOS 下推；数字键盘（金额/手机号）；下一个按钮顺序 [P]

## 校验规则（DERIVED from 页面）

| 字段 | 规则 |
|---|---|
| 用户名/手机号/邮箱 | 注册唯一性由后端校验；前端非空+格式 |
| 密码/支付密码 | 长度+二次输入一致（注册/改密页） |
| 订单日期 | 起止校验、营业时段校验（ServiceDetail availability） |
| 金额 | >0、两位小数、不超过可用余额 |
| 评价内容/日报 | 非空；图片数上限由上传接口约束 |

## 选择类（PLATFORM_ADAPTATION）

- Select/DatePicker：底部弹层（BottomSheet）内列表/日历，44px 行高，选中=wash；
- 地址：AddressPicker 地图选点（Web 用高德 AmapAddressPicker [E] → 移动端高德 Android SDK [P]）；
- 日期范围（下单）：双月历 BottomSheet，禁用非营业日（GET availability）。

## 提交规则（DERIVED）

提交中按钮 loading 禁点；失败 Toast error；成功 Snackbar + 返回/跳转；未保存离开需确认（Dialog）[R]。
