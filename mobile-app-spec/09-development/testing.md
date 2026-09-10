# Testing（测试要求）

## 分层测试（RECOMMENDATION）

| 层 | 工具 | 覆盖 |
|---|---|---|
| Unit | JUnit+MockK+Turbine | ViewModel 状态机、Repository 解包/错误映射、枚举 fallback、支付金额计算 |
| 网络层 | MockWebServer | Result 解包、401 刷新重放（并发）、403 循环防护、超时 |
| Compose UI | createComposeRule | 组件四态（Button/Chip/Input/Card/Badge）、Token 断言 |
| 导航 | Navigation 测试 | 守卫：未登录/角色/深链回跳 |
| E2E | Espresso / 内部冒烟 | 见下 |

## E2E 关键路径（必须全绿）

1. 注册→登录→登出
2. 浏览服务→详情→下单（券报价）→钱包支付→订单详情
3. 寄养师：check-in→接单→start(传图)→日报→complete
4. 订单取消 / 退款申请→（管理）approve→complete
5. IM 发消息+收 SSE 实时；通知红点→已读
6. 暗色切换全页面走查；断网→离线横幅→恢复重试

## QA 页面清单（每页必查）

四态齐全 · 触控热区 ≥48dp · 空态文案 · 错误 message 透传 · 返回保留列表状态 · 动态字体 130% · 无 TalkBack 阻塞

## 验收数据

- 冷启 <2s；列表滚动 60fps；SSE 重连成功率；Crash-free ≥99.5% [R]
