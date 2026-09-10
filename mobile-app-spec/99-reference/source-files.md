# Source Files（来源文件清单）

> 本规格包的事实来源。置信等级：★=直接读取源码/全量 · ☆=读取摘要/文档。

## 设计语言
| 文件 | 内容 | 等级 |
|---|---|---|
| `设计语言/src/index.css` | `--ref-*` Token + 审计账本（confirmed 规则） | ★ |
| `设计语言/tailwind.config.js` | fontSize/spacing/shadow Token 映射 | ★ |
| `设计语言/src/components/Button/index.tsx` | Button 7 变体完整解剖 | ★ |
| `frontend/src/assets/css/design-tokens.css` | 双主题 Token 权威 | ★ |
| `frontend/src/assets/css/app.css` | 全局样式/控件/Toast/断点 | ★ |
| `frontend/src/views/user/Services.vue` | 视觉权威页（Hero/Chip/Card/搜索） | ★ |
| `frontend/旧样式改写交接文档.md` | 新旧 Token 迁移映射 | ☆ |
| `frontend/src/components/common/*.vue` | AppDialog/EmptyState/Loading/StatusBadge/AppIcon/Reveal | ★ |
| `mobile-design-language/`（本仓库新增） | 设计语言提取产物 | ★ |

## 前端结构
| 文件 | 内容 | 等级 |
|---|---|---|
| `frontend/src/router/index.js` | 783 行：全路由 + 角色守卫 | ★ |
| `frontend/src/utils/request.js` | axios 拦截器/刷新/错误处理 | ★ |
| `frontend/src/utils/SocketManager.js` | SSE/轮询/退避 | ★（前 80 行逐行） |
| `frontend/src/utils/permission.js` · `stores/auth.js` · `stores/app.js` | 角色/会话/主题/Toast | ★ |
| `frontend/src/constants/statusMaps.js` | 全部状态枚举映射 | ★ |
| `frontend/index.html` | 字体加载 | ★ |

## 后端
| 来源 | 内容 | 等级 |
|---|---|---|
| 55 个 `*Controller.java` | 类级/方法级路由清单 | ★（映射注解全量） |
| `pet-common/src/main/java` 文件清单 | 枚举/Result/异常 | ★（清单级） |
| `PROJECT_MAP.md` | 模块/依赖/队列/统计/架构要点 | ☆（编码部分乱码，取结构信息） |
| `PRODUCT.md` | 产品定位/原则/无障碍目标 | ★ |

## 数据库
`database/*.sql`（建库/测试/修复脚本清单）★清单级；逐表 DDL 未全量读取 → unresolved.md #1。
