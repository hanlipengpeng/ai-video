# ============ Stage 1: Node 构建 ============
FROM node:20-alpine AS builder
WORKDIR /build
# 先复制 package.json 利用缓存
COPY package.json package-lock.json* ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
# 构建时注入后端 API 地址（生产环境前端通过 nginx 反代后端，用相对路径 /api）
ENV VITE_API_BASE=/api/v1
RUN npm run build

# ============ Stage 2: Nginx 托管 ============
FROM nginx:alpine
# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html
# 自定义 nginx 配置：SPA 路由 + 反代 /api 到后端
RUN cat > /etc/nginx/conf.d/default.conf <<'EOF'
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # SPA history 路由兜底
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反代后端 API
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 300s;
    }

    # 反代静态资源（图片/视频）
    location /storage/ {
        proxy_pass http://backend:8080/storage/;
        proxy_set_header Host $host;
    }
}
EOF
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
