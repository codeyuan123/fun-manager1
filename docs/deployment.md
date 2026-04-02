# 部署与服务器准备文档

## 1. 部署原则

- 本地只负责编码、构建、调试
- 服务器负责数据库、缓存、反向代理、后端运行和前端发布
- 生产环境统一使用 `Asia/Shanghai`
- 服务端目录统一放置在 `/opt/fund-manager`
- 当前已停用旧 Nginx 站点，基金项目将独占 `80` 端口入口

## 2. 实际服务器基线

- 操作系统：`Debian 12 (bookworm)`
- 已安装：`OpenJDK 17`、`Node.js 22`、`Nginx`、`Docker`
- 待安装：`MariaDB 10.11`、`Redis 7`、`Maven`

## 3. 推荐目录规划

```text
/opt/fund-manager/
  backend/
    app.jar
    logs/
    config/
  frontend/
    dist/
  scripts/
  backups/
  config/
    backend.env
```

## 4. 服务规划

- `fund-manager-backend.service`
- `nginx`
- `mariadb`
- `redis-server`

## 5. 网络规划

- `80`：前端入口
- `443`：HTTPS 入口（后续）
- `18080`：后端应用内部监听，仅 Nginx 反代
- `3306`：MariaDB，仅本机访问
- `6379`：Redis，仅本机访问

## 6. Nginx 规划

- `/`：前端静态资源与 SPA 路由
- `/api/`：反向代理到 `http://127.0.0.1:18080`

## 7. 初始化步骤

1. 中国网络环境默认执行 `MIRROR_PROFILE=cn bash scripts/server_bootstrap_debian.sh`，脚本会切到 `TUNA` Debian 源；如服务器不在国内可显式改为 `MIRROR_PROFILE=global`
2. 安装 MariaDB、Redis、Maven
3. 创建应用目录、运行用户和日志目录
4. 初始化数据库与应用账号
5. 生成服务端环境变量文件
6. 配置 Nginx 站点
7. 配置 systemd 托管 Spring Boot
8. 验证数据库和 Redis 连通性

## 8. 数据库规划

- 数据库名：`fund_manager`
- 应用用户：`fund_app`
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`

## 9. 运行配置规划

后端核心环境变量：

- `SPRING_PROFILES_ACTIVE=prod`
- `SERVER_PORT=18080`
- `DB_HOST=127.0.0.1`
- `DB_PORT=3306`
- `DB_NAME=fund_manager`
- `DB_USERNAME=fund_app`
- `DB_PASSWORD=<server-side-secret>`
- `REDIS_HOST=127.0.0.1`
- `REDIS_PORT=6379`
- `JWT_SECRET=<server-side-secret>`

## 10. 说明

本文档已结合当前服务器实际情况整理，可直接作为服务器初始化与后续部署基线。

补充说明：

- 前端依赖默认通过 `frontend/.npmrc` 使用 `https://registry.npmmirror.com/`
- 后端 Maven 构建默认通过 `backend/.mvn/settings.xml` 使用 `https://maven.aliyun.com/repository/public`
- Windows 本地运行可使用 `scripts/start_local_mariadb.ps1`、`scripts/run_local_backend.ps1`、`scripts/run_local_frontend.ps1`

## 11. Windows 本地运行基线

本地推荐目录：

```text
C:\pro\runtime\
  apache-maven-3.9.14\
  jdk-17.0.18+8\
  mariadb-10.11.16-winx64\
  mariadb-data\
```

当前已验证的本地版本：

- `Node.js 24`
- `Maven 3.9.14`
- `Temurin JDK 17`
- `MariaDB 10.11`

本地启动顺序：

1. 执行 `powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\start_local_mariadb.ps1`
2. 执行 `powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_backend.ps1`
3. 执行 `powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_frontend.ps1`

说明：

- 本地后端默认使用 `SPRING_PROFILES_ACTIVE=local`
- `local` profile 会禁用 Redis 自动装配，本地不强制安装 Redis
- 本地数据库账号：
  - root：`root_local_password`
  - 应用用户：`fund_app`
  - 应用库：`fund_manager`
- 默认登录账号：`admin / admin123`
- 本地访问地址：
  - 前端：`http://127.0.0.1:5173`
  - 后端：`http://127.0.0.1:18080`
