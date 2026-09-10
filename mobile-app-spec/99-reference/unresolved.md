# Unresolved（未解决问题）

> 禁止编造答案；实现时按 implementation-rules.md 默认规则保守处理并标注 TODO(需确认)。

| # | 问题 | 影响面 | 当前处理 |
|---|---|---|---|
| 1 | 55 张表逐表 DDL 未确认（schema SQL 在本快照不可读） | data-models 字段完整性 | 按前端消费字段建模；联调修正 |
| 2 | 各端点 HTTP 动词与部分 query/body 字段名未逐一反编译 DTO | ApiService 定义 | 按语义标注，联调校正 |
| 3 | 分页响应字段确切命名（records/total vs 其他） | 分页通用层 | common.md 双预案 |
| 4 | availability / quote 的确切请求响应结构 | 下单页 | 联调确定 |
| 5 | 是否支持多设备同时登录（refresh 是否单点失效） | 认证体验 | 默认支持；401 链路兜底 |
| 6 | 支付渠道：Web 仅钱包余额；移动端是否接第三方支付 | 支付页 | M3 先钱包；渠道单列任务 |
| 7 | 后端是否提供推送网关（FCM/APNs 无证据） | 后台通知 | 仅 App 内 SSE+轮询；推送后置 |
| 8 | 深链/App Links 后端是否配合（域名校验文件） | 深链 | 先 scheme；App Links 待定 |
| 9 | ADMIN 移动端裁剪边界（哪些表格页必须移动化） | page-020 | 按待办型操作优先；其余保留 Web |
| 10 | CS 是否需要移动接单提醒（响铃/强提醒） | page-021 | 默认仅通知红点 |
| 11 | 离线模式范围（是否允许缓存只读浏览） | local-storage | 默认仅缓存已见数据 |
| 12 | `isTokenExpired` 的 exp 时钟偏差容忍值 | 启动校验 | 默认 30s 提前刷新 |
| 13 | 文件上传字段名与大小限制确切值 | upload.md | 默认 `file`、10MB，联调校正 |
| 14 | 评价提交端点归属（OrderController vs Rating 独立提交） | page-007/017 | 联调确认 |
