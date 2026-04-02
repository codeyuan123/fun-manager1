#!/usr/bin/env bash
set -euo pipefail

APP_ROOT=/opt/fund-manager

mkdir -p "$APP_ROOT/frontend/dist"
rm -rf "$APP_ROOT/frontend/dist"/*
tar -xzf /root/frontend-dist.tar.gz -C "$APP_ROOT/frontend/dist"
chown -R fundapp:fundapp "$APP_ROOT/frontend"

nginx -t
systemctl reload nginx

echo "frontend deployed"
