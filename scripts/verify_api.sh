#!/usr/bin/env bash
set -euo pipefail

cat >/tmp/fm_login.json <<EOF
{"username":"admin","password":"admin123"}
EOF

LOGIN_RESP=$(curl -sS -X POST http://127.0.0.1:18080/api/auth/login -H 'Content-Type: application/json' --data @/tmp/fm_login.json)
echo "$LOGIN_RESP"

TOKEN=$(echo "$LOGIN_RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "token missing"
  exit 1
fi

echo "token_ok"
echo "backend overview:"
curl -sS http://127.0.0.1:18080/api/dashboard/overview -H "Authorization: Bearer $TOKEN"
echo
echo "nginx overview:"
curl -sS http://127.0.0.1/api/dashboard/overview -H "Authorization: Bearer $TOKEN"
echo
