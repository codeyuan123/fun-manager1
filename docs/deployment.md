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

1. 安装 MariaDB、Redis、Maven
2. 创建应用目录、运行用户和日志目录
3. 初始化数据库与应用账号
4. 生成服务端环境变量文件
5. 配置 Nginx 站点
6. 配置 systemd 托管 Spring Boot
7. 验证数据库和 Redis 连通性

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
