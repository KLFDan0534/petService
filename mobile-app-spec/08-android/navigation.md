# Android Navigation（导航实现）

## 结构（对齐 03-pages/navigation-map.md）

```
NavHost
├── Auth Graph: login · register · forgot
├── Main Graph（BottomBar 5 Tab，各自保存状态）
│   ├── home · services · pets · orders · profile
├── Detail Graph: service/{id} · merchant/{id} · keeper/{id} · pet/{id}
│   ├── order/{id} · order_create · payment/{orderNo} · chat/{conversationId?}
│   ├── notifications · favorites · coupons · membership · wallet · recharge
│   └── aftersales（refunds/ratings/complaints/credit/revenue/anomaly）
├── Role Graphs（按角色装配 [E addDynamicRoutes 语义]）
│   ├── keeper: workspace · keeper_profile · keeper_apply
│   ├── merchant: dashboard · services · orders · keepers · hours · stats
│   ├── support: workbench · tickets/{id} · complaints/{id}
│   └── admin: dashboard · audits（各审核待办）
└── Error: forbidden · notfound · server_error
```

## 规则

- 登录成功 → `navigate(Main){popUpTo(Auth){inclusive}}`；支付成功 → replace order/{id}（防返回到支付页 [D]）
- 受保护路由统一包装 `RequireAuth/RequireRole` 节点；无权 → forbidden
- Tab 状态保留：`saveState/restoreState`（对齐 Web keepAlive [E]）
- 深链 [R]：`qiyu://order/{id}` 等 + App Links；未登录 → 登录后续跳（对齐 redirect 语义 [E]）
- 返回：System Back 默认；Dialog/Sheet 在 Compose 内自处理（优先级高于页面返回 [P]）
