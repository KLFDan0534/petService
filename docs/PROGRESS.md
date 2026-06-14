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

## 技术栈
- Spring Boot 3.2.5
- Spring Security + JWT + Redis
- MyBatis-Plus 3.5.6
- MySQL + H2 (Test)
- RabbitMQ
- MinIO
- Spring AI + LangChain4j
- Swagger/OpenAPI

## 项目结构
```
pet-service/
├── src/main/java/com/pet/
│   ├── common/          # 通用工具类
│   ├── config/          # 全局配置
│   ├── security/        # 安全配置
│   ├── mq/              # 消息队列
│   └── module/          # 业务模块
│       ├── user/
│       ├── pet/
│       ├── merchant/
│       ├── keeper/
│       ├── service/
│       ├── favorite/
│       ├── order/
│       ├── payment/
│       ├── refund/
│       ├── complaint/
│       ├── chat/
│       ├── minio/
│       ├── rabbitmq/
│       ├── statistics/
│       ├── rag/
│       └── agent/
```
