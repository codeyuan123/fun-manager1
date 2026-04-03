# 架构说明（按当前实现）

更新时间：`2026-04-03`

## 1. 目标与边界

`fun-manager1` 当前定位是可持续迭代的基金管理应用，不是静态演示页。系统职责：

- 账号认证与会话管理
- 基金行情/估值/详情聚合
- 持仓与交易管理
- 自选管理
- 看板与回测分析

边界：

- 前端不直连第三方基金接口
- 第三方异常不直接透出给页面，优先缓存/快照兜底

## 2. 组件拓扑

```text
Browser (Vue SPA)
  -> Nginx (/, /api/)
    -> Spring Boot API (JWT + Service Layer)
      -> MariaDB (业务数据 + 历史快照)
      -> Redis (远程值缓存，可选)
      -> Eastmoney/Tiantian 公共接口
```

## 3. 前端架构

- 框架：Vue 3 + Vue Router + Pinia
- UI：Element Plus
- 图表：ECharts
- 通信：Axios，统一封装在 `api/http.ts`

关键机制：

- 路由守卫依赖 `auth.ensureSession()`
- 接口 `401` 统一清理登录态并跳回 `/login`
- 会话超时策略：30 分钟，提前 5 分钟提示续期

主要页面：

- 工作台 `/dashboard/workbench`
- 实时行情 `/fund/market`
- 持仓 `/fund/positions`
- 自选 `/fund/watchlist`
- 基金详情 `/fund/:code`
- 回测 `/backtest/strategies`、`/backtest/funds`

## 4. 后端架构

后端采用典型 Controller-Service-Repository 分层：

- Controller：API 入口与参数映射
- Service：业务编排、解析与估算计算、回测执行
- Repository：JPA 持久化
- Scheduler：交易时段自动刷新估值

关键服务：

- `FundQuoteService`：估值与详情快照加载
- `EastmoneyFundClient` + `EastmoneyFundParser`：第三方拉取与解析
- `EstimateRefreshService`：按用户或全量刷新估值
- `DashboardService`：看板指标聚合
- `BacktestService`：回测执行入口

## 5. 数据流

### 5.1 基金详情页

1. 前端请求 `/api/funds/{fundCode}`
2. 后端尝试刷新远程详情与估值，失败时读取本地快照/缓存
3. 聚合返回详情、经理信息、资产配置、规模趋势、同类参考等
4. 前端继续请求：
   - `/api/funds/{fundCode}/nav-history`
   - `/api/funds/{fundCode}/estimate-history`
   - `/api/funds/{fundCode}/holdings`

### 5.2 估值刷新

1. 用户触发 `/api/estimates/refresh`（可指定基金列表）
2. 后端按持仓 + 自选基金集刷新估值
3. 写入 `fund_estimate` 并返回刷新统计

### 5.3 看板

1. 基于交易与持仓数据计算成本、市值、收益
2. 结合估值快照生成趋势、分布和排行
3. 通过 `dashboard/*` 接口返回给前端

## 6. 数据与缓存设计

主存储：MariaDB  
缓存：Redis（本地 `local` profile 可关闭）

远程值缓存策略：

- 搜索：30 分钟
- 估值：10 分钟
- 详情：6 小时
- 季度持仓：12 小时

缓存读取优先级：

1. Redis 可用值
2. 本地内存缓存
3. 第三方请求
4. 第三方失败则尽量回退已有缓存/快照

## 7. 安全与鉴权

- 认证：用户名密码 + JWT
- 密码存储：BCrypt
- 放行接口：登录、基金搜索、健康检查
- 其余 API 统一鉴权
- 未授权统一 `401 Unauthorized`

## 8. 运行时约束

- 全链路时间时区：`Asia/Shanghai`
- 后端内部端口：`18080`
- Nginx 对外入口：`80`
- API 前缀固定：`/api`
