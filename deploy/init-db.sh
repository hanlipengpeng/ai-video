#!/usr/bin/env bash
# ============================================================
# 数据库独立初始化脚本（可单独运行，deploy.sh 也会调用相同逻辑）
# 用法：
#   MYSQL_USER=studen MYSQL_PASSWORD=studenadmin bash deploy/init-db.sh
#   # 或用 root 建库授权：
#   MYSQL_USER=root MYSQL_PASSWORD=<root密码> bash deploy/init-db.sh
# ============================================================
set -euo pipefail

MYSQL_USER="${MYSQL_USER:-studen}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-studenadmin}"
MYSQL_DB="${MYSQL_DB:-comic_db}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
SCHEMA="$REPO_DIR/backend/src/main/resources/db/schema.sql"

[ -f "$SCHEMA" ] || { echo "找不到 schema.sql: $SCHEMA"; exit 1; }

echo "[init-db] 使用 $MYSQL_USER@$MYSQL_HOST 初始化数据库 $MYSQL_DB ..."

if mysql -h"$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" < "$SCHEMA" 2>/dev/null; then
    echo "[init-db] 成功：库 + 8 张表已创建"
else
    echo "[init-db] $MYSQL_USER 权限不足或库不存在，尝试只建表..."
    if mysql -h"$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < "$SCHEMA" 2>/dev/null; then
        echo "[init-db] 成功：在现有库 $MYSQL_DB 中建表完成"
    else
        echo "[init-db] 失败。若 $MYSQL_USER 无建库权限，请用 root 执行："
        echo "  MYSQL_USER=root MYSQL_PASSWORD=<root密码> bash deploy/init-db.sh"
        echo "  或手动授权："
        echo "  mysql -uroot -p -e \"CREATE DATABASE IF NOT EXISTS comic_db DEFAULT CHARSET utf8mb4; GRANT ALL ON comic_db.* TO 'studen'@'%'; FLUSH PRIVILEGES;\""
        exit 1
    fi
fi

echo "[init-db] 当前表："
mysql -h"$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" -e "SHOW TABLES;" 2>/dev/null || true
