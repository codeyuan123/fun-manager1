# Codex 接管说明

本文档用于未来在其他机器上继续接手 `fun-manager1` 时快速恢复上下文。

## 1. 当前接管结论

截至 `2026-04-02`：

- 仓库分支：`master`
- 远程仓库：`https://github.com/codeyuan123/fun-manager1.git`
- 本地 Windows 运行环境已实测可用
- 远端 Debian 服务器部署链路已实测可用
- 当前代码已经不是最初的演示版，已经开始接入真实基金公开数据

## 2. 这轮已经做了什么

### 2.1 数据与后端

- 接入东方财富 / 天天基金搜索、估值、详情、季度持仓
- 新增缓存层 `RemoteValueCacheService`
- 新增季度持仓快照表 `fund_holding_snapshot`
- 详情接口扩展为聚合返回
- 看板趋势改成基于真实持仓数据计算

### 2.2 前端

- 登录页、看板、持仓、自选、基金详情等主要页面已改成中文
- 新增基金详情页 ` /fund/:code `
- 页面视觉已做过一轮重构，但仍有继续打磨空间

### 2.3 本地环境

- 本地 MariaDB 启动脚本已补好
- 本地后端启动脚本已补好
- 本地前端启动脚本已补好
- 本地 `local` profile 已补上，默认不强依赖 Redis
- 通过国内镜像下载并验证了 `JDK 17 / Maven / MariaDB`

### 2.4 部署

- Debian 部署脚本链路已走通
- 当前要求是：每次任务完成都要部署到远端服务器
- 注意 Windows 提交 shell 脚本时必须保持 LF 行尾

## 3. 本地环境基线

工作目录建议：

```text
C:\pro\
  fun-manager1\
  runtime\
```

`runtime` 内当前建议放：

```text
apache-maven-3.9.14
jdk-17.0.18+8
mariadb-10.11.16-winx64
mariadb-data
```

本地启动命令：

```powershell
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\start_local_mariadb.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_backend.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_frontend.ps1
```

本地验证：

- `http://127.0.0.1:5173`
- `http://127.0.0.1:18080/actuator/health`
- 默认账号：`admin / admin123`

## 4. 远端环境基线

目标是 Debian 服务器，当前生产形态：

- Nginx 提供 `/`
- Spring Boot 监听 `127.0.0.1:18080`
- MariaDB 监听本机 `3306`
- Redis 监听本机 `6379`
- systemd 服务名：`fund-manager-backend`

重要目录：

- `/opt/fund-manager/backend`
- `/opt/fund-manager/frontend/dist`
- `/opt/fund-manager/config/backend.env`

辅助脚本：

- 仓库内：`scripts/remote_exec.py`
- 仓库内：`scripts/remote_upload.py`
- 服务器内：`/root/server_deploy_backend.sh`
- 服务器内：`/root/server_deploy_frontend.sh`

## 5. 继续开发时的硬约束

- 全程中文回复
- 使用国内镜像，避免国外下载过慢
- 页面文案保持中文
- 每次任务完成后都要重新部署远端
- 生产环境仍以 Debian + 非 Docker 方式为主
- 以当前 `master` 为基线继续推进

## 6. 真实代码约定

需要特别注意这些地方，避免被旧文档误导：

- 后端 ORM 现在以 `Spring Data JPA` 为准，不是 `MyBatis-Plus`
- 本地已经可以运行完整开发环境，不再是“只能本地编码、只在服务器运行”
- Redis 在生产启用；本地 `local` profile 默认关闭自动装配

## 7. 当前已知问题

- 基金搜索中文名称乱码，优先排查东方财富搜索接口解析与编码
- 前端打包产物偏大，需要继续做拆包
- 部分设计文档存在历史表述，接手时要先看代码和 README

## 8. 推荐接手顺序

1. 拉取仓库
2. 看 `README.md`
3. 看本文档
4. 启动本地 MariaDB / 后端 / 前端
5. 验证登录与基金搜索
6. 修当前最明确的问题，再部署到远端
