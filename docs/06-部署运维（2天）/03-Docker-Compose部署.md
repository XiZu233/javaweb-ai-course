# Docker Compose 部署

## 学习目标

- 理解 Docker Compose 的作用和语法
- 能够编写 docker-compose.yml 文件
- 能够一键启动完整项目

## 核心知识点

### 1. 为什么需要 Docker Compose

一个完整项目通常包含多个服务：MySQL、Redis、后端、前端。手动逐个启动容器：
- 命令冗长，容易出错
- 服务间的网络和依赖需要手动配置
- 不方便团队协作

Docker Compose 通过一个 YAML 文件定义所有服务，一条命令启动整个应用栈。

### 2. 版本 A 部署配置

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: tlias-mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: tlias_db
    ports:
      - "3306:3306"
    volumes:
      - ./sql/tlias_init.sql:/docker-entrypoint-initdb.d/tlias_init.sql
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p123456"]
      interval: 5s
      timeout: 10s
      retries: 10

  backend:
    build: ./tlias-backend
    container_name: tlias-backend
    ports:
      - "8080:8080"
    environment:
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_USER: root
      MYSQL_PASSWORD: 123456
    depends_on:
      mysql:
        condition: service_healthy

  frontend:
    image: nginx:alpine
    container_name: tlias-frontend
    ports:
      - "80:80"
    volumes:
      - ./tlias-frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend

volumes:
  mysql_data:
```

### 3. 版本 B 部署配置

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: tlias-pro-mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: tlias_pro_db
    ports:
      - "3306:3306"
    volumes:
      - ./sql/tlias-pro_init.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql_pro_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p123456"]
      interval: 5s
      timeout: 10s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: tlias-pro-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  backend:
    build: ./tlias-pro-server
    container_name: tlias-pro-backend
    ports:
      - "48080:48080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tlias_pro_db?serverTimezone=Asia/Shanghai
      SPRING_REDIS_HOST: redis
      SPRING_AI_OPENAI_API_KEY: ${KIMI_API_KEY:-mock-key}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started

  frontend:
    image: nginx:alpine
    container_name: tlias-pro-frontend
    ports:
      - "80:80"
    volumes:
      - ./tlias-pro-frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend

volumes:
  mysql_pro_data:
  redis_data:
```

### 4. 关键配置解析

**healthcheck（健康检查）**：

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p123456"]
  interval: 5s
  timeout: 10s
  retries: 10
```

MySQL 启动需要时间，健康检查确保 MySQL 完全就绪后，后端服务才启动。

**depends_on**：

```yaml
depends_on:
  mysql:
    condition: service_healthy   # 等 MySQL 健康检查通过
```

**数据卷（volumes）**：

命名卷 `mysql_data` 由 Docker 管理，即使容器删除，数据仍然保留。

### 5. 常用命令

```bash
# 启动所有服务（后台运行）
docker-compose up -d

# 查看运行状态
docker-compose ps

# 查看日志
docker-compose logs -f backend

# 重启某个服务
docker-compose restart backend

# 停止并删除容器
docker-compose down

# 停止并删除容器+数据卷（慎用）
docker-compose down -v

# 重新构建镜像
docker-compose up -d --build
```

### 6. Nginx 配置

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**重点**：前端路由使用 history 模式时，需要配置 `try_files` 防止刷新 404。

## 动手练习

### 练习 1：一键启动版本 A

```bash
cd version-a
# 前端先打包
cd tlias-frontend && npm run build && cd ..
# 启动全部服务
docker-compose up -d
# 访问 http://localhost 验证
```

### 练习 2：查看服务日志

1. 查看 MySQL 初始化日志
2. 查看后端启动日志
3. 模拟错误，通过日志定位问题

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| backend 不断重启 | 数据库未就绪或连接配置错误 | 检查 depends_on 和 environment |
| 前端刷新 404 | Nginx 未配置 try_files | 检查 nginx.conf |
| 数据库数据丢失 | 使用了匿名卷或没挂载 | 使用命名卷并正确配置 volumes |
| 端口冲突 | 本地已占用 3306/8080/80 | 修改 docker-compose.yml 端口映射 |

## 本节小结

Docker Compose 让多容器项目的部署变得简单。一条 `docker-compose up -d` 命令，就能启动包含数据库、缓存、后端、前端的完整应用栈，是本地开发和生产部署的利器。

## 参考文档

- [Docker Compose 官方文档](https://docs.docker.com/compose/)

