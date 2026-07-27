#!/usr/bin/env bash
# ============================================================
# AI 漫剧制作网站 - 一键部署脚本（在服务器上执行）
# 适用：Ubuntu 24.04 + Java 21 + MySQL
# 端口：81（前后端打包在同一 jar）
#
# 用法：
#   # 1. SSH 到服务器
#   # 2. 拉取代码（首次）
#   git clone https://github.com/hanlipengpeng/ai-video.git /opt/ai-video
#   cd /opt/ai-video
#   # 3. 设置 Agnes Key（必填）
#   export AGNES_KEY_01=sk-xxxxxxxx
#   # 4. 执行部署
#   bash deploy/deploy.sh
#
# 后续更新：cd /opt/ai-video && git pull && bash deploy/deploy.sh
# ============================================================
set -euo pipefail

# ---------- 配置 ----------
APP_NAME="comic"
APP_DIR="/opt/ai-video"
INSTALL_DIR="/opt/comic"
REPO_URL="https://github.com/hanlipengpeng/ai-video.git"
JAR_NAME="comic-backend-0.0.1-SNAPSHOT.jar"
MYSQL_USER="${MYSQL_USER:-studen}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-studenadmin}"
MYSQL_DB="${MYSQL_DB:-comic_db}"

# 颜色
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[部署]${NC} $*"; }
warn() { echo -e "${YELLOW}[警告]${NC} $*"; }
err()  { echo -e "${RED}[错误]${NC} $*" >&2; }

# ---------- 前置检查 ----------
[ -n "${AGNES_KEY_01:-}" ] || { err "未设置 AGNES_KEY_01 环境变量，请先 export AGNES_KEY_01=sk-xxxx"; exit 1; }
[ "$(id -u)" -eq 0 ] || { err "请用 root 执行：sudo bash deploy/deploy.sh"; exit 1; }

# 确定脚本所在目录（仓库根）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
cd "$REPO_DIR"
log "仓库目录：$REPO_DIR"

# ---------- 1. 安装系统依赖 ----------
install_deps() {
    log "检查并安装系统依赖..."
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -qq
    # Java 21 已预装，这里只确保；maven/node 用于构建；ffmpeg 用于视频合成；mysql-client 用于建库
    apt-get install -y -qq git maven nodejs npm ffmpeg mysql-client curl >/dev/null 2>&1 || true

    # 校验关键依赖
    command -v java    >/dev/null || { err "Java 未安装"; exit 1; }
    command -v mvn     >/dev/null || { err "Maven 未安装"; exit 1; }
    command -v node    >/dev/null || { err "Node.js 未安装"; exit 1; }
    command -v npm     >/dev/null || { err "npm 未安装"; exit 1; }
    command -v ffmpeg  >/dev/null || { err "ffmpeg 未安装"; exit 1; }
    log "Java: $(java -version 2>&1 | head -1)"
    log "Maven: $(mvn -version 2>&1 | head -1)"
    log "Node: $(node --version)  npm: $(npm --version)"
    log "ffmpeg: $(ffmpeg -version 2>&1 | head -1)"
}

# ---------- 2. 构建前端 ----------
build_frontend() {
    log "构建前端（产物输出到后端 static 目录）..."
    cd "$REPO_DIR/frontend"
    npm install --silent 2>&1 | tail -3 || { err "npm install 失败"; exit 1; }
    npm run build 2>&1 | tail -5 || { err "前端构建失败"; exit 1; }
    log "前端产物已输出到 backend/src/main/resources/static/"
    cd "$REPO_DIR"
}

# ---------- 3. 构建后端 jar ----------
build_backend() {
    log "构建后端 jar..."
    cd "$REPO_DIR/backend"
    # 使用阿里云 Maven 镜像加速（服务器在国内）
    mvn clean package -DskipTests -s "$REPO_DIR/maven-settings.xml" -q 2>&1 | tail -5 || { err "后端构建失败"; exit 1; }
    [ -f "target/$JAR_NAME" ] || { err "jar 未生成：target/$JAR_NAME"; exit 1; }
    log "jar 构建成功：$(ls -lh target/$JAR_NAME | awk '{print $5}')"
    cd "$REPO_DIR"
}

# ---------- 4. 初始化数据库 ----------
init_database() {
    log "初始化数据库..."
    cd "$REPO_DIR"
    # 尝试用 studen 用户建库建表
    if mysql -h127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" < backend/src/main/resources/db/schema.sql 2>/dev/null; then
        log "数据库初始化成功（用户 $MYSQL_USER）"
    else
        warn "用 $MYSQL_USER 建库失败，可能缺少 CREATE DATABASE 权限。"
        warn "请用 root 账号执行以下命令授权后重试："
        echo ""
        echo "  mysql -uroot -p<root密码> -e \"CREATE DATABASE IF NOT EXISTS comic_db DEFAULT CHARSET utf8mb4; GRANT ALL ON comic_db.* TO 'studen'@'%'; FLUSH PRIVILEGES;\""
        echo ""
        # 尝试只建表（假设库已存在）
        if mysql -h127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < backend/src/main/resources/db/schema.sql 2>/dev/null; then
            log "已用现有数据库 $MYSQL_DB 完成建表"
        else
            err "数据库初始化失败，请按上方提示用 root 授权后重试"; exit 1
        fi
    fi
}

# ---------- 5. 安装应用 ----------
install_app() {
    log "安装应用到 $INSTALL_DIR ..."
    mkdir -p "$INSTALL_DIR/logs" "$INSTALL_DIR/storage"
    cp "backend/target/$JAR_NAME" "$INSTALL_DIR/app.jar"

    # 生成 .env（含 Agnes Key，不提交到 git）
    cat > "$INSTALL_DIR/.env" <<EOF
# Agnes AI Key
AGNES_KEY_01=$AGNES_KEY_01
# MySQL（同机部署）
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DB=$MYSQL_DB
MYSQL_USER=$MYSQL_USER
MYSQL_PASSWORD=$MYSQL_PASSWORD
# 存储路径
COMIC_STORAGE_PATH=$INSTALL_DIR/storage
FFMPEG_PATH=ffmpeg
# JWT Secret
JWT_SECRET=$(openssl rand -base64 48 2>/dev/null || echo "comic-jwt-secret-$(date +%s)")
EOF
    chmod 600 "$INSTALL_DIR/.env"
    log ".env 已生成（含 Agnes Key，权限 600）"
}

# ---------- 6. 安装 systemd 服务 ----------
install_service() {
    log "安装 systemd 服务..."
    cat > /etc/systemd/system/${APP_NAME}.service <<EOF
[Unit]
Description=AI Comic Maker (Spring Boot)
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=${INSTALL_DIR}
EnvironmentFile=${INSTALL_DIR}/.env
ExecStart=/usr/bin/java -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod ${INSTALL_DIR}/app.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:${INSTALL_DIR}/logs/stdout.log
StandardError=append:${INSTALL_DIR}/logs/stderr.log

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload
    systemctl enable "$APP_NAME" >/dev/null 2>&1
    log "systemd 服务已安装"
}

# ---------- 7. 启动 / 重启 ----------
start_service() {
    log "启动服务..."
    systemctl restart "$APP_NAME" || { err "服务启动失败，查看日志：journalctl -u $APP_NAME -n 50"; exit 1; }
    # 等待启动
    log "等待应用启动（最多 40 秒）..."
    for i in $(seq 1 40); do
        if curl -sf -o /dev/null http://127.0.0.1:81/ 2>/dev/null; then
            log "应用已启动！"
            return
        fi
        sleep 1
        printf "."
    done
    echo ""
    warn "应用可能还在启动中，查看日志：journalctl -u $APP_NAME -n 50"
}

# ---------- 8. 健康检查 ----------
health_check() {
    log "健康检查..."
    echo ""
    echo "================ 部署结果 ================"
    if curl -sf -o /dev/null -w "首页 HTTP %{http_code}\n" http://127.0.0.1:81/ 2>/dev/null; then
        log "首页访问正常"
    else
        warn "首页访问失败"
    fi
    if curl -sf -o /dev/null -w "Swagger HTTP %{http_code}\n" http://127.0.0.1:81/swagger-ui.html 2>/dev/null; then
        log "Swagger 访问正常"
    else
        warn "Swagger 访问失败"
    fi
    echo ""
    log "服务状态："
    systemctl status "$APP_NAME" --no-pager -n 5 | grep -E "Active:|└─" || true
    echo ""
    echo "=========================================="
    echo " 访问地址：http://<服务器IP>:81/"
    echo " Swagger：http://<服务器IP>:81/swagger-ui.html"
    echo " 查看日志：journalctl -u $APP_NAME -f"
    echo " 重启服务：systemctl restart $APP_NAME"
    echo "=========================================="
}

# ---------- 主流程 ----------
main() {
    log "===== AI 漫剧制作网站部署开始 ====="
    install_deps
    build_frontend
    build_backend
    init_database
    install_app
    install_service
    start_service
    health_check
    log "===== 部署完成 ====="
}

main "$@"
