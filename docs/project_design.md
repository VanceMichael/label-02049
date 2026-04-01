# 演唱会抢票系统 - 项目设计文档

## 1. 系统架构

```mermaid
flowchart TD
    subgraph Client["客户端"]
        A[用户端 frontend-user :8081]
        B[管理后台 frontend-admin :8082]
    end

    subgraph Backend["后端服务 backend :8080"]
        C[Controller Layer]
        D[Service Layer]
        E[Mapper Layer]
        F[WebSocket Handler]
    end

    subgraph Middleware["中间件"]
        G[Redis - 库存/限流/缓存]
        H[RabbitMQ - 延迟队列/异步]
        I[MySQL 8.0 - 持久化]
    end

    A -->|HTTP/WS| C
    B -->|HTTP/WS| C
    C --> D
    D --> E
    D --> G
    D --> H
    E --> I
    F -->|WebSocket Push| A
    F -->|WebSocket Push| B
    H -->|库存释放/订单超时| D
```

## 2. ER 图

```mermaid
erDiagram
    USER {
        bigint id PK
        varchar username UK
        varchar password
        varchar email
        varchar phone
        varchar avatar
        tinyint role "0=user,1=admin"
        tinyint status "0=normal,1=disabled"
        int login_fail_count
        datetime lock_until
        datetime created_at
        datetime updated_at
    }

    CONCERT {
        bigint id PK
        varchar title
        text description
        varchar artist
        varchar venue
        varchar city
        datetime show_time
        varchar poster_url
        tinyint status "0=未开始,1=售票中,2=已结束,3=已下架"
        datetime created_at
        datetime updated_at
    }

    TICKET_TIER {
        bigint id PK
        bigint concert_id FK
        varchar tier_name "普通票/VIP票"
        decimal price
        int total_stock
        int available_stock
        datetime created_at
        datetime updated_at
    }

    ORDERS {
        bigint id PK
        varchar order_no UK
        bigint user_id FK
        bigint concert_id FK
        bigint ticket_tier_id FK
        int quantity
        decimal total_price
        tinyint status "0=待支付,1=已支付,2=已取消,3=已退款,4=已完成"
        datetime expire_at
        datetime created_at
        datetime updated_at
    }

    ORDER_LOG {
        bigint id PK
        bigint order_id FK
        varchar action
        varchar operator
        datetime created_at
    }

    NOTIFICATION {
        bigint id PK
        bigint user_id FK "NULL=全员通知"
        varchar type "SYSTEM/PERSONAL"
        varchar title
        text content
        tinyint is_read "0=未读,1=已读"
        datetime created_at
    }

    USER ||--o{ ORDERS : "下单"
    CONCERT ||--o{ TICKET_TIER : "包含票档"
    CONCERT ||--o{ ORDERS : "关联"
    TICKET_TIER ||--o{ ORDERS : "关联"
    ORDERS ||--o{ ORDER_LOG : "状态日志"
    USER ||--o{ NOTIFICATION : "接收通知"
```

## 3. 接口清单

### AuthController (`/api/auth`)
| Method | Path | Description |
|--------|------|-------------|
| POST | /register | 用户注册 |
| POST | /login | 用户登录 |

### UserController (`/api/user`)
| Method | Path | Description |
|--------|------|-------------|
| GET | /profile | 获取个人信息 |
| PUT | /profile | 修改个人信息 |
| GET | /list | 管理员-用户列表 |
| PUT | /{id}/status | 管理员-修改用户状态 |

### ConcertController (`/api/concert`)
| Method | Path | Description |
|--------|------|-------------|
| POST | / | 新增演唱会 |
| PUT | /{id} | 修改演唱会 |
| DELETE | /{id} | 删除演唱会 |
| GET | /{id} | 演唱会详情 |
| GET | /list | 演唱会列表(筛选/搜索) |

### TicketTierController (`/api/ticket-tier`)
| Method | Path | Description |
|--------|------|-------------|
| POST | / | 新增票档 |
| PUT | /{id} | 修改票档 |
| DELETE | /{id} | 删除票档 |
| GET | /concert/{concertId} | 查询演唱会票档 |

### GrabTicketController (`/api/grab`)
| Method | Path | Description |
|--------|------|-------------|
| POST | / | 抢票 |

### OrderController (`/api/order`)
| Method | Path | Description |
|--------|------|-------------|
| GET | /my | 我的订单 |
| GET | /list | 管理员-所有订单 |
| POST | /{id}/pay | 模拟支付 |
| POST | /{id}/cancel | 取消订单 |
| POST | /{id}/refund | 退款 |

### NotificationController (`/api/notification`)
| Method | Path | Description |
|--------|------|-------------|
| GET | /list | 通知列表 |
| GET | /unread-count | 未读数量 |
| PUT | /{id}/read | 标记已读 |
| PUT | /read-all | 全部已读 |
| POST | /publish | 管理员-发布公告 |

## 4. UI/UX 规范

- 主色调: `#1a1a2e` (深蓝黑), 强调色: `#e94560` (红), 辅助色: `#16213e`
- 字体: `'Segoe UI', 'PingFang SC', sans-serif`
- 卡片圆角: `12px`, 阴影: `0 4px 20px rgba(0,0,0,0.08)`
- 间距体系: `8px / 16px / 24px / 32px`
- 按钮圆角: `8px`, Hover 过渡: `0.3s ease`
- 成功色: `#27ae60`, 警告色: `#f39c12`, 错误色: `#e74c3c`
