# Docker Compose 部署

---

## 学习目标

- 理解为什么需要 Docker Compose，掌握多容器编排的核心价值
- 熟练掌握 docker-compose.yml 语法结构：version、services、networks、volumes
- 能够编写完整的 docker-compose.yml 文件，编排 MySQL + Redis + SpringBoot 后端 + Nginx 前端
- 掌握 Docker Compose 常用命令：up、down、ps、logs、exec、restart
- 理解环境变量配置方式，能够在 Compose 中安全地注入配置
- 掌握 healthcheck 健康检查机制，实现服务启动顺序控制
- 能够独立完成从编写配置到一键启动完整应用栈的全流程

---

## 核心知识点

### 1. 为什么需要 Docker Compose？

#### 1.1 是什么

Docker Compose 是 Docker 官方提供的多容器编排工具。通过一个 YAML 配置文件（docker-compose.yml），你可以定义一个完整应用栈中的所有服务，然后用一条命令启动或停止整个应用。

#### 1.2 为什么需要它

**真实场景类比——餐厅后厨的协调**：

> 想象你开了一家餐厅，后厨需要多个岗位协作：切配（MySQL 数据库）、炒菜（SpringBoot 后端）、装盘（Nginx 前端）、冷柜（Redis 缓存）。
>
> 没有 Compose 时，你需要逐个喊人上班："切配到岗！""炒菜到岗！""装盘到岗！"还要确保他们互相认识（网络连接）、每人有自己的工具箱（数据卷）、炒菜的要等切配准备好才能开工（依赖顺序）。一旦有人请假（容器挂了），你得手动一个个处理。
>
> 有了 Compose，你只需一份"排班表"（docker-compose.yml），喊一声"全体上班！"（docker-compose up），所有人按顺序到岗、自动配好对讲机（网络）、领好工具箱（数据卷），后厨立刻运转起来。

#### 1.3 手动管理多容器的痛苦

假设一个项目需要 MySQL + Redis + 后端 + 前端四个容器：

```bash
# 没有 Compose 时，你需要执行这么多命令：

# 1. 启动 MySQL
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=mydb \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  mysql:8.0

# 2. 启动 Redis
docker run -d --name redis \
  -p 6379:6379 \
  -v redis_data:/data \
  redis:7-alpine

# 3. 构建并启动后端（需要等 MySQL 就绪）
docker build -t my-backend ./backend
docker run -d --name backend \
  -p 8080:8080 \
  -e DB_HOST=mysql \
  -e REDIS_HOST=redis \
  --link mysql \
  --link redis \
  my-backend

# 4. 启动前端 Nginx
docker run -d --name frontend \
  -p 80:80 \
  -v ./frontend/dist:/usr/share/nginx/html \
  -v ./nginx.conf:/etc/nginx/conf.d/default.conf \
  nginx:alpine

# 停止时还要一个个停：
docker stop frontend backend redis mysql
docker rm frontend backend redis mysql
```

**有了 Compose，全部浓缩成**：

```bash
docker-compose up -d    # 启动全部
docker-compose down     # 停止并清理全部
```

---

### 2. docker-compose.yml 语法详解

#### 2.1 文件结构总览

```yaml
# docker-compose.yml 顶层结构
version: "3.8"           # Compose 文件格式版本

services:                # 服务定义（核心部分）
  mysql:                 # 服务名称（自定义，容器间通过这个名字通信）
    ...                  # 该服务的配置
  redis:
    ...
  backend:
    ...
  frontend:
    ...

networks:                # 网络定义（可选，Compose 会自动创建默认网络）
  my-network:
    ...

volumes:                 # 数据卷定义（可选）
  mysql_data:
    ...
```

#### 2.2 常用配置项详解

```yaml
version: "3.8"

services:
  # ============================================================
  # 服务名称：mysql
  # 作用：数据库服务
  # ============================================================
  mysql:
    image: mysql:8.0               # 使用 MySQL 8.0 官方镜像
    container_name: tlias-mysql    # 指定容器名称（不指定则自动生成为 项目名_服务名_序号）
    restart: unless-stopped        # 重启策略：除非手动停止，否则自动重启
    # restart 可选值：
    #   no: 不自动重启（默认）
    #   always: 总是重启
    #   on-failure: 仅在退出码非 0 时重启
    #   unless-stopped: 除非手动停止，否则总是重启

    environment:                   # 环境变量，设置 MySQL 初始配置
      MYSQL_ROOT_PASSWORD: 123456  # root 密码
      MYSQL_DATABASE: tlias_db     # 初始化时创建的数据库
      TZ: Asia/Shanghai            # 时区设置

    ports:                         # 端口映射
      - "3306:3306"                # 宿主机 3306 映射到容器 3306
      # 注意：字符串形式 "3306:3306" 避免 YAML 解析为数字

    volumes:                       # 数据卷挂载
      - mysql_data:/var/lib/mysql  # 命名卷：Docker 管理，数据持久化
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
        # 绑定挂载：将宿主机 sql/init.sql 映射到容器初始化目录
        # /docker-entrypoint-initdb.d/ 是 MySQL 镜像的特殊目录
        # 该目录下的 .sql/.sh 文件会在首次启动时自动执行

    healthcheck:                   # 健康检查配置
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p123456"]
        # 测试命令：使用 mysqladmin ping 检查 MySQL 是否就绪
        # 返回 0 表示健康，非 0 表示不健康
      interval: 5s                 # 检查间隔：每 5 秒检查一次
      timeout: 10s                 # 超时时间：命令执行超过 10 秒视为失败
      retries: 10                  # 重试次数：连续失败 10 次后才标记为不健康
      start_period: 30s            # 启动宽限期：容器启动后 30 秒内失败不计入重试

    networks:                      # 网络配置
      - tlias-network              # 加入自定义网络

    command: --default-authentication-plugin=mysql_native_password
      # 覆盖容器默认启动命令，添加额外参数

  # ============================================================
  # 服务名称：redis
  # 作用：缓存服务
  # ============================================================
  redis:
    image: redis:7-alpine          # 使用轻量版 Redis
    container_name: tlias-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data           # Redis 数据持久化到命名卷
    networks:
      - tlias-network
    # Redis 默认不需要密码，生产环境建议配置 requirepass

  # ============================================================
  # 服务名称：backend
  # 作用：SpringBoot 后端服务
  # ============================================================
  backend:
    build:                         # 从 Dockerfile 构建镜像
      context: ./tlias-backend     # 构建上下文：Dockerfile 所在目录
      dockerfile: Dockerfile       # Dockerfile 文件名（默认就是 Dockerfile，可省略）
    container_name: tlias-backend
    restart: unless-stopped
    ports:
      - "8080:8080"

    environment:                   # 注入到容器的环境变量
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tlias_db?serverTimezone=Asia/Shanghai
        # mysql 是服务名，Compose 会自动将其解析为容器 IP
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_AI_OPENAI_API_KEY: ${KIMI_API_KEY:-mock-key}
        # ${KIMI_API_KEY:-mock-key} 是 Shell 变量语法：
        # 如果宿主机环境变量 KIMI_API_KEY 存在，使用它的值
        # 如果不存在，使用默认值 mock-key

    depends_on:                    # 依赖关系
      mysql:
        condition: service_healthy  # 等 MySQL 健康检查通过后才启动
      redis:
        condition: service_started  # 等 Redis 启动后就启动（不检查健康）
        # condition 可选值：
        #   service_started: 服务已启动（默认）
        #   service_healthy: 服务健康检查通过
        #   service_completed_successfully: 服务成功完成（用于一次性任务）

    networks:
      - tlias-network

  # ============================================================
  # 服务名称：frontend
  # 作用：Vue 前端 + Nginx
  # ============================================================
  frontend:
    image: nginx:alpine
    container_name: tlias-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./tlias-frontend/dist:/usr/share/nginx/html
        # 将前端打包后的 dist 目录映射到 Nginx 默认页面目录
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
        # 将自定义 Nginx 配置映射到容器内
    depends_on:
      - backend                    # 等后端启动后再启动
    networks:
      - tlias-network

# ============================================================
# 数据卷定义
# ============================================================
volumes:
  mysql_data:                      # 命名卷声明
    driver: local                  # 使用本地驱动（默认）
  redis_data:
    driver: local

# ============================================================
# 网络定义
# ============================================================
networks:
  tlias-network:                   # 自定义桥接网络
    driver: bridge                 # 使用 bridge 驱动（默认）
    ipam:                          # IP 地址管理（可选）
      config:
        - subnet: 172.20.0.0/16    # 自定义子网
```

#### 2.3 服务间通信原理

```
+----------------------------------------------------------+
|                    Docker Compose 网络                     |
|                   (tlias-network)                         |
|                                                          |
|   +-------------+      +-------------+                   |
|   |   mysql     |      |   redis     |                   |
|   |  172.20.0.2 |      |  172.20.0.3 |                   |
|   |   :3306     |      |   :6379     |                   |
|   +------+------+      +------+------+                   |
|          |                    |                          |
|          | DNS: mysql        | DNS: redis               |
|          v                    v                          |
|   +-------------+      +-------------+                   |
|   |   backend   |      |  frontend   |                   |
|   |  172.20.0.4 |      |  172.20.0.5 |                   |
|   |   :8080     |      |    :80      |                   |
|   +-------------+      +-------------+                   |
|                                                          |
|   容器间通过服务名互相访问：                                |
|   backend 连接 MySQL：jdbc:mysql://mysql:3306/...        |
|   backend 连接 Redis：redis:6379                          |
|   frontend 代理到 backend：http://backend:8080            |
+----------------------------------------------------------+
```

---

### 3. 完整项目编排示例

#### 3.1 版本 A 部署配置（基础版：MySQL + 后端 + 前端）

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: tlias-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: tlias_db
      TZ: Asia/Shanghai
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
    networks:
      - tlias-net

  backend:
    build: ./tlias-backend
    container_name: tlias-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tlias_db?serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - tlias-net

  frontend:
    image: nginx:alpine
    container_name: tlias-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./tlias-frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend
    networks:
      - tlias-net

volumes:
  mysql_data:

networks:
  tlias-net:
    driver: bridge
```

#### 3.2 版本 B 部署配置（完整版：MySQL + Redis + 后端 + 前端）

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: tlias-pro-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: tlias_pro_db
      TZ: Asia/Shanghai
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
      start_period: 30s
    networks:
      - tlias-pro-net
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:7-alpine
    container_name: tlias-pro-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - tlias-pro-net

  backend:
    build: ./tlias-pro-server
    container_name: tlias-pro-backend
    restart: unless-stopped
    ports:
      - "48080:48080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tlias_pro_db?serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_AI_OPENAI_API_KEY: ${KIMI_API_KEY:-mock-key}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - tlias-pro-net

  frontend:
    image: nginx:alpine
    container_name: tlias-pro-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./tlias-pro-frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend
    networks:
      - tlias-pro-net

volumes:
  mysql_pro_data:
  redis_data:

networks:
  tlias-pro-net:
    driver: bridge
```

#### 3.3 Nginx 反向代理配置

```nginx
# nginx.conf - 用于前端容器
server {
    # 监听 80 端口
    listen 80;
    server_name localhost;

    # 前端静态资源
    location / {
        # 根目录指向 Nginx 默认页面目录
        root /usr/share/nginx/html;
        index index.html;
        # try_files 解决 Vue/React 前端路由的 history 模式刷新 404 问题
        # 尝试匹配文件/目录，都找不到则返回 index.html（由前端路由处理）
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        # proxy_pass 将请求转发到后端服务
        # backend 是 docker-compose.yml 中定义的服务名
        # Compose 会自动将其解析为容器 IP
        proxy_pass http://backend:8080/;

        # 转发原始 Host 头
        proxy_set_header Host $host;
        # 转发真实客户端 IP
        proxy_set_header X-Real-IP $remote_addr;
        # 转发代理链（如果有多个代理）
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        # 转发协议（http/https）
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 健康检查端点（可选）
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

---

### 4. Docker Compose 常用命令

```bash
# ============================================================
# 启动与停止
# ============================================================

# 启动所有服务（前台运行，日志输出到终端）
docker-compose up

# 启动所有服务（后台运行，推荐）
docker-compose up -d
# -d = detached，后台模式

# 启动并强制重新构建镜像（代码更新后使用）
docker-compose up -d --build
# --build = 构建镜像，即使镜像已存在

# 启动指定服务
docker-compose up -d mysql backend

# 停止所有服务（保留容器、网络、数据卷）
docker-compose stop

# 停止并删除容器、网络（保留数据卷和镜像）
docker-compose down

# 停止并删除容器、网络、数据卷（⚠️ 数据会丢失）
docker-compose down -v
# -v = volumes，删除命名卷

# 停止并删除容器、网络、数据卷、镜像
docker-compose down --rmi all -v

# ============================================================
# 查看状态
# ============================================================

# 查看所有服务状态
docker-compose ps
# 输出示例：
# NAME              COMMAND                  SERVICE    STATUS     PORTS
# tlias-mysql       "docker-entrypoint.s…"   mysql      running    0.0.0.0:3306->3306/tcp
# tlias-backend     "java -jar app.jar"      backend    running    0.0.0.0:8080->8080/tcp

# 查看服务资源使用
docker-compose top

# ============================================================
# 日志管理
# ============================================================

# 查看所有服务日志
docker-compose logs

# 实时追踪所有服务日志
docker-compose logs -f
# -f = follow，实时追踪

# 查看指定服务日志
docker-compose logs backend

# 查看最后 100 行
docker-compose logs --tail 100 mysql

# 显示时间戳
docker-compose logs -t backend

# ============================================================
# 进入容器执行命令
# ============================================================

# 进入后端容器内的 bash
docker-compose exec backend /bin/sh
# exec = 在运行中的容器执行命令

# 进入 MySQL 容器执行 SQL
docker-compose exec mysql mysql -uroot -p123456 -e "SHOW DATABASES;"

# 进入 Redis 容器使用 redis-cli
docker-compose exec redis redis-cli ping
# 如果返回 PONG，说明 Redis 正常

# ============================================================
# 重启与重建
# ============================================================

# 重启所有服务
docker-compose restart

# 重启指定服务
docker-compose restart backend

# 重新构建镜像（不启动）
docker-compose build

# 重新构建指定服务
docker-compose build backend

# ============================================================
# 其他实用命令
# ============================================================

# 验证配置文件语法是否正确
docker-compose config

# 查看配置文件的完整展开形式（包含环境变量替换后）
docker-compose config --services      # 只显示服务名列表

# 拉取所有服务定义的最新镜像（不启动）
docker-compose pull

# 暂停所有服务（冻结进程）
docker-compose pause

# 恢复暂停的服务
docker-compose unpause

# 查看数据卷
docker-compose volume ls
```

---

### 5. 环境变量配置

#### 5.1 为什么需要环境变量

**真实场景类比**：

> 你开发时用的是本地数据库（密码简单），生产环境用的是云数据库（密码复杂）。如果密码硬编码在 docker-compose.yml 里，每次部署都要改文件，还容易把真实密码提交到 Git。
>
> 环境变量就像餐厅的"今日特供"黑板——每天的内容不同，但黑板（配置文件）不用换。

#### 5.2 环境变量的三种配置方式

```yaml
# 方式一：直接写在 docker-compose.yml 中（适合非敏感配置）
environment:
  SPRING_PROFILES_ACTIVE: dev
  SERVER_PORT: 8080

# 方式二：引用宿主机环境变量（适合敏感配置）
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  API_KEY: ${KIMI_API_KEY}
# 在启动前执行：export MYSQL_ROOT_PASSWORD=123456

# 方式三：使用默认值（推荐）
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-123456}
  # :- 语法：如果环境变量未设置，使用默认值 123456
  API_KEY: ${KIMI_API_KEY:-mock-key}
  # 开发环境不设置 KIMI_API_KEY，自动使用 mock-key

# 方式四：使用 .env 文件（最推荐，敏感信息不进入 Git）
# 在 docker-compose.yml 同级目录创建 .env 文件：
# MYSQL_ROOT_PASSWORD=123456
# KIMI_API_KEY=sk-xxxxxxxx
# REDIS_PASSWORD=
#
# 然后在 docker-compose.yml 中：
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  KIMI_API_KEY: ${KIMI_API_KEY}
```

#### 5.3 .env 文件示例

```bash
# .env 文件 - 放在 docker-compose.yml 同级目录
# 注意：.env 文件不应该提交到 Git，应在 .gitignore 中排除

# 数据库配置
MYSQL_ROOT_PASSWORD=123456
MYSQL_DATABASE=tlias_pro_db

# Redis 配置
REDIS_PASSWORD=

# AI 服务配置
KIMI_API_KEY=sk-your-actual-api-key-here

# 后端配置
SPRING_PROFILES_ACTIVE=dev
BACKEND_PORT=48080

# 前端配置
FRONTEND_PORT=80
```

```bash
# .gitignore 中添加：
echo ".env" >> .gitignore
echo "*.env.local" >> .gitignore
```

---

### 6. 健康检查（Healthcheck）

#### 6.1 是什么

健康检查是 Docker 提供的一种机制，用于定期检查容器内服务是否正常运行。Compose 可以利用健康检查实现服务启动顺序控制。

#### 6.2 为什么需要它

**真实场景类比**：

> 你组织一场演出，主唱（后端服务）必须等音响师（MySQL）把设备调试好才能上台。如果没有健康检查，主唱可能音响还没开就开始唱（后端在数据库还没就绪时就启动，导致连接失败报错）。
>
> 健康检查就是音响师的"准备就绪"手势——主唱看到手势才上台。

#### 6.3 常见服务的健康检查配置

```yaml
# MySQL 健康检查
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p$${MYSQL_ROOT_PASSWORD}"]
  # $${MYSQL_ROOT_PASSWORD} 使用 $$ 转义，避免 Compose 提前解析
  interval: 5s       # 每 5 秒检查一次
  timeout: 10s       # 命令执行超过 10 秒视为失败
  retries: 10        # 连续失败 10 次后标记为 unhealthy
  start_period: 30s  # 启动后 30 秒内失败不计入

# Redis 健康检查
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 5s
  timeout: 3s
  retries: 5

# HTTP 服务健康检查（后端应用）
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  # -f = fail silently，HTTP 错误码时不输出内容
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 60s  # 给 Java 应用足够的启动时间

# 自定义脚本健康检查
healthcheck:
  test: ["CMD-SHELL", "pgrep java >/dev/null || exit 1"]
  # CMD-SHELL 允许使用 Shell 语法
  interval: 10s
  timeout: 5s
  retries: 3
```

#### 6.4 depends_on 的 condition 详解

```yaml
depends_on:
  mysql:
    condition: service_healthy
    # 等待 MySQL 健康检查通过后才启动
    # 避免后端在数据库未就绪时启动导致连接失败

  redis:
    condition: service_started
    # 只等 Redis 容器启动，不检查健康状态
    # 适合启动快、不需要复杂初始化的服务

  init-job:
    condition: service_completed_successfully
    # 等 init-job 成功完成后才启动
    # 适合数据库迁移、数据初始化等一次性任务
```

---

### 7. 项目目录结构

一个典型的使用 Docker Compose 部署的项目目录结构：

```
project-root/                          # 项目根目录
|
├── docker-compose.yml                 # Compose 主配置文件
├── .env                               # 环境变量文件（不提交到 Git）
├── .env.example                       # 环境变量模板（提交到 Git，供参考）
├── nginx.conf                         # Nginx 反向代理配置
|
├── sql/                               # 数据库初始化脚本
│   ├── tlias_init.sql                 # 版本 A 初始化脚本
│   └── tlias-pro_init.sql             # 版本 B 初始化脚本
|
├── tlias-backend/                     # 版本 A 后端（版本 B 同理）
│   ├── Dockerfile                     # 后端镜像构建文件
│   ├── pom.xml                        # Maven 配置
│   └── src/                           # 源代码
│
├── tlias-frontend/                    # 版本 A 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/                           # 源代码
│   └── dist/                          # 打包输出（npm run build 生成）
│
└── tlias-pro-server/                  # 版本 B 后端
    ├── Dockerfile
    ├── pom.xml
    └── src/
```

---

## 动手练习

### 练习 1：一键启动完整应用栈

```bash
# 前置条件：
# 1. Docker 和 Docker Compose 已安装
# 2. 项目目录结构完整
# 3. 前端已打包（dist 目录存在）

# 步骤 1：进入项目根目录
cd /path/to/project

# 步骤 2：确认前端已打包
ls tlias-frontend/dist/index.html
# 如果不存在，先进入前端目录打包：
# cd tlias-frontend && npm run build && cd ..

# 步骤 3：检查配置文件语法
docker-compose config
# 如果输出展开后的配置，说明语法正确

# 步骤 4：拉取基础镜像（可选，加速启动）
docker-compose pull

# 步骤 5：一键启动所有服务（后台运行）
docker-compose up -d

# 步骤 6：查看服务状态
docker-compose ps
# 确认所有服务 STATUS 都是 running

# 步骤 7：查看启动日志（特别是后端）
docker-compose logs -f backend
# 看到 "Started Application in x.xxx seconds" 表示启动成功
# 按 Ctrl + C 退出日志追踪

# 步骤 8：验证各服务
# MySQL：
docker-compose exec mysql mysql -uroot -p123456 -e "SHOW DATABASES;"
# Redis：
docker-compose exec redis redis-cli ping
# 后端接口：
curl http://localhost:8080/api/health
# 前端页面：在浏览器访问 http://localhost

# 步骤 9：停止所有服务
docker-compose down
```

### 练习 2：查看服务日志与故障排查

```bash
# 1. 查看 MySQL 初始化日志
docker-compose logs mysql
# 关注 "ready for connections" 确认 MySQL 已就绪

# 2. 实时追踪后端日志
docker-compose logs -f backend
# 观察是否有异常堆栈

# 3. 搜索所有服务中的 ERROR
docker-compose logs | grep ERROR

# 4. 模拟后端故障，观察重启行为
# 先启动服务
docker-compose up -d
# 手动停止后端容器
docker stop tlias-backend
# 由于配置了 restart: unless-stopped，观察容器是否自动重启
docker-compose ps

# 5. 进入 MySQL 容器查看数据
docker-compose exec mysql mysql -uroot -p123456 -e "SELECT * FROM tlias_db.employee LIMIT 5;"
```

### 练习 3：使用环境变量部署不同环境

```bash
# 1. 创建 .env 文件
cat > .env << 'EOF'
MYSQL_ROOT_PASSWORD=prod_password_123
KIMI_API_KEY=sk-your-production-key
SPRING_PROFILES_ACTIVE=prod
EOF

# 2. 验证环境变量被正确读取
docker-compose config | grep -A 5 environment

# 3. 使用环境变量启动
docker-compose up -d

# 4. 验证 MySQL 密码已生效
docker-compose exec mysql mysql -uroot -pprod_password_123 -e "SELECT 1;"
# 如果使用旧密码 123456 会报错

# 5. 清理
docker-compose down -v
rm .env
```

---

## 常见错误排查

### 安装环境问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 安装环境问题 | `docker-compose: command not found` | Docker Compose 未安装或旧版使用 `docker-compose` 而新版使用 `docker compose` | 新版 Docker 使用 `docker compose`（空格）；检查安装 `docker-compose-plugin` |
| 安装环境问题 | `Version in "./docker-compose.yml" is unsupported` | Compose 文件版本与当前 Docker 版本不兼容 | 降低 version 值，如改为 `"3.3"`；或升级 Docker 版本 |

### 命令执行问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 命令执行问题 | `service "backend" is not running` | 容器启动失败或已退出 | 执行 `docker-compose ps` 查看状态；执行 `docker-compose logs backend` 查看错误原因 |
| 命令执行问题 | `Error starting userland proxy: listen tcp4 0.0.0.0:3306: bind: address already in use` | 宿主机端口已被占用 | 使用 `netstat -tlnp \| grep 3306` 查找占用进程；修改 docker-compose.yml 中的端口映射 |
| 命令执行问题 | `Cannot create container for service backend: not a directory` | volumes 绑定挂载时，宿主机路径是文件但映射成了目录，或反之 | 检查 volumes 配置，确保宿主机路径类型与容器内一致 |

### 配置问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 配置问题 | backend 不断重启（`Restarting (1)`） | 数据库未就绪、连接配置错误、或应用启动报错 | 检查 `depends_on` 和 `condition` 配置；查看 `docker-compose logs backend`；确认环境变量中的数据库地址和端口正确 |
| 配置问题 | 前端页面刷新后 404 | Nginx 未配置 `try_files` 或配置未生效 | 检查 nginx.conf 中是否包含 `try_files $uri $uri/ /index.html;`；确认 nginx.conf 正确挂载到容器内 |
| 配置问题 | 数据库数据丢失 | 使用了匿名卷或没有挂载数据卷 | 使用命名卷并在 volumes 顶层声明；避免使用 `docker-compose down -v` |
| 配置问题 | `depends_on` 不生效，后端先于 MySQL 启动 | 使用了旧版 `depends_on` 语法（没有 condition） | 确保使用 `depends_on: \n  mysql: \n    condition: service_healthy`，且 MySQL 配置了 healthcheck |
| 配置问题 | 环境变量未生效 | .env 文件位置不对或变量名拼写错误 | .env 文件必须与 docker-compose.yml 在同一目录；检查变量名拼写；使用 `docker-compose config` 验证 |

### 网络问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 网络问题 | 容器间无法通信（如 backend 连不上 mysql） | 容器不在同一网络，或服务名拼写错误 | 确认所有服务加入了同一个 networks；检查连接 URL 中的服务名是否与 docker-compose.yml 中一致 |
| 网络问题 | 宿主机无法访问容器服务 | 端口映射未配置或映射错误 | 检查 ports 配置格式 `"宿主机端口:容器端口"`；确认防火墙未阻挡 |
| 网络问题 | `curl: (7) Failed to connect` 访问后端 | 后端未启动完成、端口未监听、或代理配置错误 | 查看后端日志确认启动完成；检查 `docker-compose ps` 端口映射；确认 Nginx 代理地址正确 |

---

## 本节小结

```
+----------------------------------------------------------+
|                 Docker Compose 编排思维导图                |
+----------------------------------------------------------+
|                                                          |
|   +----------------+    +----------------+               |
|   |  核心概念      |    |  配置文件      |               |
|   |  多容器编排    |    |  docker-compose.yml          |
|   |  一键启停      |    |  version      |               |
|   |  服务依赖      |    |  services     |               |
|   |  网络自动      |    |  networks     |               |
|   +----------------+    |  volumes      |               |
|            |            +----------------+               |
|            v                      |                      |
|   +----------------+    +----------------+               |
|   |   关键配置     |    |   常用命令     |               |
|   |  image/build   |    |  up -d        |               |
|   |  ports         |    |  down         |               |
|   |  environment   |    |  ps / logs    |               |
|   |  volumes       |    |  exec         |               |
|   |  depends_on    |    |  restart      |               |
|   |  healthcheck   |    |  --build      |               |
|   +----------------+    +----------------+               |
|                                                          |
|   黄金法则：                                              |
|   1. 数据用命名卷持久化                                   |
|   2. 敏感配置用 .env 文件                                 |
|   3. 数据库等服务配 healthcheck + depends_on              |
|   4. 前端 Nginx 配 try_files 防刷新 404                   |
|   5. 所有服务加入同一自定义网络                             |
|                                                          |
+----------------------------------------------------------+
```

---

## 参考文档

- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Compose 文件参考](https://docs.docker.com/compose/compose-file/)
- [Docker Compose 命令行参考](https://docs.docker.com/compose/reference/)
- [Nginx 反向代理配置指南](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)
- [Spring Boot Docker 部署最佳实践](https://spring.io/guides/topicals/spring-boot-docker/)
