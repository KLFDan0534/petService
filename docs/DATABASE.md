# 数据库设计文档

## 表结构

### user (用户表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 密码(加密) |
| nickname | VARCHAR(50) | 昵称 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(500) | 头像URL |
| email | VARCHAR(100) | 邮箱 |
| address | VARCHAR(255) | 地址 |
| latitude | DECIMAL(10,6) | 纬度 |
| longitude | DECIMAL(10,6) | 经度 |
| status | TINYINT | 状态 0禁用 1启用 |
| deleted | TINYINT | 逻辑删除 |

### role (角色表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(50) | 角色名称 |
| code | VARCHAR(50) | 角色编码 |
| description | VARCHAR(255) | 描述 |

### user_role (用户角色关联表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| role_id | BIGINT | 角色ID |

### pet (宠物表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| owner_id | BIGINT | 主人ID |
| name | VARCHAR(50) | 宠物名 |
| type | VARCHAR(50) | 类型(dog/cat/other) |
| breed | VARCHAR(100) | 品种 |
| age | INT | 年龄 |
| weight | DECIMAL(10,2) | 体重 |
| gender | TINYINT | 性别 |
| sterilized | TINYINT | 是否绝育 |
| vaccinated | TINYINT | 是否免疫 |
| allergies | TEXT | 过敏信息 |
| habits | TEXT | 习惯 |

### merchant (商家表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| name | VARCHAR(100) | 商家名 |
| address | VARCHAR(255) | 地址 |
| latitude | DECIMAL(10,6) | 纬度 |
| longitude | DECIMAL(10,6) | 经度 |
| rating | DECIMAL(3,2) | 评分 |
| status | TINYINT | 状态 |

### keeper (寄养员表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| merchant_id | BIGINT | 商家ID |
| user_id | BIGINT | 用户ID |
| name | VARCHAR(50) | 姓名 |
| rating | DECIMAL(3,2) | 评分 |
| completion_rate | DECIMAL(5,2) | 完成率 |
| complaint_rate | DECIMAL(5,2) | 投诉率 |
| price_per_day | DECIMAL(10,2) | 每日价格 |
| max_pets | INT | 最大接单数 |

### pet_order (订单表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| order_no | VARCHAR(50) | 订单号 |
| owner_id | BIGINT | 主人ID |
| pet_id | BIGINT | 宠物ID |
| keeper_id | BIGINT | 寄养员ID |
| merchant_id | BIGINT | 商家ID |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期 |
| days | INT | 天数 |
| price_per_day | DECIMAL(10,2) | 每日价格 |
| total_amount | DECIMAL(10,2) | 总金额 |
| discount | DECIMAL(10,2) | 折扣 |
| final_amount | DECIMAL(10,2) | 实付金额 |
| status | VARCHAR(20) | 状态 |

### payment (支付表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| order_no | VARCHAR(50) | 订单号 |
| pay_no | VARCHAR(100) | 支付号 |
| amount | DECIMAL(10,2) | 金额 |
| method | VARCHAR(20) | 支付方式 |
| status | VARCHAR(20) | 状态 |

### refund (退款表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| order_no | VARCHAR(50) | 订单号 |
| amount | DECIMAL(10,2) | 金额 |
| reason | TEXT | 原因 |
| status | VARCHAR(20) | 状态 |

### complaint (投诉表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| order_id | BIGINT | 订单ID |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 内容 |
| status | VARCHAR(20) | 状态 |

### knowledge_document (知识库文档表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 内容 |
| category | VARCHAR(50) | 分类 |
| source_type | VARCHAR(20) | 来源类型 |
| word_count | INT | 字数 |

## 索引设计
- user: uk_username (username)
- order: uk_order_no (order_no)
- payment: idx_order_no (order_no)
- favorite: uk_user_target (user_id, target_id, target_type)
