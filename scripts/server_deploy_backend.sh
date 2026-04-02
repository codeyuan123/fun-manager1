#!/usr/bin/env bash
set -euo pipefail

APP_ROOT=/opt/fund-manager
APP_USER=fundapp
SRC_DIR=/opt/fund-manager/backend-src

mkdir -p "$SRC_DIR"
rm -rf "$SRC_DIR"/*
tar -xzf /root/backend-source.tar.gz -C "$SRC_DIR"

cd "$SRC_DIR"
mvn -DskipTests clean package

cp -f target/backend-0.0.1-SNAPSHOT.jar "$APP_ROOT/backend/app.jar"
chown "$APP_USER:$APP_USER" "$APP_ROOT/backend/app.jar"

cat > /etc/systemd/system/fund-manager-backend.service <<'EOF'
[Unit]
Description=Fund Manager Backend
After=network.target mariadb.service redis-server.service

[Service]
Type=simple
User=fundapp
Group=fundapp
WorkingDirectory=/opt/fund-manager/backend
EnvironmentFile=/opt/fund-manager/config/backend.env
ExecStart=/usr/bin/java -jar /opt/fund-manager/backend/app.jar
Restart=always
RestartSec=5
StandardOutput=append:/opt/fund-manager/backend/logs/backend.log
StandardError=append:/opt/fund-manager/backend/logs/backend-error.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable fund-manager-backend
systemctl restart fund-manager-backend
systemctl is-active fund-manager-backend
