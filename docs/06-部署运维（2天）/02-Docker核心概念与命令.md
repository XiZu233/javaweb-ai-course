# Docker 核心概念与命令

---

## 学习目标

- 理解 Docker 出现的背景和解决的"环境不一致"痛点
- 清晰区分镜像（Image）、容器（Container）、仓库（Registry）三者关系
- 能够在 Linux 和 Windows 环境下完成 Docker 安装与基础配置
- 熟练掌握 Docker 常用命令：pull、run、ps、exec、stop、rm、images、rmi
- 能够独立编写 Dockerfile，理解 FROM、RUN、COPY、CMD、EXPOSE 等指令
- 掌握数据卷 Volume 的使用，实现容器数据的持久化存储
- 掌握端口映射 `-p` 和容器网络互联机制
- 能够为 SpringBoot 项目编写 Dockerfile 并构建可运行的镜像

---

## 核心知识点

### 1. 为什么需要 Docker？

#### 1.1 是什么

Docker 是一个开源的容器化平台，它允许开发者将应用程序及其所有依赖（代码、运行时、系统工具、库、配置等）打包成一个标准化的单元——容器。

#### 1.2 为什么需要它

**真实场景类比——"在我机器上能跑"的悲剧**：

> 想象你是一个厨师，你在家研发了一道新菜（开发了一个应用）。你用的是家里的炉灶（开发环境：Windows + JDK 8 + MySQL 5.7）。菜品味道完美，你信心满满地把菜谱（代码）交给了餐厅厨房（生产环境：Linux + JDK 11 + MySQL 8.0）。结果厨师按照你的菜谱做出来味道完全不对——因为炉灶火力不同、调料品牌不同、锅具大小不同。
>
> Docker 就是一台"可移动的迷你厨房"：你把家里那套完整的炉灶、调料、锅具全部打包进一个标准化集装箱。无论送到哪个餐厅，打开集装箱，里面的环境和你家一模一样，菜品味道永远一致。

**Docker vs 虚拟机对比**：

```
+---------------------------------------------------------------+
|                      虚拟机架构                                |
|  +----------------+  +----------------+  +----------------+   |
|  |   应用 A        |  |   应用 B        |  |   应用 C        |   |
|  |   依赖库        |  |   依赖库        |  |   依赖库        |   |
|  |   操作系统      |  |   操作系统      |  |   操作系统      |   |
|  | (Guest OS)     |  | (Guest OS)     |  | (Guest OS)     |   |
|  +----------------+  +----------------+  +----------------+   |
|  |        虚拟化层 (Hypervisor)                  |            |
|  +----------------------------------------------------------+ |
|  |              宿主机操作系统 (Host OS)                      | |
|  +----------------------------------------------------------+ |
|  |              宿主机硬件 (CPU/内存/磁盘)                    | |
|  +----------------------------------------------------------+ |
+---------------------------------------------------------------+

+---------------------------------------------------------------+
|                      Docker 容器架构                           |
|  +----------------+  +----------------+  +----------------+   |
|  |   应用 A        |  |   应用 B        |  |   应用 C        |   |
|  |   依赖库        |  |   依赖库        |  |   依赖库        |   |
|  +----------------+  +----------------+  +----------------+   |
|  |        Docker 引擎 (共享宿主机内核)                         |   |
|  +----------------------------------------------------------+ |
|  |              宿主机操作系统 (Host OS)                      | |
|  +----------------------------------------------------------+ |
|  |              宿主机硬件 (CPU/内存/磁盘)                    | |
|  +----------------------------------------------------------+ |
+---------------------------------------------------------------+
```

| 特性 | 虚拟机 (VM) | Docker 容器 |
|------|-------------|-------------|
| 启动速度 | 分钟级（需要启动完整操作系统） | 秒级（共享宿主机内核） |
| 资源占用 | GB 级（每个 VM 一个完整 OS） | MB 级（只包含应用和依赖） |
| 隔离级别 | 硬件级隔离 | 进程级隔离 |
| 性能损耗 | 高（需要硬件虚拟化） | 低（接近原生性能） |
| 镜像大小 | 数 GB | 数十 MB 到数百 MB |
| 密度 | 一台机器跑十几个 VM | 一台机器跑上百个容器 |

---

### 2. 核心概念：镜像、容器、仓库

#### 2.1 镜像（Image）—— 类（Class）

**是什么**：镜像是一个只读的模板，包含了运行应用所需的所有内容：操作系统文件系统、应用程序代码、运行时环境、系统工具、系统库等。

**为什么需要它**：镜像就是"标准化模板"。就像蛋糕模具，同一个模具可以做出无数个一模一样的蛋糕。

**类比**：

> 镜像 = 蛋糕模具（类 Class）
> 容器 = 用模具做出来的蛋糕（对象 Object）
> 仓库 = 蛋糕模具商店（应用商店）

```bash
# 查看本地已有的镜像
docker images
# 输出示例：
# REPOSITORY   TAG       IMAGE ID       CREATED         SIZE
# nginx        latest    605c77e624dd   2 weeks ago     141MB
# mysql        8.0       3218b38490ce   3 weeks ago     538MB
# openjdk      17        3f1f7b9c8d2e   1 month ago     470MB
```

#### 2.2 容器（Container）—— 对象（Object）

**是什么**：容器是镜像的运行实例。它是一个独立的、隔离的进程，拥有自己独立的文件系统、网络接口和进程空间。

**为什么需要它**：容器让应用运行在一个独立、一致的环境中，不受宿主机环境变化的影响。

```bash
# 查看运行中的容器
docker ps
# 输出示例：
# CONTAINER ID   IMAGE     COMMAND                  CREATED         STATUS         PORTS                NAMES
# a1b2c3d4e5f6   nginx     "/docker-entrypoint.…"   5 minutes ago   Up 5 minutes   0.0.0.0:80->80/tcp   my-nginx
```

#### 2.3 仓库（Registry）—— 应用商店

**是什么**：仓库是存储和分发镜像的服务。最著名的公共仓库是 Docker Hub（https://hub.docker.com）。

**为什么需要它**：就像手机应用商店一样，开发者可以把镜像推送到仓库，其他人可以拉取使用。

```bash
# 从 Docker Hub 拉取镜像
docker pull nginx:latest          # pull = 拉取，latest = 最新版本标签
docker pull mysql:8.0             # 拉取 MySQL 8.0 版本
docker pull redis:7-alpine        # 拉取基于 Alpine Linux 的轻量版 Redis
```

**镜像命名规范**：

```
[仓库地址/]用户名/镜像名:标签

# 示例：
nginx:latest                    # 官方镜像，省略仓库地址和用户名
mysql:8.0                       # 官方 MySQL 镜像，8.0 标签
ubuntu:22.04                    # Ubuntu 22.04 版本
registry.cn-hangzhou.aliyuncs.com/library/nginx:latest   # 阿里云镜像仓库
```

---

### 3. Docker 安装

#### 3.1 Linux（以 Ubuntu 为例）安装

```bash
# 步骤 1：更新软件包索引
sudo apt update

# 步骤 2：安装必要的依赖包
sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
# apt-transport-https: 允许 apt 使用 HTTPS
# ca-certificates: SSL 证书
# curl: 下载工具
# gnupg: 密钥管理
# lsb-release: 获取系统版本信息

# 步骤 3：添加 Docker 官方 GPG 密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
# -fsSL: f=失败不显示进度, s=静默, S=错误时显示, L=跟随重定向

# 步骤 4：添加 Docker 软件源
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null <<EOF
deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable
EOF
# tee: 同时输出到屏幕和文件
# $(lsb_release -cs): 获取系统代号，如 focal、jammy

# 步骤 5：再次更新软件包索引
sudo apt update

# 步骤 6：安装 Docker Engine
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
# docker-ce: Docker 社区版引擎
# docker-ce-cli: Docker 命令行工具
# containerd.io: 容器运行时
# docker-compose-plugin: Docker Compose 插件

# 步骤 7：验证安装
sudo docker --version
# 输出示例：Docker version 24.0.7, build afdd53b

# 步骤 8：将当前用户加入 docker 组（免 sudo 运行 docker）
sudo usermod -aG docker $USER
# -a = append, -G = 添加到附加组
# $USER = 当前用户名
# 注意：需要退出并重新登录，或执行 newgrp docker 使权限生效

# 步骤 9：验证免 sudo 运行
docker run hello-world
# 如果看到 "Hello from Docker!" 说明安装成功
```

#### 3.2 Windows 安装

```
Windows 安装步骤：
1. 访问 https://www.docker.com/products/docker-desktop/
2. 下载 Docker Desktop for Windows
3. 双击安装包，按向导完成安装
4. 安装过程中勾选 "Use WSL 2 instead of Hyper-V"（推荐）
5. 安装完成后重启电脑
6. 打开 Docker Desktop，等待状态栏显示 "Docker Desktop is running"
7. 打开 PowerShell，执行 docker --version 验证

注意事项：
- Windows 10/11 专业版/企业版：支持 Hyper-V 或 WSL2 后端
- Windows 10/11 家庭版：只能使用 WSL2 后端
- 需要在 BIOS 中开启虚拟化（Intel VT-x 或 AMD-V）
```

#### 3.3 配置国内镜像加速

```bash
# 编辑 Docker 配置文件
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json > /dev/null <<EOF
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
EOF
# registry-mirrors: 镜像加速器地址，从国内服务器拉取镜像，速度更快

# 重启 Docker 服务使配置生效
sudo systemctl daemon-reload
sudo systemctl restart docker
```

---

### 4. Docker 常用命令

#### 4.1 镜像操作命令

```bash
# docker pull = 从仓库拉取镜像到本地
docker pull nginx:latest              # 拉取最新版 Nginx
docker pull nginx:1.25                # 拉取指定版本
docker pull redis:7-alpine            # 拉取轻量版（Alpine 基于 musl，体积更小）

# docker images = 查看本地所有镜像
docker images                         # 列出所有镜像
docker images | grep nginx            # 筛选 Nginx 相关镜像
docker images -q                      # -q = quiet，只显示 IMAGE ID（用于批量操作）

# docker rmi = Remove Image，删除镜像
docker rmi nginx:latest               # 删除指定镜像
docker rmi $(docker images -q)        # 删除所有镜像（⚠️ 危险）
docker image prune                    # 删除所有 dangling（悬空）镜像
docker image prune -a                 # 删除所有未使用的镜像

# docker inspect = 查看镜像详细信息
docker inspect nginx:latest           # 查看镜像的元数据、层信息、环境变量等

# docker history = 查看镜像构建历史
docker history nginx:latest           # 查看每一层是怎么构建的
```

#### 4.2 容器生命周期命令

```bash
# docker run = 创建并启动容器（最常用）
docker run -d -p 80:80 --name my-nginx nginx:latest
# -d = detached，后台运行
# -p 80:80 = 端口映射，宿主机80端口映射到容器80端口
# --name my-nginx = 给容器起个名字（方便后续操作）
# nginx:latest = 使用的镜像

# 更多 run 参数示例：
docker run -it --rm ubuntu:22.04 bash
# -i = interactive，保持 STDIN 打开
# -t = tty，分配一个伪终端
# --rm = 容器停止后自动删除（适合临时测试）
# bash = 在容器内执行的命令

docker run -d \
  --name my-mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=mydb \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  mysql:8.0
# -e = environment，设置环境变量
# -v = volume，挂载数据卷

# docker ps = 查看容器状态
docker ps                             # 查看运行中的容器
docker ps -a                          # -a = all，查看所有容器（包括已停止的）
docker ps -q                          # 只显示容器 ID
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
# --format = 自定义输出格式

# docker start / stop / restart = 启停容器
docker start my-nginx                 # 启动已停止的容器
docker stop my-nginx                  # 优雅停止容器（发送 SIGTERM，等待 10 秒后 SIGKILL）
docker stop -t 30 my-nginx            # -t = timeout，等待 30 秒
docker restart my-nginx               # 重启容器
docker kill my-nginx                  # 强制停止（直接 SIGKILL）

# docker rm = Remove，删除容器
docker rm my-nginx                    # 删除已停止的容器
docker rm -f my-nginx                 # -f = force，强制删除运行中的容器
docker rm $(docker ps -aq)            # 删除所有容器（⚠️ 危险）
docker container prune                # 删除所有已停止的容器

# docker logs = 查看容器日志
docker logs my-nginx                  # 查看全部日志
docker logs -f my-nginx               # -f = follow，实时追踪日志
docker logs --tail 100 my-nginx       # 只查看最后 100 行
docker logs -t my-nginx               # -t = timestamp，显示时间戳

# docker exec = 在运行中的容器内执行命令
docker exec -it my-nginx /bin/bash    # 进入容器内部的 bash 交互式终端
# -i = interactive
# -t = tty
docker exec my-nginx nginx -t         # 在容器内执行 nginx -t（测试配置文件）
docker exec my-nginx ls /usr/share/nginx/html   # 查看容器内文件

# docker inspect = 查看容器详细信息
docker inspect my-nginx               # 查看容器的 IP、挂载、环境变量等

# docker stats = 查看容器资源使用情况
docker stats                          # 实时显示所有容器的 CPU、内存、网络、磁盘 I/O
```

---

### 5. Dockerfile 编写

#### 5.1 是什么

Dockerfile 是一个文本文件，包含了一系列指令，Docker 引擎按照这些指令一步一步构建出镜像。

#### 5.2 为什么需要它

**真实场景类比**：

> Dockerfile 就像一份详细的食谱。你不仅告诉别人"做蛋糕"，还一步步说明：用什么面粉（FROM 基础镜像）、加多少糖（RUN 安装依赖）、怎么搅拌（COPY 代码）、烤多久（CMD 启动命令）。任何人拿到这份食谱，都能做出一模一样的蛋糕。

#### 5.3 常用指令详解

| 指令 | 作用 | 示例 |
|------|------|------|
| FROM | 指定基础镜像，每个 Dockerfile 必须以 FROM 开头 | `FROM openjdk:17-jdk-alpine` |
| WORKDIR | 设置工作目录，后续指令都在此目录执行 | `WORKDIR /app` |
| COPY | 从宿主机复制文件到镜像 | `COPY target/app.jar app.jar` |
| ADD | 类似 COPY，但支持自动解压 tar 和下载 URL | `ADD https://example.com/file.tar.gz /tmp/` |
| RUN | 在镜像构建时执行命令（每行 RUN 产生一个新层） | `RUN apt-get update && apt-get install -y curl` |
| ENV | 设置环境变量 | `ENV JAVA_OPTS="-Xmx512m"` |
| EXPOSE | 声明容器运行时监听的端口（仅文档作用，不实际开放） | `EXPOSE 8080` |
| CMD | 容器启动时执行的默认命令（可被覆盖） | `CMD ["java", "-jar", "app.jar"]` |
| ENTRYPOINT | 容器启动时执行的命令（不可被覆盖，配合 CMD 使用） | `ENTRYPOINT ["java", "-jar"]` |
| VOLUME | 声明挂载点 | `VOLUME ["/data"]` |

#### 5.4 SpringBoot 项目 Dockerfile 示例

```dockerfile
# ============================================
# 阶段一：构建阶段（多阶段构建，减小最终镜像体积）
# ============================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
# FROM: 使用 Maven + JDK 17 的 Alpine 版本作为构建环境
# AS builder: 给这个阶段起名为 builder，后续可以引用

# 设置工作目录，后续操作都在 /app 目录下进行
WORKDIR /app

# 先复制 pom.xml 和依赖配置，利用 Docker 层缓存
# 如果依赖没有变化，这一步不会重新执行，加速构建
COPY pom.xml .
COPY src ./src

# 执行 Maven 打包，跳过测试以加速
# -B = batch mode，非交互模式
# -DskipTests = 跳过测试
RUN mvn clean package -B -DskipTests

# ============================================
# 阶段二：运行阶段（只包含 JRE，体积更小）
# ============================================
FROM eclipse-temurin:17-jre-alpine
# 使用仅包含 JRE 的轻量镜像，比完整 JDK 小很多

# 设置工作目录
WORKDIR /app

# 从构建阶段复制打包好的 jar 文件
# --from=builder: 从名为 builder 的阶段复制文件
COPY --from=builder /app/target/*.jar app.jar

# 设置时区为东八区（中国时间）
ENV TZ=Asia/Shanghai

# 声明应用监听的端口（仅文档说明，实际映射用 -p 参数）
EXPOSE 8080

# 容器启动时运行的命令
# 使用数组形式（exec 形式），确保信号正确传递
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 5.5 简单版 Dockerfile（适合已打好 jar 包的场景）

```dockerfile
# 使用 OpenJDK 17 的 Alpine 版本作为基础镜像
# Alpine Linux 是一个轻量级发行版，基础镜像只有 5MB 左右
FROM openjdk:17-jdk-alpine

# 设置工作目录为 /app
WORKDIR /app

# 将宿主机 target 目录下的 jar 包复制到镜像的 /app 目录，重命名为 app.jar
COPY target/tlias-backend-1.0.0.jar app.jar

# 声明容器暴露 8080 端口（配合 docker run -p 使用）
EXPOSE 8080

# 容器启动时执行的命令：运行 Java 应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 5.6 构建镜像

```bash
# 进入项目根目录（Dockerfile 所在目录）
cd /path/to/project

# 构建镜像
# -t = tag，给镜像打标签（名称:版本）
# . = 构建上下文，Docker 引擎会将当前目录发送给守护进程
# -f = 指定 Dockerfile 路径（如果文件名不是 Dockerfile）
docker build -t tlias-backend:1.0 .

# 构建时输出详细过程
docker build -t tlias-backend:1.0 . --progress=plain

# 不使用缓存（强制重新构建）
docker build -t tlias-backend:1.0 . --no-cache

# 查看构建好的镜像
docker images | grep tlias-backend

# 运行镜像
 docker run -d \
  --name tlias-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  tlias-backend:1.0
```

---

### 6. 数据持久化：Volume

#### 6.1 是什么

容器中的数据默认存储在容器的可写层中，当容器删除时，数据也随之消失。Volume（数据卷）将数据存储在宿主机上，实现数据持久化。

#### 6.2 为什么需要它

**真实场景类比**：

> 容器就像一次性的纸杯，用完就扔。但你在纸杯里泡的茶（数据）不能扔。Volume 就是一个外挂的保温杯——纸杯可以换，但保温杯里的茶一直留着。

#### 6.3 Volume 的三种类型

```
+----------------------------------------------------------+
|                    Docker 数据卷类型                       |
+----------------------------------------------------------+
|                                                          |
|  1. Bind Mount（绑定挂载）                                |
|     宿主机路径 : 容器路径                                  |
|     例：-v /host/data:/container/data                    |
|     特点：直接映射宿主机目录，适合开发环境                  |
|                                                          |
|  2. Named Volume（命名卷）                                |
|     卷名 : 容器路径                                       |
|     例：-v mysql_data:/var/lib/mysql                     |
|     特点：由 Docker 管理，适合生产环境数据持久化            |
|                                                          |
|  3. tmpfs Mount（内存挂载）                               |
|     例：--tmpfs /tmp                                     |
|     特点：数据存储在内存中，重启后丢失，适合敏感临时数据    |
|                                                          |
+----------------------------------------------------------+
```

```bash
# 绑定挂载：将宿主机目录映射到容器
docker run -d \
  --name my-mysql \
  -v /host/mysql/data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  mysql:8.0
# /host/mysql/data 是宿主机目录，/var/lib/mysql 是容器内 MySQL 数据目录

# 命名卷：由 Docker 管理存储位置
docker run -d \
  --name my-mysql \
  -v mysql_data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  mysql:8.0
# mysql_data 是卷名，Docker 会自动在 /var/lib/docker/volumes/ 下创建

# 查看所有数据卷
docker volume ls

# 查看数据卷详情
docker volume inspect mysql_data

# 删除未使用的数据卷
docker volume prune
```

---

### 7. 端口映射与网络

#### 7.1 端口映射 `-p`

```bash
# 端口映射语法：-p 宿主机端口:容器端口

# 标准映射
docker run -d -p 80:80 nginx               # 宿主机 80 映射到容器 80
docker run -d -p 8080:8080 myapp           # 宿主机 8080 映射到容器 8080

# 随机分配宿主机端口
docker run -d -p 80 nginx                  # Docker 随机分配一个宿主机高端口

# 绑定特定 IP
docker run -d -p 127.0.0.1:8080:8080 myapp    # 只允许本机访问
docker run -d -p 0.0.0.0:8080:8080 myapp      # 允许所有网卡访问（默认）

# 映射多个端口
docker run -d \
  -p 80:80 \
  -p 443:443 \
  nginx

# 查看端口映射
docker port my-nginx
```

#### 7.2 容器网络

```bash
# 查看 Docker 网络列表
docker network ls
# 输出示例：
# NETWORK ID     NAME      DRIVER    SCOPE
# abc123def456   bridge    bridge    local      # 默认桥接网络
# def456ghi789   host      host      local      # 共享宿主机网络
# ghi789jkl012   none      null      local      # 无网络

# 创建自定义网络
docker network create my-network
# 自定义网络支持容器名作为 DNS 解析，容器间可以通过名字互相访问

# 运行容器并指定网络
docker run -d --name mysql --network my-network -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0
docker run -d --name backend --network my-network myapp:1.0
# 在 backend 容器内，可以通过 mysql 这个主机名访问数据库
# 例如：jdbc:mysql://mysql:3306/mydb

# 将已运行的容器加入网络
docker network connect my-network my-nginx

# 查看网络详情
docker network inspect my-network

# 删除网络
docker network rm my-network
```

#### 7.3 容器互联演进

```
早期方式：--link（已废弃，不推荐）
  docker run --link mysql:db myapp
  # 通过环境变量注入连接信息，不够灵活

现代方式：自定义 Bridge 网络
  docker network create app-net
  docker run --name mysql --network app-net mysql:8.0
  docker run --name backend --network app-net myapp
  # 容器间通过容器名直接通信，支持自动 DNS 解析
```

---

## 动手练习

### 练习 1：运行 Nginx 容器

```bash
# 步骤 1：拉取 Nginx 官方镜像
docker pull nginx:latest

# 步骤 2：运行 Nginx 容器
docker run -d \
  --name my-nginx \
  -p 80:80 \
  nginx:latest
# -d: 后台运行
# --name: 命名容器
# -p 80:80: 将宿主机的 80 端口映射到容器的 80 端口

# 步骤 3：验证容器运行状态
docker ps

# 步骤 4：在浏览器访问 http://localhost（Linux）或 http://localhost:80（Windows/Mac）
# 应该看到 "Welcome to nginx!" 页面

# 步骤 5：进入容器内部查看文件
docker exec -it my-nginx /bin/bash
# 在容器内执行：
ls /usr/share/nginx/html/        # 查看 Nginx 默认页面文件
cat /etc/nginx/nginx.conf        # 查看 Nginx 主配置
exit                             # 退出容器

# 步骤 6：停止并删除容器
docker stop my-nginx
docker rm my-nginx
```

### 练习 2：为 SpringBoot 项目构建 Docker 镜像

```bash
# 前置条件：项目已用 Maven 打包，target 目录下有 jar 文件

# 步骤 1：确认 jar 文件存在
ls target/*.jar
# 输出：target/tlias-backend-1.0.0.jar

# 步骤 2：在项目根目录创建 Dockerfile
cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/tlias-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# 步骤 3：构建镜像
docker build -t tlias-backend:1.0 .

# 步骤 4：查看构建好的镜像
docker images | grep tlias-backend

# 步骤 5：运行容器（假设需要连接 MySQL）
docker run -d \
  --name tlias-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/tlias_db \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  tlias-backend:1.0
# host.docker.internal 是 Docker 提供的特殊 DNS，指向宿主机

# 步骤 6：查看日志确认启动成功
docker logs -f tlias-app

# 步骤 7：测试接口
curl http://localhost:8080/api/health
```

### 练习 3：使用 Volume 持久化 MySQL 数据

```bash
# 步骤 1：创建命名卷
docker volume create mysql_data

# 步骤 2：运行 MySQL 容器，挂载数据卷
docker run -d \
  --name my-mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=testdb \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  mysql:8.0

# 步骤 3：连接 MySQL 创建一些数据
docker exec -it my-mysql mysql -uroot -p123456 -e "CREATE TABLE testdb.users (id INT, name VARCHAR(50)); INSERT INTO testdb.users VALUES (1, 'Alice');"

# 步骤 4：停止并删除容器
docker stop my-mysql
docker rm my-mysql

# 步骤 5：重新创建容器，使用同一个数据卷
docker run -d \
  --name my-mysql-new \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  mysql:8.0

# 步骤 6：验证数据是否还在
docker exec -it my-mysql-new mysql -uroot -p123456 -e "SELECT * FROM testdb.users;"
# 应该能看到之前插入的 Alice 数据，证明 Volume 持久化生效
```

---

## 常见错误排查

### 安装环境问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 安装环境问题 | `Cannot connect to the Docker daemon` | Docker 服务未启动，或当前用户不在 docker 组 | 执行 `sudo systemctl start docker` 启动服务；或执行 `sudo usermod -aG docker $USER` 后重新登录 |
| 安装环境问题 | `permission denied while trying to connect to Docker daemon` | 用户权限不足 | 在命令前加 `sudo`，或将用户加入 docker 组后重新登录 |
| 安装环境问题 | Windows 安装后提示 "WSL2 installation is incomplete" | WSL2 内核未安装或虚拟化未开启 | 在 PowerShell 执行 `wsl --install`；在 BIOS 中开启 Intel VT-x / AMD-V |

### 命令执行问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 命令执行问题 | `Error response from daemon: pull access denied` | 镜像名称错误或私有镜像未登录 | 检查镜像名称拼写；如果是私有镜像，先执行 `docker login` |
| 命令执行问题 | `Error response from daemon: manifest for xxx not found` | 镜像标签不存在 | 使用 `docker pull 镜像名` 不带标签拉取 latest，或到 Docker Hub 确认正确标签 |
| 命令执行问题 | `docker: Error response from daemon: Conflict. The container name is already in use` | 容器名已被占用 | 使用 `docker rm 容器名` 删除旧容器，或用 `--name` 指定新名称 |
| 命令执行问题 | `Error response from daemon: No such image` | 本地没有这个镜像 | 先执行 `docker pull 镜像名` 拉取镜像 |

### 配置问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 配置问题 | `port is already allocated` | 宿主机端口已被其他进程占用 | 使用 `netstat -tlnp \| grep 端口号` 查找占用进程；修改 `-p` 映射为其他端口 |
| 配置问题 | Dockerfile 构建时 `COPY failed: file not found` | COPY 的源文件路径错误或构建上下文不包含该文件 | 确认文件存在于构建上下文目录；检查 Dockerfile 中 COPY 的源路径是否正确 |
| 配置问题 | 容器启动后立即退出（`Exited (1)`） | 主进程执行完或启动报错 | 使用 `docker logs 容器名` 查看错误日志；检查 CMD/ENTRYPOINT 是否正确 |
| 配置问题 | `exec format error` | 镜像架构与宿主机不匹配（如 ARM 镜像在 x86 运行） | 拉取对应架构的镜像，如 `docker pull --platform linux/amd64 镜像名` |

### 网络问题

| 阶段 | 错误现象 | 原因 | 解决方案 |
|------|----------|------|----------|
| 网络问题 | 浏览器访问 `localhost:8080` 无响应 | 端口映射错误、容器未运行、或应用未监听 0.0.0.0 | 检查 `docker ps` 确认容器运行中；检查 `-p` 参数；确认应用绑定 `0.0.0.0` 而非 `127.0.0.1` |
| 网络问题 | 容器内无法访问外网 | Docker 网络配置问题或防火墙 | 检查 `docker network inspect bridge`；检查宿主机防火墙 `iptables -L`；尝试重启 Docker |
| 网络问题 | 容器间无法通过容器名通信 | 使用了默认 bridge 网络（不支持 DNS） | 创建自定义网络 `docker network create xxx`，容器加入同一网络 |
| 网络问题 | 镜像拉取速度极慢或超时 | 连接 Docker Hub 网络慢 | 配置国内镜像加速器，编辑 `/etc/docker/daemon.json` 添加 registry-mirrors |

---

## 本节小结

```
+----------------------------------------------------------+
|                   Docker 核心概念思维导图                  |
+----------------------------------------------------------+
|                                                          |
|   +----------------+    +----------------+               |
|   |    镜像 Image   |    |    容器 Container |            |
|   |   (只读模板)    |    |   (运行实例)      |            |
|   |   类 Class      |    |   对象 Object     |            |
|   +----------------+    +----------------+               |
|            |                      |                      |
|            v                      v                      |
|   +----------------+    +----------------+               |
|   |   仓库 Registry |    |   Dockerfile    |              |
|   |   (应用商店)    |    |   (构建食谱)    |              |
|   +----------------+    +----------------+               |
|                                                          |
|   +----------------+    +----------------+               |
|   |   数据持久化    |    |   网络与端口    |              |
|   |   Volume 卷    |    |   -p 端口映射   |              |
|   |   Bind Mount   |    |   自定义网络    |              |
|   +----------------+    +----------------+               |
|                                                          |
|   核心命令：                                              |
|   pull → images → run → ps → exec → logs → stop → rm    |
|   build -t → rmi                                        |
|                                                          |
|   黄金法则：一个容器一个进程；数据用 Volume；配置用环境变量 |
|                                                          |
+----------------------------------------------------------+
```

---

## 参考文档

- [Docker 官方文档](https://docs.docker.com/)
- [Dockerfile 参考手册](https://docs.docker.com/engine/reference/builder/)
- [Docker Hub 镜像仓库](https://hub.docker.com/)
- [Docker 从入门到实践（中文）](https://yeasy.gitbook.io/docker_practice/)
- [Spring Boot with Docker 官方指南](https://spring.io/guides/gs/spring-boot-docker/)
