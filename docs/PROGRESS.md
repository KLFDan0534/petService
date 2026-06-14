# Pet Service Platform - 项目进度

## 已完成模块
- [x] 01 用户模块 (User)
- [x] 02 RBAC权限模块 (Role/Permission)
- [x] 03 宠物模块 (Pet)
- [x] 04 商家模块 (Merchant)
- [x] 05 寄养员模块 (Keeper)
- [x] 06 服务模块 (ServiceItem)
- [x] 07 收藏模块 (Favorite)
- [x] 08 订单模块 (Order)
- [x] 09 支付模块 (Payment)
- [x] 10 退款模块 (Refund)
- [x] 11 投诉模块 (Complaint)
- [x] 12 聊天模块 (Chat)
- [x] 13 MinIO模块 (File Storage)
- [x] 14 RabbitMQ模块 (Message Queue)
- [x] 15 数据统计模块 (Statistics)
- [x] 16 RAG知识库 (Knowledge Base)
- [x] 17 AI照护建议 (AI Care)
- [x] 18 AI寄养报告 (AI Report)
- [x] 19 Agent自动下单 (AI Agent)
- [x] 20 全链路测试 (Full Link Test)

## 测试结果
- **FullLinkTest**: 19/19 全部通过
- **AuthControllerTest**: 3/3 全部通过
- **PetApplicationTests**: 1/1 全部通过
- **总计**: 23/23 测试全部通过
- **测试环境**: H2内存数据库 (MODE=MYSQL, NON_KEYWORDS=USER)
- **覆盖**: 注册 → 商家 → 寄养员 → 宠物 → 下单 → 支付 → 完成 → 评价 → 退款 → RAG → Agent → 登录 → 搜索 → 预算下单 → AI报告 → 收藏 → 聊天 → 投诉

## RAG向量搜索升级
- **嵌入模型**: 基于TF-IDF + 哈希特征向量的轻量级嵌入服务
- **向量维度**: 256维
- **相似度算法**: 余弦相似度
- **自动索引**: 创建文档时自动生成并存储嵌入向量
- **降级策略**: 向量搜索无结果时自动降级到关键词匹配
- **存储**: `document_embedding` 表持久化嵌入向量
- **无需外部服务**: 完全自包含，无需OpenAI API Key或外部向量数据库

## 基础设施 (Docker Compose)
- **MySQL 8.0**: 主数据库
- **Redis 7**: 缓存/令牌存储
- **RabbitMQ 3.13**: 消息队列 + 管理面板
- **MinIO**: 对象存储 (文件/图片/视频)
- **Chroma**: 向量数据库 (可选，用于生产环境)

## 修复记录
1. **H2保留字冲突**: `user`表名在H2中为保留字，添加`NON_KEYWORDS=USER`解决
2. **中文分词问题**: RAG搜索对中文单字符分词，确保"寄养需要什么疫苗"匹配疫苗文档
3. **编码问题**: 添加`project.build.sourceEncoding=UTF-8` + `spring.datasource.sql-script-encoding=UTF-8`解决Windows GBK环境下中文乱码
4. **测试配置**: 所有`@SpringBootTest`类统一添加`@Import(TestConfig.class)`确保Mock Bean注入

## Swagger API文档
- **工具**: Springdoc OpenAPI 2.5.0
- **端点**: `/api-docs` (JSON), `/swagger-ui.html` (UI)
- **覆盖**: 所有Controller添加`@Tag` + `@Operation`中文注释
- **模块**: 17个Controller全部文档化

## Git提交
- 20个模块化提交，按功能拆分
- 最新: `feat: split AI report into dedicated ai module with controller; fix test encoding; expand tests to 23`

## 技术栈
- Spring Boot 3.2.5 / JDK 17
- Spring Security + JWT (无状态)
- MyBatis-Plus 3.5.7
- MySQL (生产) + H2 (测试)
- RabbitMQ + MinIO
- Spring AI 1.0.0-M2
- Swagger/OpenAPI (springdoc 2.5.0)
- Docker Compose (MySQL/Redis/RabbitMQ/MinIO/Chroma)

## 项目结构
```
pet-service/
├── docker-compose.yml    # 基础设施一键启动
├── src/main/java/com/pet/
│   ├── common/           # 通用工具类
│   ├── config/           # 全局配置
│   ├── security/         # 安全配置
│   ├── mq/               # 消息队列 (RabbitMQ)
│   └── module/           # 业务模块 (17个模块)
│       ├── user/         # 用户 + RBAC
│       ├── pet/          # 宠物
│       ├── merchant/     # 商家
│       ├── keeper/       # 寄养员
│       ├── service/      # 服务项目
│       ├── favorite/     # 收藏
│       ├── order/        # 订单
│       ├── payment/      # 支付
│       ├── refund/       # 退款
│       ├── complaint/    # 投诉
│       ├── chat/         # 聊天
│       ├── minio/        # 文件存储
│       ├── rabbitmq/     # RabbitMQ配置
│       ├── statistics/   # 数据统计
│       ├── rag/          # RAG知识库 (向量搜索)
│       ├── ai/           # AI报告 (照护/寄养)
│       └── agent/        # AI Agent (自动下单)
├── src/test/
│   ├── java/com/pet/     # FullLinkTest (19), AuthControllerTest (3), PetApplicationTests (1)
│   └── resources/        # H2 schema + test config
└── docs/                 # DATABASE.md + PROGRESS.md
```
