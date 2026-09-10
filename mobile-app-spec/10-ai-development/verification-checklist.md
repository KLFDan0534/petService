# Verification Checklist（页面级验证清单）

> 每个页面完成后逐项勾选；任何一项不过不得视为完成。

## 视觉
- [ ] 颜色全部来自 Token（抽查无裸色值）
- [ ] 字号/字重/行高 = typography.md 对应级
- [ ] 间距/圆角 = spacing/radius Token
- [ ] 卡片静置无阴影（1px line）；按钮 42 / Chip·输入 44
- [ ] 暗色模式走查无对比度问题

## 状态
- [ ] Loading（骨架/spinner）· Empty · Error(可重试) · Success 四态
- [ ] 提交类按钮有 Submitting（防重复点击）
- [ ] 分页页脚「加载中/没有更多了」

## 数据与权限
- [ ] 端点与 endpoints.md 一致；DTO 字段 `_wsh`
- [ ] code≠200 → message 透传展示
- [ ] 401 刷新链路无死循环；403 → 「权限不足」
- [ ] 页面角色限制 = role-matrix；操作按钮 = 状态机白名单

## 交互
- [ ] 热区 ≥48dp；按压态（加深+scale.98）
- [ ] 返回行为正确（列表状态保留/replace 场景）
- [ ] 键盘不遮挡输入；日期/金额键盘正确
- [ ] TalkBack 全元素可理解；图片有语义说明

## 动效
- [ ] 弹窗 scale(.98→1)+scrim；无夸张动效
- [ ] reduce-motion 下降级

## 记录
- [ ] 偏离规格处已写入 99-reference/conflicts.md
- [ ] 未决问题已写入 99-reference/unresolved.md
