# 工程设计文档（按代码对齐）

更新时间：`2026-04-03`

## 1. 仓库结构

```text
fun-manager1/
  backend/
    src/main/java/com/fundmanager/
      common/      统一响应与异常
      config/      安全、HTTP 客户端等配置
      controller/  REST 接口
      domain/      entity/dto/vo
      repository/  Spring Data JPA
      scheduler/   定时任务
      security/    JWT 过滤器与服务
      service/     核心业务
    src/main/resources/
      application.yml
      application-local.yml
      db/migration/
  frontend/
    src/
      api/
      components/
      composables/
      layout/
      router/
      stores/
      types/
      views/
  scripts/
  docs/
```

## 2. 后端分层与职责

- `controller`：暴露 API，不做复杂业务计算
- `service`：业务编排、第三方数据解析、收益计算、回测执行
- `repository`：JPA 访问 MariaDB
- `scheduler`：交易时段估值刷新任务
- `security`：JWT 鉴权、用户认证
- `common`：`ApiResponse` + 全局异常处理

## 3. 前端模块

- 路由入口：`frontend/src/router/index.ts`
- 登录与会话：`stores/auth.ts`
- HTTP 统一处理：`api/http.ts`
- 业务页面：
  - 看板：`views/dashboard`
  - 基金行情/详情：`views/fund`
  - 持仓：`views/positions`
  - 自选：`views/watchlist`
  - 回测：`views/backtest`

## 4. API 实际清单

### 4.1 认证

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/change-password`

### 4.2 基金

- `GET /api/funds/search`
- `GET /api/funds/{fundCode}`
- `GET /api/funds/{fundCode}/estimate`
- `GET /api/funds/{fundCode}/estimate-history`
- `GET /api/funds/{fundCode}/nav-history`
- `GET /api/funds/{fundCode}/holdings`

### 4.3 估值刷新

- `POST /api/estimates/refresh`

### 4.4 持仓

- `GET /api/positions`
- `POST /api/positions/transactions/buy`
- `POST /api/positions/transactions/sell`
- `GET /api/positions/{fundCode}/transactions`

### 4.5 自选

- `GET /api/watchlist`
- `POST /api/watchlist/{fundCode}`
- `DELETE /api/watchlist/{fundCode}`

### 4.6 看板

- `GET /api/dashboard/overview`
- `GET /api/dashboard/trend`
- `GET /api/dashboard/distribution`
- `GET /api/dashboard/ranking`

### 4.7 回测

- `POST /api/backtests/strategies/run`
- `POST /api/backtests/funds/run`

### 4.8 鉴权规则

- 放行：`POST /api/auth/login`、`GET /api/funds/search`、`GET /actuator/health`
- 其他接口统一要求 JWT，未授权返回 `401`

## 5. 数据库与迁移

Flyway 迁移文件：

- `V1__init_schema.sql`
- `V2__add_fund_holding_snapshot.sql`
- `V3__extend_fund_estimate.sql`

核心表（当前真实存在）：

- `sys_user`
- `fund_info`
- `fund_nav`
- `fund_estimate`（已扩展 `estimate_source/estimate_confidence/holding_coverage_rate/quoted_coverage_rate`）
- `fund_transaction`
- `fund_position`
- `fund_watchlist`
- `fund_holding_snapshot`

## 6. 第三方数据接入

由后端统一请求东方财富/天天基金接口：

- 搜索：`FundSearchAPI.ashx`
- 估值：`fundgz/{code}.js`
- 详情：`pingzhongdata/{code}.js`
- 持仓：`FundArchivesDatas.aspx`

缓存层：`RemoteValueCacheService`（Redis + 本地内存兜底）

TTL：

- 搜索 30m
- 估值 10m
- 详情 6h
- 持仓 12h

## 7. 调度任务

`QuoteSyncScheduler` 已启用，`Asia/Shanghai` 时区下在交易日多段 cron 自动刷新估值：

- 9:35-9:55（5 分钟粒度）
- 10:00-10:55（5 分钟粒度）
- 11:00-11:30（5 分钟粒度）
- 13:05-13:55（5 分钟粒度）
- 14:00-14:55（5 分钟粒度）
- 15:00 收盘刷新

## 8. 本地配置与约束

- 默认 profile：`local`
- `application-local.yml` 中禁用 Redis 自动配置，便于本地无 Redis 运行
- 时间统一：`Asia/Shanghai`
- API 前缀固定：`/api`
- 前端构建基路径：`/`

## 9. 工程注意事项

- Shell 脚本统一 LF（见 `.gitattributes`）
- 生产密钥与数据库密码只放服务器环境变量，不进仓库
- 文档与实现冲突时，以代码为准并及时回写本文档
