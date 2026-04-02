#!/usr/bin/env bash
set -euo pipefail

APP_ROOT=/opt/fund-manager
APP_USER=fundapp
DB_NAME=fund_manager
DB_USER=fund_app

export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y mariadb-server redis-server maven curl unzip

if ! id -u "$APP_USER" >/dev/null 2>&1; then
  useradd --system --home "$APP_ROOT" --shell /usr/sbin/nologin "$APP_USER"
fi

mkdir -p \
  "$APP_ROOT/backend/config" \
  "$APP_ROOT/backend/logs" \
  "$APP_ROOT/frontend/dist" \
  "$APP_ROOT/scripts" \
  "$APP_ROOT/backups" \
  "$APP_ROOT/config"

chown -R "$APP_USER:$APP_USER" "$APP_ROOT/backend" "$APP_ROOT/frontend" "$APP_ROOT/scripts"
chmod 755 "$APP_ROOT" "$APP_ROOT/backend" "$APP_ROOT/frontend" "$APP_ROOT/scripts"

systemctl enable --now mariadb
systemctl enable --now redis-server
systemctl enable --now nginx

DB_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -hex 32)

cat > "$APP_ROOT/config/backend.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=18080
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
JWT_SECRET=$JWT_SECRET
TZ=Asia/Shanghai
EOF

chown root:"$APP_USER" "$APP_ROOT/config/backend.env"
chmod 640 "$APP_ROOT/config/backend.env"

mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

cat > /etc/nginx/sites-available/fund-manager <<'EOF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    root /opt/fund-manager/frontend/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

ln -sfn /etc/nginx/sites-available/fund-manager /etc/nginx/sites-enabled/fund-manager
nginx -t
systemctl reload nginx

echo "Bootstrap completed."
echo "Environment file: $APP_ROOT/config/backend.env"
