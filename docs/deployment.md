# 部署文档（按当前脚本与代码）

更新时间：`2026-04-03`

## 1. 目标环境

- 系统：Debian 12
- Web：Nginx
- 后端：Spring Boot + systemd（`fund-manager-backend`）
- 数据：MariaDB 10.11
- 缓存：Redis 7
- 项目根目录：`/opt/fund-manager`

目录约定：

```text
/opt/fund-manager/
  backend/
    app.jar
    logs/
  frontend/
    dist/
  config/
    backend.env
```

## 2. 一次性初始化（新机器）

在服务器执行：

```bash
MIRROR_PROFILE=cn bash scripts/server_bootstrap_debian.sh
```

说明：

- `MIRROR_PROFILE=cn` 会切换 Debian APT 为清华源
- 脚本会安装并启用：MariaDB、Redis、Maven、Nginx
- 自动生成 `/opt/fund-manager/config/backend.env`
- 自动写入 Nginx 站点并 reload

## 3. 本地构建与打包

### 3.1 前端

```powershell
cd C:\pro\fun-manager1\frontend
npm.cmd run build
tar -czf C:\pro\fun-manager1\artifacts\frontend-dist.tar.gz -C C:\pro\fun-manager1\frontend\dist .
```

### 3.2 后端

```powershell
tar -czf C:\pro\fun-manager1\artifacts\backend-source.tar.gz -C C:\pro\fun-manager1\backend .
```

> 后端在服务器通过 `mvn -DskipTests clean package` 编译成 `app.jar`。

## 4. 上传部署包

使用仓库脚本 `scripts/remote_upload.py`：

```powershell
$env:REMOTE_HOST='你的服务器IP'
$env:REMOTE_USER='root'
$env:REMOTE_PASS='你的密码'

python scripts/remote_upload.py C:\pro\fun-manager1\artifacts\backend-source.tar.gz /root/backend-source.tar.gz
python scripts/remote_upload.py C:\pro\fun-manager1\artifacts\frontend-dist.tar.gz /root/frontend-dist.tar.gz
python scripts/remote_upload.py C:\pro\fun-manager1\scripts\server_deploy_backend.sh /root/server_deploy_backend.sh
python scripts/remote_upload.py C:\pro\fun-manager1\scripts\server_deploy_frontend.sh /root/server_deploy_frontend.sh
```

## 5. 服务器执行部署

> 首次从 Windows 上传 shell 时，建议先做换行修正。

```bash
sed -i 's/\r$//' /root/server_deploy_backend.sh /root/server_deploy_frontend.sh
chmod +x /root/server_deploy_backend.sh /root/server_deploy_frontend.sh

/root/server_deploy_backend.sh
/root/server_deploy_frontend.sh
```

脚本行为：

- `server_deploy_backend.sh`
  - 解压 `/root/backend-source.tar.gz`
  - 服务器编译后端
  - 更新 `/opt/fund-manager/backend/app.jar`
  - 重启 `fund-manager-backend`
- `server_deploy_frontend.sh`
  - 解压 `/root/frontend-dist.tar.gz` 到 `frontend/dist`
  - `nginx -t` + `systemctl reload nginx`

## 6. 部署后验证

```bash
systemctl is-active fund-manager-backend
systemctl is-active nginx
curl -s -o /dev/null -w "web:%{http_code}\n" http://127.0.0.1/
curl -s -o /dev/null -w "api:%{http_code}\n" http://127.0.0.1/api/funds/161725
```

说明：

- `api:401` 在未携带 token 时是预期结果，表示链路通

## 7. 本地运行（开发）

```powershell
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\start_local_mariadb.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_backend.ps1
powershell -ExecutionPolicy Bypass -File C:\pro\fun-manager1\scripts\run_local_frontend.ps1
```

## 8. 生产注意事项

- 生产密钥和数据库密码只保存在服务器 `backend.env`
- 不要把服务器密码、JWT 密钥提交到 GitHub
- `*.sh` 必须 LF 行尾（仓库 `.gitattributes` 已约束）
