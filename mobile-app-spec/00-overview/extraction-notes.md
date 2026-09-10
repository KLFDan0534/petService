# Extraction Notes（提取方法与证据说明）

## 扫描范围（2026-09 执行）

| 层 | 已读内容 |
|---|---|
| 设计语言 | `设计语言/`（index.css 审计账本全量、tailwind.config.js、Button/Card 等 React 组件、Context.md）★Primary |
| 前端 | `design-tokens.css`、`app.css` 全量；`Services.vue`（视觉权威）样式全量；UserLayout/AppDialog/EmptyState/LoadingSpinner/StatusBadge/AppIcon/Reveal 源码；router/index.js 全量 783 行；stores(app/auth)；request.js；SocketManager.js；statusMaps.js 全量；permission.js |
| 后端 | 55 个 Controller 的类级 @RequestMapping + 全部方法级映射清单；pet-common 枚举清单；PROJECT_MAP.md（模块/队列/统计） |
| 文档 | PRODUCT.md、PROJECT_MAP.md、旧样式改写交接文档.md、frontend/docs/AppDialog-API.md |
| 统计 | 全项目正则频率统计：border-radius(25 种)/box-shadow(16 种)/font-size(20+ 种)/gap/padding |

## 本包与原项目的关系

- **只读提取**：未修改任何 `.vue/.java/.js/.sql` 业务代码。
- `mobile-design-language/`：设计语言提取产物（Primary Design Source）。
- `mobile-app-spec/`：本包 = 设计语言 + 产品 + API + 业务 + Android 工程 + AI 指令。

## 证据分布概况

| 等级 | 大致占比 | 说明 |
|---|---|---|
| EXISTING | ~70% | Token/组件数值、路由、角色、API 契约、状态机、认证流程 |
| DERIVED | ~15% | 页面分组、Tab 结构、列表模式、状态→UI 映射 |
| PLATFORM_ADAPTATION | ~10% | 底部导航/手势/Safe Area/系统组件映射 |
| RECOMMENDATION | ~5% | Android 技术栈、本地缓存策略、推送、离线 |

## 已知的提取局限

1. 数据库 55 表未逐表读取 DDL（`database/06-full-schema.sql` 路径在当前快照不存在）；实体关系按 Controller/枚举/前端字段归纳，见 `07-data/entities.md` 的置信标注。
2. ~151 个端点的 Request/Response 字段未逐一反编译 DTO；`06-api/data-models.md` 覆盖核心模型（Auth/Order/Pet/Service/Wallet），其余以"字段后缀 `_wsh` + 分页约定"推导并标注。
3. Controller 映射清单中部分路径的 HTTP 动词未逐一确认（`06-api/endpoints.md` 按语义标注 GET/POST，置信度中）。
4. 管理端 18+ 页面逐页文档合并为分组规范（page-020），因移动端范围裁剪。
