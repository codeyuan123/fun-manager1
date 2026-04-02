# 工程设计与数据库设计文档

## 1. 仓库结构

```text
fund-manager/
  docs/
    architecture.md
    engineering.md
    deployment.md
  scripts/
    remote_exec.py
    remote_upload.py
    server_bootstrap_debian.sh
  backend/
  frontend/
```

后续目录规划：

```text
backend/
  pom.xml
  src/main/java/com/fundmanager/
    FundManagerApplication.java
    common/
    config/
    controller/
    domain/
    repository/
    service/
    scheduler/
  src/main/resources/
    application.yml
    db/migration/

frontend/
  package.json
  vite.config.ts
  src/
    api/
    components/
    router/
    stores/
    types/
    views/
      dashboard/
      positions/
      watchlist/
      login/
```

## 2. 后端分层

- `controller`：接收请求、参数校验、返回统一结构
- `service`：业务编排、收益测算、看板聚合
- `repository`：数据库访问
- `domain`：实体、DTO、VO
- `scheduler`：基金净值与估值同步任务
- `common`：统一响应、异常、工具

## 3. 前端页面规划

- `/login`：登录页
- `/dashboard`：数据看板
- `/positions`：持仓列表
- `/positions/transactions`：交易明细
- `/watchlist`：自选列表
- `/fund/:code`：基金详情

## 4. API 规划

### 4.1 认证

- `POST /api/auth/login`
- `GET /api/auth/me`

### 4.2 基金

- `GET /api/funds/search`
- `GET /api/funds/{code}`
- `GET /api/funds/{code}/nav-history`
- `GET /api/funds/{code}/estimate`

### 4.3 持仓

- `GET /api/positions`
- `POST /api/positions/transactions/buy`
- `POST /api/positions/transactions/sell`
- `GET /api/positions/{fundCode}/transactions`

### 4.4 自选

- `GET /api/watchlist`
- `POST /api/watchlist/{fundCode}`
- `DELETE /api/watchlist/{fundCode}`

### 4.5 看板

- `GET /api/dashboard/overview`
- `GET /api/dashboard/trend`
- `GET /api/dashboard/distribution`
- `GET /api/dashboard/ranking`

## 5. 数据库表设计

### 5.1 `sys_user`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| username | varchar(64) | 登录名 |
| password_hash | varchar(255) | 密码哈希 |
| nickname | varchar(64) | 昵称 |
| status | tinyint | 状态 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 5.2 `fund_info`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| fund_code | varchar(16) | 基金代码 |
| fund_name | varchar(128) | 基金名称 |
| fund_type | varchar(64) | 基金类型 |
| risk_level | varchar(32) | 风险等级 |
| management_company | varchar(128) | 基金公司 |
| status | tinyint | 状态 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 5.3 `fund_nav`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| fund_code | varchar(16) | 基金代码 |
| nav_date | date | 净值日期 |
| unit_nav | decimal(12,6) | 单位净值 |
| accumulated_nav | decimal(12,6) | 累计净值 |
| daily_growth_rate | decimal(10,4) | 日涨跌幅 |
| source | varchar(64) | 数据来源 |
| created_at | datetime | 创建时间 |

### 5.4 `fund_estimate`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| fund_code | varchar(16) | 基金代码 |
| estimate_time | datetime | 估值时间 |
| estimate_nav | decimal(12,6) | 估算净值 |
| estimate_growth_rate | decimal(10,4) | 估算涨跌幅 |
| source | varchar(64) | 数据来源 |
| created_at | datetime | 创建时间 |

### 5.5 `fund_transaction`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| user_id | bigint | 用户 ID |
| fund_code | varchar(16) | 基金代码 |
| transaction_type | varchar(16) | BUY/SELL |
| trade_date | date | 交易日期 |
| amount | decimal(18,2) | 交易金额 |
| shares | decimal(18,4) | 份额 |
| fee | decimal(18,2) | 手续费 |
| nav | decimal(12,6) | 交易净值 |
| remark | varchar(255) | 备注 |
| created_at | datetime | 创建时间 |

### 5.6 `fund_position`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| user_id | bigint | 用户 ID |
| fund_code | varchar(16) | 基金代码 |
| total_amount | decimal(18,2) | 累计投入金额 |
| total_shares | decimal(18,4) | 当前份额 |
| average_cost_nav | decimal(12,6) | 平均成本净值 |
| current_cost | decimal(18,2) | 当前持仓成本 |
| last_trade_date | date | 最近交易日期 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 5.7 `fund_watchlist`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| user_id | bigint | 用户 ID |
| fund_code | varchar(16) | 基金代码 |
| created_at | datetime | 创建时间 |

## 6. 测算规则

### 6.1 持仓成本

- 买入：`current_cost += amount + fee`
- 卖出：按平均成本法冲减成本
- 平均成本净值：`current_cost / total_shares`

### 6.2 实时估算收益

- `estimated_market_value = total_shares * estimate_nav`
- `estimated_profit = estimated_market_value - current_cost`
- `estimated_profit_rate = estimated_profit / current_cost`

### 6.3 当日收益

- `today_profit = total_shares * (estimate_nav - previous_nav)`

## 7. 开发约定

- Java 使用 `BigDecimal` 处理金额和净值
- 关键计算逻辑必须覆盖单元测试
- 时间统一使用 `Asia/Shanghai`
- 前端构建路径为 `/`
- 后端 API 前缀固定为 `/api`
- 生产数据库使用 MariaDB 10.11（MySQL 协议兼容）
- 中国网络环境默认使用仓库内镜像配置：`frontend/.npmrc` 指向 `https://registry.npmmirror.com/`，`backend/.mvn/settings.xml` 指向 `https://maven.aliyun.com/repository/public`
