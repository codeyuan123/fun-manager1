# fun-manager1

面向个人投资场景的基金管理系统，前后端分离，已接入东方财富/天天基金公开数据并可本地运行、远端部署。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Element Plus + ECharts + Pinia
- 后端：Spring Boot 3.3.4 + Java 17 + Spring Data JPA + Flyway + Spring Security + JWT
- 数据与缓存：MariaDB 10.11（MySQL 协议兼容）+ Redis 7
- 部署：Debian 12 + Nginx + systemd

## 当前能力（按代码实现）

- 认证：登录、当前用户信息、修改密码
- 基金：搜索、详情聚合、净值历史、估值历史、季度持仓
- 交易：买入、卖出、交易记录、持仓汇总
- 自选：增删查、自选实时估值
- 看板：总览、趋势、分布、涨跌排行
- 回测：策略回测、基金对比回测
- 详情页：区间涨幅走势图 / 今日涨幅走势图（Tab 切换）

## 目录结构

```text
fun-manager1/
  backend/      Spring Boot 后端
  frontend/     Vue 前端
  docs/         架构、工程、部署、交接文档
  scripts/      本地运行与远端部署辅助脚本
```

## 后端 API 概览

统一前缀：`/api`

- 认证：
  - `POST /auth/login`
  - `GET /auth/me`
  - `POST /auth/change-password`
- 基金：
  - `GET /funds/search?keyword=...`
  - `GET /funds/{fundCode}`
  - `GET /funds/{fundCode}/estimate`
  - `GET /funds/{fundCode}/estimate-history?date=YYYY-MM-DD`
  - `GET /funds/{fundCode}/nav-history?range=1m|3m|6m|1y|max`
  - `GET /funds/{fundCode}/holdings?year=YYYY&quarter=1|2|3|4`
- 估值刷新：
  - `POST /estimates/refresh`
- 持仓：
  - `GET /positions`
  - `POST /positions/transactions/buy`
  - `POST /positions/transactions/sell`
  - `GET /positions/{fundCode}/transactions`
- 自选：
  - `GET /watchlist`
  - `POST /watchlist/{fundCode}`
  - `DELETE /watchlist/{fundCode}`
- 看板：
  - `GET /dashboard/overview`
  - `GET /dashboard/trend`
  - `GET /dashboard/distribution`
  - `GET /dashboard/ranking`
- 回测：
  - `POST /backtests/strategies/run`
  - `POST /backtests/funds/run`

### 鉴权规则

- 放行接口：`POST /api/auth/login`、`GET /api/funds/search`、`GET /actuator/health`
- 其他接口需要 JWT；未授权统一返回 `401 Unauthorized`

## 前端路由（主路由）

- `/login`
- `/dashboard/workbench`
- `/fund/market`
- `/fund/positions`
- `/fund/watchlist`
- `/fund/:code`
- `/backtest/strategies`
- `/backtest/funds`

## 本地运行（Windows）

### 1) 推荐环境

- Node.js 22+（已验证 24）
- JDK 17
- Maven 3.9.x
- MariaDB 10.11

### 2) 一键启动顺序

```powershell
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\start_local_mariadb.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_backend.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_frontend.ps1
```

### 3) 访问地址

- 前端：`http://127.0.0.1:5173`
- 后端：`http://127.0.0.1:18080`
- 健康检查：`http://127.0.0.1:18080/actuator/health`
- 默认账号：`admin / admin123`

## 数据源与缓存

第三方数据由后端统一拉取与解析，前端不直连：

- 搜索：`fundsuggest.eastmoney.com`
- 估值：`fundgz.1234567.com.cn`
- 详情/走势：`fund.eastmoney.com/pingzhongdata`
- 持仓：`fundf10.eastmoney.com/FundArchivesDatas.aspx`

缓存 TTL（代码常量）：

- 搜索：30 分钟
- 估值：10 分钟
- 详情：6 小时
- 季度持仓：12 小时

## 部署（Debian）

初始化脚本：

- `scripts/server_bootstrap_debian.sh`

部署脚本：

- `scripts/server_deploy_backend.sh`
- `scripts/server_deploy_frontend.sh`
- `scripts/remote_upload.py`
- `scripts/remote_exec.py`

> Shell 脚本需使用 LF 行尾，仓库通过 `.gitattributes` 约束了 `*.sh text eol=lf`。

## 文档索引

- [docs/architecture.md](docs/architecture.md)
- [docs/engineering.md](docs/engineering.md)
- [docs/deployment.md](docs/deployment.md)
- [docs/codex-handoff.md](docs/codex-handoff.md)

## 协作约定（当前）

- 用户沟通与页面文案使用中文
- 中国网络环境优先使用国内镜像
- 默认不自动远端部署，仅在明确下达“部署/远端部署”指令时执行
