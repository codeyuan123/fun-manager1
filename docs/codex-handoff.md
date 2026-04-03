# Codex 交接文档

更新时间：`2026-04-03`

## 1. 仓库与分支

- 仓库：`https://github.com/codeyuan123/fun-manager1.git`
- 默认分支：`master`
- 本地工作目录：`C:\pro\fun-manager1`

## 2. 当前系统状态（代码事实）

- 前端：Vue 3 + Vite + TS，页面已中文化
- 后端：Spring Boot 3.3.4 + JPA + Flyway + JWT
- 数据：MariaDB，缓存：Redis（local profile 默认禁用 Redis）
- 真实数据源：东方财富/天天基金，由后端统一代理
- 已上线功能：登录、看板、持仓、自选、基金详情、回测
- 详情页图表：区间涨幅 / 今日涨幅切换

## 3. 用户偏好与协作规则

- 与用户沟通使用中文
- 中国网络优先国内镜像
- 页面文案保持中文
- 默认不自动部署；只有用户明确说“部署/远端部署”才执行远端部署
- 需要提交代码时，包含本次任务相关文档与代码改动

## 4. 接手后先做什么

1. `git pull` 拉取最新
2. 读 `README.md`
3. 读 `docs/engineering.md` 与 `docs/deployment.md`
4. 本地起服务验证基础链路
5. 再进行功能开发或线上修复

## 5. 本地运行基线

推荐 runtime 目录：

```text
C:\pro\runtime\
  apache-maven-3.9.14\
  jdk-17.0.18+8\
  mariadb-10.11.16-winx64\
  mariadb-data\
```

启动命令：

```powershell
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\start_local_mariadb.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_backend.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_frontend.ps1
```

默认账号：`admin / admin123`

## 6. 远端部署基线

关键脚本：

- `scripts/remote_upload.py`
- `scripts/remote_exec.py`
- `scripts/server_deploy_backend.sh`
- `scripts/server_deploy_frontend.sh`

关键目录：

- `/opt/fund-manager/backend/app.jar`
- `/opt/fund-manager/frontend/dist`
- `/opt/fund-manager/config/backend.env`

## 7. 当前待关注事项

- 线上偶发异常优先看：
  - `/opt/fund-manager/backend/logs/backend.log`
  - `/opt/fund-manager/backend/logs/backend-error.log`
- 前端打包体积较大（`useEChart` chunk 体积高），后续可做路由级拆包与图表按需优化
- 文档如再与代码不一致，优先修文档再继续开发
