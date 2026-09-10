# 全局字段命名统一重构文档

## 目标
将前后端传递的字段名统一为 `_wsh` 后缀，消除 `@JsonGetter`、`@JsonAlias` 等双重序列化机制。

## 影响范围

| 模块 | 实体/DTO文件数 | _wsh字段数 | @JsonGetter数 | @JsonAlias数 |
|------|--------------|-----------|--------------|-------------|
| pet-system | 3实体 + 17DTO + 6VO | ~26 | ~13 | ~20 |
| pet-business/boarding | 6实体 + 33DTO + 8VO | ~75 | ~34 | ~20 |
| pet-business/order | 4实体 + 17DTO | ~98 | ~32 | ~20 |
| pet-business/pet | 3实体 + 9DTO | ~26 | ~19 | ~25 |
| pet-business/customer | 6实体 + 24DTO | ~60 | ~16 | ~10 |
| pet-business/membership | 5实体 + 8DTO | ~69 | 0 | 0 |
| pet-business/finance | 3实体 + 7DTO | ~37 | 0 | 0 |
| pet-business/operation | 7实体 + 14DTO | ~55 | ~1 | 0 |
| pet-ai | 2实体 + 11DTO | ~16 | 0 | ~5 |
| pet-common | 1DTO | ~2 | 0 | ~2 |
| **合计** | **~150+文件** | **~397** | **~132** | **~130** |

## 后端修改计划

### Phase 1: 实体类清理（43个文件）
移除以下注解：
- `@JsonGetter("cleanName")` 方法 → 共~132个
- `@JsonAlias({"cleanName"})` 注解 → 共~130个
- `@JsonProperty("field_wsh")` 注解 → 字段名已匹配，可省略

### Phase 2: DTO/VO类清理（~150+文件）
移除：
- `@JsonAlias` 注解
- 清理不需要的字段映射

### Phase 3: 测试代码更新
更新所有测试文件中的字段引用。

## 前端修改计划

### Phase 1: 组件模板（.vue文件）
全局替换：
- `item.name` → `item.name_wsh`
- `item.status` → `item.status_wsh`
- `item.id` → `item.id_wsh`
- 等等

### Phase 2: JavaScript逻辑
更新所有字段引用。

### Phase 3: 测试文件
更新所有 `.spec.js` 文件中的字段断言。

## 风险评估
- **高风险**：PetOrder.java（72个字段，改动最大）
- **中风险**：前端全局替换可能遗漏
- **低风险**：DTO/VO类相对独立

## 验证策略
1. 后端单元测试：`mvn test`
2. 前端单元测试：`npm test`
3. 前端构建：`npm run build`
4. 手动API测试：验证关键接口返回格式

## 执行顺序
1. 先修改后端实体类（核心）
2. 再修改DTO/VO类
3. 更新测试代码
4. 修改前端代码
5. 全局验证
