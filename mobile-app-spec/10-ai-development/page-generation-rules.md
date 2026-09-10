# Page Generation Rules（页面生成规则）

> AI 创建任何新页面时，按以下 14 步执行：

1. **判断模块**：归属业务域（见 00-overview/project-overview.md 子域表）
2. **查页面规范**：03-pages/pages/ 是否已有该页/相近页 → 有则严格按规范
3. **查组件规范**：02-components/ 选取组件；缺组件先按规范新建到 designsystem
4. **查 Token**：所有视觉值来自 01-design-system/tokens.md
5. **查 API**：06-api/endpoints.md + data-models.md；缺失端点 → unresolved 记录，禁止编造
6. **查权限**：04-user-roles/ 确定角色门槛与操作白名单
7. **实现 UI**：布局 = 页面规范的 Layout/Sections；单列移动结构
8. **实现 State**：UiState 五态 + 提交态（08-android/state-management.md）
9. **实现 API**：ViewModel → UseCase(可选) → Repository → ApiService
10. **实现 Error**：errors.md 决策树；message 透传
11. **实现 Loading**：骨架屏/按钮 spinner（区分首载/操作/追加）
12. **实现 Empty**：EmptyState 文案 + 引导动作
13. **实现 Accessibility**：热区/语义标签/动态字体/对比度
14. **验证视觉一致性**：对照组件规范表逐列核对（02-components）

## 附加约束

- 新路由必须登记到导航图（08-android/navigation.md）并声明 meta 角色
- 新增跨页组件必须更新 02-components/component-index.md
- 每页完成即跑 verification-checklist.md
