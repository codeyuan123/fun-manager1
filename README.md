# fun-manager1

一个面向个人投资者的基金管理系统，当前采用前后端分离架构：

- 前端：`Vue 3 + Vite + TypeScript + Element Plus + ECharts`
- 后端：`Spring Boot 3.3 + Java 17 + Spring Data JPA + Flyway + JWT`
- 数据：`MariaDB 10.11`
- 缓存：`Redis 7`（生产启用，本地 `local` profile 默认可不装）
- 部署：`Debian 12 + Nginx + systemd`

项目目标不是做演示页，而是逐步演进成一个可以真实接入基金公开数据、支持持仓、自选、看板和基金详情的可持续产品。

## 1. 当前状态

截至 `2026-04-02`，当前仓库已经完成这些基线能力：

- 登录、看板、持仓、自选、基金详情的基础页面与接口
- 接入东方财富 / 天天基金公开数据接口，由后端统一代理和解析
- 基金详情聚合、净值历史、季度持仓快照、真实看板趋势计算
- 前端主要页面中文化
- Debian 服务器部署链路可用
- Windows 本地开发环境可跑通

当前默认分支：`master`

远程仓库：

- `origin = https://github.com/codeyuan123/fun-manager1.git`

## 2. 先看哪里

如果是第一次接手，建议按这个顺序看：

1. `README.md`
2. `docs/codex-handoff.md`
3. `docs/engineering.md`
4. `docs/deployment.md`
5. `docs/architecture.md`

说明：

- 以代码和 `README.md` / `docs/codex-handoff.md` 为准
- 旧文档中若有与代码不一致的地方，优先相信当前代码实现

## 3. 目录结构

```text
fun-manager1/
  backend/                     Spring Boot 后端
  frontend/                    Vue 3 前端
  docs/                        架构、工程、部署、交接文档
  scripts/                     本地运行、远端部署、远端上传脚本
```

## 4. 已实现的核心能力

### 4.1 认证

- `POST /api/auth/login`
- `GET /api/auth/me`
- 默认开发账号：`admin / admin123`

### 4.2 基金能力

- `GET /api/funds/search`
- `GET /api/funds/{code}`
- `GET /api/funds/{code}/nav-history?range=1m|3m|6m|1y|max`
- `GET /api/funds/{code}/estimate`
- `GET /api/funds/{code}/holdings?year=YYYY&quarter=1|2|3|4`

### 4.3 持仓与自选

- 当前持仓查询
- 买入 / 卖出交易
- 自选增删查
- 看板总览、趋势、分布、排行

## 5. 第三方数据源

当前主数据源是东方财富 / 天天基金公开接口，由后端统一访问，前端不直接访问第三方：

- 搜索：`FundSearchAPI.ashx`
- 盘中估值：`fundgz.1234567.com.cn`
- 基金详情与走势：`pingzhongdata/{code}.js`
- 季度持仓：`FundArchivesDatas.aspx`

缓存策略：

- 搜索：30 分钟
- 估值：10 分钟
- 详情：6 小时
- 季度持仓：12 小时

本地缓存与 Redis 缓存同时存在；上游异常时优先回退缓存或数据库快照。

## 6. 本地开发环境

当前已验证的本地开发环境是 Windows，目录建议：

```text
C:\pro\
  fun-manager1\
  runtime\
    apache-maven-3.9.14\
    jdk-17.0.18+8\
    mariadb-10.11.16-winx64\
    mariadb-data\
```

### 6.1 推荐版本

- `Node.js 24`
- `npm 11`
- `Temurin JDK 17`
- `Maven 3.9.14`
- `MariaDB 10.11`

### 6.2 国内镜像

仓库已内置中国网络环境所需镜像配置：

- 前端：[`frontend/.npmrc`](frontend/.npmrc)
- 后端：[`backend/.mvn/settings.xml`](backend/.mvn/settings.xml)

### 6.3 本地启动顺序

1. 启动数据库

```powershell
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\start_local_mariadb.ps1
```

2. 启动后端

```powershell
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_backend.ps1
```

3. 启动前端

```powershell
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_frontend.ps1
```

本地访问地址：

- 前端：`http://127.0.0.1:5173`
- 后端：`http://127.0.0.1:18080`
- 健康检查：`http://127.0.0.1:18080/actuator/health`

### 6.4 本地说明

- `local` profile 会禁用 Redis 自动装配
- Windows 非管理员环境下，MariaDB 默认走后台进程模式，不强依赖 Windows 服务
- 本地数据库初始化账号写在脚本里，仅供开发使用，不是生产凭据

## 7. 测试与构建

前端：

```powershell
cd C:\pro\fun-manager1\frontend
npm ci
npm run build
```

后端：

```powershell
$env:JAVA_HOME='C:\pro\runtime\jdk-17.0.18+8'
$env:Path="$env:JAVA_HOME\bin;C:\pro\runtime\apache-maven-3.9.14\bin;$env:Path"
cd C:\pro\fun-manager1\backend
mvn test
```

已验证通过：

- `npm run build`
- `mvn test`
- 本地登录接口冒烟
- 本地前后端启动

## 8. 生产部署

目标服务器：

- 系统：`Debian 12`
- 入口：`Nginx`
- 后端：`systemd` 托管 Spring Boot
- 数据库：`MariaDB 10.11`
- 缓存：`Redis 7`

核心路径：

- `/opt/fund-manager/backend/app.jar`
- `/opt/fund-manager/frontend/dist`
- `/opt/fund-manager/config/backend.env`

常用脚本：

- [`scripts/server_bootstrap_debian.sh`](scripts/server_bootstrap_debian.sh)
- 远端 `/root/server_deploy_backend.sh`
- 远端 `/root/server_deploy_frontend.sh`
- [`scripts/remote_exec.py`](scripts/remote_exec.py)
- [`scripts/remote_upload.py`](scripts/remote_upload.py)

说明：

- 生产秘密信息不要写进仓库
- 生产数据库密码、JWT 秘钥等放在服务器的 `backend.env`
- Shell 脚本需要 LF 行尾，仓库已通过 [`.gitattributes`](.gitattributes) 约束 `*.sh`

## 9. 下次继续开发前要知道的事

### 9.1 用户偏好

- 与用户沟通时使用中文
- 中国网络环境优先使用国内镜像
- 页面文案保持中文
- 每次任务完成后都要继续部署到远端服务器

### 9.2 工程约定

- 时间统一使用 `Asia/Shanghai`
- 前端构建路径固定为 `/`
- 后端 API 前缀固定为 `/api`
- 生产数据库使用 `MariaDB 10.11`
- 不要把生产密码、服务器密码提交到 GitHub

### 9.3 当前已知问题

- 基金搜索结果里中文名称仍存在编码问题，需要继续修解析或响应编码
- `docs` 下部分旧文档有历史描述，继续开发时要优先参考代码与本 README

### 9.4 推荐下一步

1. 修复东方财富搜索结果中文乱码
2. 继续补基金详情页和看板的真实数据展示
3. 提升前端大包体积问题，做路由级拆包和图表按需加载
4. 补更多后端解析测试和接口 smoke test

## 10. 文档索引

- 工程设计：[docs/engineering.md](docs/engineering.md)
- 部署文档：[docs/deployment.md](docs/deployment.md)
- 架构说明：[docs/architecture.md](docs/architecture.md)
- 交接说明：[docs/codex-handoff.md](docs/codex-handoff.md)
