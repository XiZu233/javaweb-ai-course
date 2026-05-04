# Docker 环境准备与容器化部署

> Docker 是现代化开发的"环境标准化神器"。
> 学完本节，你将能够一键启动 MySQL、Redis、Nginx 等全套开发环境，彻底告别"在我电脑上能跑"的尴尬。
> 即使你是第一次接触容器技术，也不用担心——我们会从最基础的安装开始，手把手带你完成环境配置。

---

## 一、学习目标

完成本节学习后，你将能够：

- **理解 Docker 的核心价值**：清楚 Docker 解决了什么问题，为什么现代开发离不开它。
- **独立完成 Docker Desktop 的安装与配置**：在 Windows、Mac 或 Linux 上成功安装并运行 Docker。
- **使用 Docker 一键启动 MySQL 和 Redis**：掌握 `docker run` 命令的常用参数，能够独立启动数据库服务。
- **理解数据卷（Volume）机制**：知道如何保证容器删除后数据不丢失，正确配置持久化存储。
- **使用 Docker Compose 编排多容器应用**：能够编写和运行 `docker-compose.yml` 文件，一键启动完整技术栈。
- **排查常见 Docker 问题**：遇到端口冲突、内存不足、WSL2 配置等问题时能够独立解决。

---

## 二、核心知识点

### 2.1 为什么需要 Docker？——环境配置的噩梦

**没有 Docker 的日子（真实场景）**：

想象一个 5 人开发团队，每个人电脑上安装的开发环境各不相同：

```
+---------------------------------------------------------------+
|                  "在我电脑上能跑"的悲剧                          |
+---------------------------------------------------------------+
|                                                               |
|  小明：Windows 10 + MySQL 5.7 + JDK 8 + Node 14               |
|    |                                                          |
|    v                                                          |
|  项目在小明电脑上跑得好好的                                     |
|    |                                                          |
|    v                                                          |
|  小红：MacBook + MySQL 8.0 + JDK 11 + Node 16                 |
|    |                                                          |
|    v                                                          |
|  小红拉下代码，启动报错：                                       |
|  "SQLException: Unknown column 'xxx' in 'field list'"         |
|  原因：MySQL 5.7 和 8.0 的语法差异                             |
|    |                                                          |
|    v                                                          |
|  小刚：Windows 11 + MySQL 8.0 + JDK 17 + Node 18              |
|    |                                                          |
|    v                                                          |
|  小刚拉下代码，启动报错：                                       |
|  "Unsupported class file major version 61"                    |
|  原因：JDK 17 编译的代码无法在 JDK 8 运行                       |
|    |                                                          |
|    v                                                          |
|  小李：Ubuntu + MySQL 8.0 + JDK 8 + Node 18                   |
|    |                                                          |
|    v                                                          |
|  小李拉下代码，前端构建报错：                                    |
|  "Error: Cannot find module 'xxx'"                            |
|  原因：Node 版本不兼容，依赖包安装失败                          |
|                                                               |
|  结果：5 个人花了 3 天时间配环境，还没开始写业务代码              |
+---------------------------------------------------------------+
```

**Docker 如何解决这些问题**：

| 痛点 | 没有 Docker 时 | 有了 Docker 后 |
|------|-------------|--------------|
| 环境不一致 | 每个人手动安装，版本各异 | 用同一个镜像，环境完全一致 |
| 安装复杂 | MySQL/Redis/Nginx 逐个安装配置 | 一条命令启动，无需手动配置 |
| 环境冲突 | 本地已有 MySQL 占用了 3306 端口 | 容器隔离，互不影响 |
| 清理困难 | 卸载软件残留注册表和文件 | `docker rm` 彻底删除，不留痕迹 |
| 新成员加入 | 花一天时间配环境 | `docker-compose up -d`，5 分钟搞定 |
| 生产部署 | 开发环境能跑，生产环境报错 | 开发/测试/生产用同一个镜像 |

**一句话总结**：Docker 把你的应用和它需要的一切（操作系统、运行时、依赖库）打包成一个"集装箱"，在任何地方打开都能一模一样地运行。

### 2.2 Docker 核心概念

#### 2.2.1 镜像（Image）vs 容器（Container）

这是 Docker 中最基础也是最重要的两个概念：

```
+---------------------------------------------------------------+
|              镜像 vs 容器 类比理解                              |
+---------------------------------------------------------------+
|                                                               |
|   镜像（Image）          容器（Container）                      |
|   = 类（Class）          = 对象（Object）                       |
|   = 安装包（.exe）        = 运行中的程序                        |
|   = 食谱                 = 做出来的菜                           |
|   = 蓝图                 = 盖好的房子                           |
|                                                               |
|   镜像是一个只读的模板，        容器是镜像的运行实例，            |
|   包含了运行应用所需的          可以被创建、启动、停止、删除。     |
|   所有文件和配置。              多个容器可以基于同一个镜像创建。    |
|                                                               |
|   例如：mysql:8.0 镜像         例如：tlias-mysql 容器            |
|   包含了 MySQL 8.0 的          正在运行的一个 MySQL 8.0 实例     |
|   所有程序文件和默认配置。       有自己的端口映射和数据存储。       |
|                                                               |
+---------------------------------------------------------------+
```

| 概念 | 英文 | 说明 | 类比 |
|------|------|------|------|
| 镜像 | Image | 只读的模板，包含应用和运行环境 | 类 / 安装包 / 食谱 |
| 容器 | Container | 镜像的运行实例，可以被创建和销毁 | 对象 / 运行中的程序 / 做出来的菜 |
| 仓库 | Registry | 存放镜像的地方，如 Docker Hub | 应用商店 |
| 数据卷 | Volume | 容器外的持久化存储 | 外挂硬盘 |

#### 2.2.2 Docker 架构图

```
+---------------------------------------------------------------+
|                    Docker 架构示意图                            |
+---------------------------------------------------------------+
|                                                               |
|  +------------------+                                         |
|  |   Docker Client  |  <-- 你输入命令的地方（终端）            |
|  |   （docker CLI）  |                                         |
|  +--------+---------+                                         |
|           | REST API                                          |
|           v                                                   |
|  +------------------+                                         |
|  |  Docker Daemon   |  <-- Docker 后台服务，管理所有容器       |
|  |   （dockerd）    |                                         |
|  +--------+---------+                                         |
|           |                                                   |
|     +-----+-----+                                             |
|     |           |                                             |
|     v           v                                             |
|  +------+   +--------+                                        |
|  |Images|   |Containers|  <-- 镜像和容器都由 Daemon 管理       |
|  +------+   +--------+                                        |
|     |           |                                             |
|     v           v                                             |
|  +------------------+                                         |
|  |     Volumes      |  <-- 数据卷，持久化存储                  |
|  +------------------+                                         |
|                                                               |
+---------------------------------------------------------------+
```

### 2.3 Docker Desktop 安装

#### 2.3.1 Windows 安装

**系统要求**：
- Windows 10/11 专业版、企业版或教育版（64 位）
- 需要启用 WSL2（Windows Subsystem for Linux 2）或 Hyper-V
- 至少 4GB 内存（推荐 8GB+）

**安装步骤**：

```bash
# 步骤 1：下载 Docker Desktop
# 访问 https://www.docker.com/products/docker-desktop
# 点击 "Download for Windows" 下载安装包

# 步骤 2：运行安装程序
# 双击下载的 Docker Desktop Installer.exe
# 安装向导会提示启用 WSL2，勾选后点击 OK

# 步骤 3：重启电脑（如果安装程序要求）

# 步骤 4：启动 Docker Desktop
# 从开始菜单找到 Docker Desktop 并启动
# 首次启动可能需要几分钟初始化

# 步骤 5：等待 Docker 状态变为 "Running"
# 系统托盘会出现 Docker 鲸鱼图标
# 鼠标悬停显示 "Docker Desktop is running"

# 步骤 6：验证安装
# 打开 PowerShell 或 Git Bash，执行：
docker --version
# 预期输出：Docker version 24.xxx, build xxxxx

docker-compose --version
# 预期输出：Docker Compose version v2.xxx

# 步骤 7：运行测试容器
docker run hello-world
# 预期输出：Hello from Docker!
#          This message shows that your installation appears to be working correctly.
```

**WSL2 配置（Windows 用户必看）**：

如果安装过程中 WSL2 配置失败，手动执行以下步骤：

```powershell
# 以管理员身份打开 PowerShell

# 步骤 1：启用 WSL
wsl --install

# 步骤 2：设置 WSL 默认版本为 2
wsl --set-default-version 2

# 步骤 3：安装一个 Linux 发行版（如 Ubuntu）
# 打开 Microsoft Store，搜索 "Ubuntu" 并安装
# 或命令行安装：
wsl --install -d Ubuntu

# 步骤 4：配置 WSL 内存限制（防止 Docker 占用过多内存）
# 在用户目录下创建 .wslconfig 文件
# 路径：C:\Users\你的用户名\.wslconfig
notepad $env:USERPROFILE\.wslconfig
```

`.wslconfig` 文件内容：

```ini
[wsl2]
# 限制 WSL2 最大使用内存为 4GB（根据你的电脑配置调整）
memory=4GB
# 限制最大使用 2 个处理器
processors=2
# 限制最大交换空间
swap=2GB
```

保存后执行：

```powershell
# 关闭 WSL
wsl --shutdown

# 重启 Docker Desktop
# 然后验证配置是否生效
wsl -d Ubuntu -e free -h
```

#### 2.3.2 Mac 安装

```bash
# 步骤 1：下载 Docker Desktop
# 访问 https://www.docker.com/products/docker-desktop
# 根据你的 Mac 芯片选择：
# - Apple Silicon (M1/M2/M3)：选择 "Mac with Apple Chip"
# - Intel 芯片：选择 "Mac with Intel Chip"

# 步骤 2：安装
# 双击 .dmg 文件，将 Docker 图标拖到 Applications 文件夹

# 步骤 3：启动 Docker Desktop
# 从 Applications 文件夹打开 Docker Desktop

# 步骤 4：验证安装
docker --version
docker-compose --version
docker run hello-world
```

#### 2.3.3 Linux 安装（Ubuntu/Debian）

```bash
# 步骤 1：更新包索引
sudo apt-get update

# 步骤 2：安装必要的依赖
sudo apt-get install ca-certificates curl gnupg

# 步骤 3：添加 Docker 官方 GPG 密钥
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# 步骤 4：添加 Docker 软件源
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 步骤 5：安装 Docker Engine
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 步骤 6：将当前用户加入 docker 组（免 sudo 运行 docker）
sudo usermod -aG docker $USER

# 步骤 7：重新登录或执行以下命令使权限生效
newgrp docker

# 步骤 8：验证安装
docker --version
docker-compose --version
docker run hello-world
```

### 2.4 Docker 常用命令速查

#### 2.4.1 镜像管理命令

```bash
# ========== 镜像操作 ==========

# 从仓库拉取镜像
# 语法：docker pull 镜像名:标签
docker pull mysql:8.0
docker pull redis:7-alpine
docker pull nginx:alpine

# 如果不指定标签，默认拉取 latest（不推荐用于生产环境）
docker pull mysql

# 查看本地所有镜像
docker images
# 输出示例：
# REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
# mysql        8.0       a1b2c3d4e5f6   2 weeks ago    500MB
# redis        7-alpine  b2c3d4e5f6g7   3 weeks ago    30MB

# 删除本地镜像
# 语法：docker rmi 镜像ID 或 镜像名:标签
docker rmi mysql:8.0
# 如果镜像被容器使用，需要先删除容器：docker rm 容器名

# 搜索镜像
docker search mysql

# 查看镜像历史（了解镜像是怎么构建的）
docker history mysql:8.0
```

#### 2.4.2 容器管理命令

```bash
# ========== 容器生命周期 ==========

# 启动一个新容器
# 语法：docker run [选项] 镜像名:标签 [命令]
docker run -d --name my-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0

# 常用选项说明：
# -d              后台运行（detached 模式）
# --name          给容器起名字，方便后续管理
# -p 主机端口:容器端口   端口映射
# -e KEY=VALUE    设置环境变量
# -v 主机路径:容器路径  挂载数据卷
# --restart=always  容器自动重启策略

# 查看运行中的容器
docker ps
# 查看所有容器（包括已停止的）
docker ps -a

# 停止容器
docker stop my-mysql
# 启动已停止的容器
docker start my-mysql
# 重启容器
docker restart my-mysql

# 删除容器（必须先停止）
docker rm my-mysql
# 强制删除运行中的容器（不推荐）
docker rm -f my-mysql

# 进入容器内部（交互式）
# 语法：docker exec -it 容器名 命令
docker exec -it my-mysql bash
# 进入 MySQL 容器后，可以执行 mysql 命令：
# mysql -u root -p

# 如果容器没有 bash，用 sh：
docker exec -it my-redis sh

# 查看容器日志
docker logs my-mysql
# 实时跟踪日志（类似 tail -f）
docker logs -f my-mysql
# 查看最近 100 行日志
docker logs --tail 100 my-mysql

# 查看容器资源使用情况
docker stats

# 复制文件到容器/从容器复制文件
# 主机 → 容器
docker cp ./my.sql my-mysql:/tmp/
# 容器 → 主机
docker cp my-mysql:/tmp/my.sql ./
```

#### 2.4.3 容器清理命令

```bash
# 删除所有已停止的容器
docker container prune

# 删除所有未使用的镜像
docker image prune

# 删除所有未使用的数据卷
docker volume prune

# 一键清理所有未使用的资源（容器、镜像、卷、网络）
docker system prune
# 危险！会删除所有未使用的资源，执行前确认
docker system prune -a
```

### 2.5 用 Docker 一键启动 MySQL

MySQL 是我们课程中最重要的数据库，下面详细讲解如何用 Docker 启动 MySQL。

#### 2.5.1 基础启动

```bash
# 步骤 1：拉取 MySQL 8.0 镜像（如果本地没有）
docker pull mysql:8.0

# 步骤 2：启动 MySQL 容器
docker run -d \
  --name tlias-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=tlias_db \
  mysql:8.0

# 参数详解：
# -d                          后台运行，不占用当前终端
# --name tlias-mysql          容器名字叫 tlias-mysql，方便管理
# -p 3306:3306                将主机的 3306 端口映射到容器的 3306 端口
#                             这样你就可以用 localhost:3306 连接 MySQL
# -e MYSQL_ROOT_PASSWORD=123456   设置 root 用户的密码为 123456
# -e MYSQL_DATABASE=tlias_db      启动时自动创建名为 tlias_db 的数据库
# mysql:8.0                   使用的镜像和版本

# 步骤 3：查看容器状态
docker ps
# 预期看到 tlias-mysql 在运行中

# 步骤 4：查看启动日志（确认 MySQL 已就绪）
docker logs tlias-mysql
# 当看到 "ready for connections" 时表示 MySQL 已启动完成

# 步骤 5：用数据库工具连接测试
# 打开 Navicat 或 DBeaver：
# - 主机：localhost
# - 端口：3306
# - 用户名：root
# - 密码：123456
# - 数据库：tlias_db
```

#### 2.5.2 带数据持久化的启动（推荐）

上面的启动方式有一个问题：如果容器被删除，里面的数据也会丢失。生产环境必须使用数据卷持久化。

```bash
# 步骤 1：创建本地数据目录
# Windows（PowerShell）：
mkdir -p $env:USERPROFILE/docker-data/mysql/data
mkdir -p $env:USERPROFILE/docker-data/mysql/conf
mkdir -p $env:USERPROFILE/docker-data/mysql/logs

# Mac/Linux：
mkdir -p ~/docker-data/mysql/data
mkdir -p ~/docker-data/mysql/conf
mkdir -p ~/docker-data/mysql/logs

# 步骤 2：创建自定义配置文件（可选）
# 在 conf 目录下创建 my.cnf，内容如下：
cat > ~/docker-data/mysql/conf/my.cnf << 'EOF'
[mysqld]
# 设置字符集为 utf8mb4，支持中文和 emoji
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# 设置时区为东八区
default-time-zone='+08:00'

# 允许最大连接数
max_connections=200
EOF

# 步骤 3：启动带持久化的 MySQL 容器
docker run -d \
  --name tlias-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=tlias_db \
  -v ~/docker-data/mysql/data:/var/lib/mysql \
  -v ~/docker-data/mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf \
  -v ~/docker-data/mysql/logs:/var/log/mysql \
  --restart=always \
  mysql:8.0

# 新增参数详解：
# -v ~/docker-data/mysql/data:/var/lib/mysql
#     将主机的数据目录挂载到容器的 MySQL 数据目录
#     即使容器删除，数据文件仍然保留在主机上
#
# -v ~/docker-data/mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf
#     挂载自定义配置文件
#
# -v ~/docker-data/mysql/logs:/var/log/mysql
#     挂载日志目录，方便在主机上查看日志
#
# --restart=always
#     容器自动重启策略：Docker 启动时自动启动该容器；容器异常退出时自动重启

# 步骤 4：验证数据持久化
# 4.1 在数据库中创建一张表
docker exec -it tlias-mysql mysql -u root -p123456 -e "
USE tlias_db;
CREATE TABLE test_persistence (id INT PRIMARY KEY, name VARCHAR(50));
INSERT INTO test_persistence VALUES (1, '持久化测试');
"

# 4.2 删除容器
docker stop tlias-mysql
docker rm tlias-mysql

# 4.3 重新启动容器（使用相同的 -v 参数）
docker run -d \
  --name tlias-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=tlias_db \
  -v ~/docker-data/mysql/data:/var/lib/mysql \
  -v ~/docker-data/mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf \
  -v ~/docker-data/mysql/logs:/var/log/mysql \
  --restart=always \
  mysql:8.0

# 4.4 查询数据，验证是否还在
docker exec -it tlias-mysql mysql -u root -p123456 -e "
USE tlias_db;
SELECT * FROM test_persistence;
"
# 预期输出：id=1, name='持久化测试'
# 如果数据还在，说明持久化配置成功！
```

### 2.6 用 Docker 一键启动 Redis

Redis 是版本 B 课程中用于会话管理和缓存的重要组件。

```bash
# 步骤 1：拉取 Redis 7 镜像（Alpine 版本体积更小）
docker pull redis:7-alpine

# 步骤 2：启动 Redis 容器
docker run -d \
  --name tlias-redis \
  -p 6379:6379 \
  --restart=always \
  redis:7-alpine

# 参数详解：
# -d                  后台运行
# --name tlias-redis  容器名称
# -p 6379:6379        Redis 默认端口映射
# --restart=always    自动重启
# redis:7-alpine     Alpine Linux 版本的 Redis 7，体积小、启动快

# 步骤 3：验证 Redis 是否启动成功
docker ps

# 步骤 4：进入 Redis 容器测试
docker exec -it tlias-redis redis-cli

# 在 redis-cli 中执行测试命令：
127.0.0.1:6379> PING
# 预期返回：PONG

127.0.0.1:6379> SET test_key "Hello Redis"
# 预期返回：OK

127.0.0.1:6379> GET test_key
# 预期返回："Hello Redis"

127.0.0.1:6379> DEL test_key
# 预期返回：(integer) 1

127.0.0.1:6379> EXIT
# 退出 redis-cli

# 步骤 5：带持久化的 Redis 启动（推荐）
# 创建数据目录
mkdir -p ~/docker-data/redis/data

# 创建 Redis 配置文件
cat > ~/docker-data/redis/redis.conf << 'EOF'
# 开启持久化（RDB 方式）
save 900 1
save 300 10
save 60 10000

# 开启 AOF 持久化（更可靠）
appendonly yes
appendfsync everysec

# 设置密码（生产环境必须）
# requirepass your_password

# 绑定所有接口
bind 0.0.0.0

# 关闭保护模式（开发环境方便连接）
protected-mode no
EOF

# 启动带持久化的 Redis
docker run -d \
  --name tlias-redis \
  -p 6379:6379 \
  -v ~/docker-data/redis/data:/data \
  -v ~/docker-data/redis/redis.conf:/usr/local/etc/redis/redis.conf \
  --restart=always \
  redis:7-alpine \
  redis-server /usr/local/etc/redis/redis.conf
```

### 2.7 Docker Compose 多容器编排

当项目需要同时启动多个服务（MySQL + Redis + Nginx + 后端 + 前端）时，逐个 `docker run` 太麻烦了。Docker Compose 可以用一个 YAML 文件定义所有服务，一条命令启动全部。

#### 2.7.1 什么是 Docker Compose

```
+---------------------------------------------------------------+
|              Docker Compose 解决的问题                          |
+---------------------------------------------------------------+
|                                                               |
|  不用 Compose（手动逐个启动）：                                 |
|  +-- docker run mysql ...                                     |
|  +-- docker run redis ...                                     |
|  +-- docker run nginx ...                                     |
|  +-- docker run backend ...                                   |
|  +-- 每次重启电脑都要重复以上步骤                                |
|  +-- 端口、网络、依赖关系手动管理                                |
|                                                               |
|  用 Compose（一键启动）：                                       |
|  +-- 写一个 docker-compose.yml 文件                            |
|  +-- docker-compose up -d                                     |
|  +-- 所有服务全部启动，自动处理依赖关系                          |
|  +-- docker-compose down 一键停止并清理                        |
|  +-- docker-compose restart 一键重启                           |
|                                                               |
+---------------------------------------------------------------+
```

#### 2.7.2 编写 docker-compose.yml

```yaml
# ============================================
# docker-compose.yml - 课程开发环境配置
# ============================================
# 版本声明，3.8 支持大部分现代特性
version: '3.8'

# 定义所有服务
services:

  # ---------- MySQL 服务 ----------
  mysql:
    # 使用的镜像
    image: mysql:8.0
    # 容器名称
    container_name: tlias-mysql
    # 重启策略：总是自动重启
    restart: always
    # 环境变量
    environment:
      # root 用户密码
      MYSQL_ROOT_PASSWORD: 123456
      # 启动时自动创建的数据库
      MYSQL_DATABASE: tlias_db
      # 时区设置
      TZ: Asia/Shanghai
    # 端口映射：主机端口:容器端口
    ports:
      - "3306:3306"
    # 数据卷挂载
    volumes:
      # MySQL 数据持久化
      - ./docker-data/mysql/data:/var/lib/mysql
      # 自定义配置文件
      - ./docker-data/mysql/conf:/etc/mysql/conf.d
      # 初始化 SQL 脚本（首次启动时自动执行）
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    # 健康检查：每 10 秒检查一次 MySQL 是否就绪
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p123456"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # ---------- Redis 服务 ----------
  redis:
    image: redis:7-alpine
    container_name: tlias-redis
    restart: always
    ports:
      - "6379:6379"
    volumes:
      - ./docker-data/redis/data:/data
    # 启动命令：开启 AOF 持久化
    command: redis-server --appendonly yes

  # ---------- Nginx 服务 ----------
  nginx:
    image: nginx:alpine
    container_name: tlias-nginx
    restart: always
    ports:
      # 将主机的 80 端口映射到容器的 80 端口
      # 访问 http://localhost 即可看到前端页面
      - "80:80"
    volumes:
      # 挂载前端构建产物
      - ./frontend/dist:/usr/share/nginx/html
      # 挂载自定义 Nginx 配置
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    # 依赖：等前端构建完成后再启动（实际项目中使用）
    depends_on:
      - backend

  # ---------- 后端服务（版本 A）----------
  backend:
    # 使用 Dockerfile 构建镜像（而不是直接用现成镜像）
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: tlias-backend
    restart: always
    ports:
      - "8080:8080"
    environment:
      # 数据库连接配置
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tlias_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
      # Redis 连接配置（版本 B）
      # SPRING_REDIS_HOST: redis
      # SPRING_REDIS_PORT: 6379
    # 依赖：等 MySQL 启动后再启动后端
    depends_on:
      mysql:
        condition: service_healthy

# ---------- 数据卷定义 ----------
volumes:
  mysql_data:
    driver: local
  redis_data:
    driver: local

# ---------- 网络定义 ----------
networks:
  # 默认网络，所有服务自动加入同一个网络，可以通过服务名互相访问
  default:
    driver: bridge
```

#### 2.7.3 Docker Compose 常用命令

```bash
# ========== Docker Compose 核心命令 ==========

# 启动所有服务（后台运行）
# -d = detached 模式，不占用终端
docker-compose up -d

# 启动并重新构建镜像（代码修改后使用）
docker-compose up -d --build

# 只构建镜像，不启动
docker-compose build

# 查看所有服务状态
docker-compose ps

# 查看所有服务日志
docker-compose logs

# 查看某个服务的实时日志（最常用）
docker-compose logs -f mysql

# 重启某个服务
docker-compose restart backend

# 停止所有服务（保留容器和数据）
docker-compose stop

# 启动已停止的服务
docker-compose start

# 停止并删除所有容器（保留数据卷）
docker-compose down

# 停止并删除所有容器和数据卷（彻底清理，数据会丢失！）
docker-compose down -v

# 进入某个服务的容器内部
docker-compose exec mysql bash
docker-compose exec redis redis-cli

# 查看服务资源使用
docker-compose top
```

#### 2.7.4 课程项目一键启动

```bash
# 步骤 1：确保 Docker Desktop 正在运行
# 检查系统托盘图标

# 步骤 2：进入项目目录
cd javaweb-ai-course

# 步骤 3：选择版本
cd version-a   # 或 cd version-b

# 步骤 4：启动所有服务
docker-compose up -d

# 步骤 5：查看服务启动状态
docker-compose ps
# 预期看到 mysql、redis、backend、nginx 都在运行

# 步骤 6：查看 MySQL 启动日志（确认数据库已就绪）
docker-compose logs -f mysql
# 当看到 "ready for connections" 时按 Ctrl+C 退出日志查看

# 步骤 7：查看后端启动日志
docker-compose logs -f backend
# 当看到 "Started TliasApplication" 时，后端启动完成

# 步骤 8：访问系统
# 打开浏览器访问 http://localhost
# 应该能看到前端登录页面

# 步骤 9：停止所有服务
docker-compose down
```

### 2.8 数据卷（Volume）详解

数据卷是 Docker 中保证数据持久化的核心机制。

#### 2.8.1 三种挂载方式对比

| 挂载方式 | 语法 | 适用场景 | 特点 |
|---------|------|---------|------|
| 命名卷 | `volumes: - mysql_data:/var/lib/mysql` | 数据库存储 | Docker 管理，位置由 Docker 决定 |
| 绑定挂载 | `volumes: - ./data:/var/lib/mysql` | 开发环境 | 主机路径明确，方便查看和备份 |
| 临时卷 | `--tmpfs /tmp` | 临时缓存 | 容器删除后数据丢失 |

#### 2.8.2 数据卷操作命令

```bash
# 查看所有数据卷
docker volume ls

# 查看某个数据卷的详细信息
docker volume inspect mysql_data

# 删除数据卷（确保没有容器在使用）
docker volume rm mysql_data

# 删除所有未使用的数据卷
docker volume prune
```

#### 2.8.3 数据备份与恢复

```bash
# ========== MySQL 数据备份 ==========

# 方式 1：使用 docker exec 执行 mysqldump
docker exec tlias-mysqldump -u root -p123456 tlias_db > backup_$(date +%Y%m%d).sql

# 方式 2：进入容器后执行
docker exec -it tlias-mysql bash
mysqldump -u root -p123456 tlias_db > /tmp/backup.sql
exit
docker cp tlias-mysql:/tmp/backup.sql ./backup.sql

# ========== MySQL 数据恢复 ==========

# 将备份文件复制到容器内
docker cp backup.sql tlias-mysql:/tmp/backup.sql

# 进入容器执行恢复
docker exec -it tlias-mysql bash
mysql -u root -p123456 tlias_db < /tmp/backup.sql

# ========== Redis 数据备份 ==========

# Redis 数据保存在 /data 目录（如果开启了 AOF 或 RDB）
# 直接复制数据卷即可
cp -r ~/docker-data/redis/data ./redis-backup

# 或使用 Redis 的 SAVE 命令
docker exec tlias-redis redis-cli SAVE
# 然后复制 /data 目录下的 dump.rdb 文件
```

### 2.9 用 Docker 部署其他开发环境

#### 2.9.1 Nginx（前端静态资源服务）

```bash
# 启动 Nginx 容器，用于托管前端构建产物
docker run -d \
  --name tlias-nginx \
  -p 80:80 \
  -v /path/to/your/dist:/usr/share/nginx/html \
  -v /path/to/your/nginx.conf:/etc/nginx/conf.d/default.conf \
  --restart=always \
  nginx:alpine

# 自定义 nginx.conf 示例：
cat > nginx.conf << 'EOF'
server {
    listen 80;
    server_name localhost;

    # 前端静态资源
    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        # 支持 Vue Router 的 history 模式
        try_files $uri $uri/ /index.html;
    }

    # 反向代理到后端 API
    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
EOF
```

#### 2.9.2 MinIO（对象存储，用于文件上传）

```bash
# MinIO 是一个兼容 S3 协议的对象存储服务
# 适合用于存储用户上传的头像、附件等文件

docker run -d \
  --name tlias-minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=admin \
  -e MINIO_ROOT_PASSWORD=admin123456 \
  -v ~/docker-data/minio/data:/data \
  --restart=always \
  minio/minio server /data --console-address ":9001"

# 访问地址：
# - API 端口：http://localhost:9000
# - 管理控制台：http://localhost:9001
# - 默认账号：admin / admin123456
```

---

## 三、动手练习

### 练习 1：完整启动 MySQL 并验证持久化

```bash
# 步骤 1：清理环境（如果之前启动过）
docker stop tlias-mysql 2>/dev/null
docker rm tlias-mysql 2>/dev/null
rm -rf ~/docker-data/mysql

# 步骤 2：创建数据目录
mkdir -p ~/docker-data/mysql/data
mkdir -p ~/docker-data/mysql/conf
mkdir -p ~/docker-data/mysql/logs

# 步骤 3：创建自定义配置文件
cat > ~/docker-data/mysql/conf/my.cnf << 'EOF'
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='+08:00'
EOF

# 步骤 4：启动 MySQL 容器
docker run -d \
  --name tlias-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=tlias_db \
  -v ~/docker-data/mysql/data:/var/lib/mysql \
  -v ~/docker-data/mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf \
  --restart=always \
  mysql:8.0

# 步骤 5：等待 MySQL 启动（约 30 秒）
sleep 30

# 步骤 6：验证 MySQL 可以连接
docker exec -it tlias-mysql mysql -u root -p123456 -e "SELECT 1+1 AS result;"
# 预期输出：result = 2

# 步骤 7：创建测试数据
docker exec -it tlias-mysql mysql -u root -p123456 -e "
USE tlias_db;
CREATE TABLE IF NOT EXISTS students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    age INT
);
INSERT INTO students (name, age) VALUES ('张三', 20), ('李四', 21);
SELECT * FROM students;
"
# 预期看到两条学生记录

# 步骤 8：删除容器
docker stop tlias-mysql
docker rm tlias-mysql

# 步骤 9：重新启动容器（使用相同的数据卷）
docker run -d \
  --name tlias-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=tlias_db \
  -v ~/docker-data/mysql/data:/var/lib/mysql \
  -v ~/docker-data/mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf \
  --restart=always \
  mysql:8.0

# 步骤 10：等待启动后验证数据是否还在
sleep 30
docker exec -it tlias-mysql mysql -u root -p123456 -e "
USE tlias_db;
SELECT * FROM students;
"
# 预期：张三和李四的数据仍然存在！
```

**完成标准**：
- MySQL 容器成功启动并能连接
- 创建的数据在容器删除重建后仍然存在

### 练习 2：启动 Redis 并测试基本操作

```bash
# 步骤 1：启动 Redis 容器
docker run -d \
  --name tlias-redis \
  -p 6379:6379 \
  --restart=always \
  redis:7-alpine

# 步骤 2：等待启动
sleep 5

# 步骤 3：进入 Redis 容器执行测试
docker exec -it tlias-redis redis-cli << 'EOF'
PING
SET course_name "JavaWeb+AI实训"
GET course_name
EXPIRE course_name 60
TTL course_name
SET student_count 100
INCR student_count
GET student_count
KEYS *
EOF

# 预期输出：
# PONG
# OK
# "JavaWeb+AI实训"
# (integer) 1
# (integer) 59
# OK
# (integer) 101
# "101"
# 1) "course_name"
# 2) "student_count"

# 步骤 4：清理
docker stop tlias-redis
docker rm tlias-redis
```

**完成标准**：
- Redis 容器成功启动
- 所有测试命令返回预期结果

### 练习 3：编写并运行 docker-compose.yml

```bash
# 步骤 1：创建一个测试目录
mkdir docker-compose-practice
cd docker-compose-practice

# 步骤 2：创建 docker-compose.yml 文件
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: practice-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: practice_db
      TZ: Asia/Shanghai
    ports:
      - "3307:3306"
    volumes:
      - ./mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p123456"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: practice-redis
    restart: always
    ports:
      - "6380:6379"
    volumes:
      - ./redis-data:/data
    command: redis-server --appendonly yes

  nginx:
    image: nginx:alpine
    container_name: practice-nginx
    restart: always
    ports:
      - "8080:80"
    depends_on:
      - mysql
EOF

# 步骤 3：启动所有服务
docker-compose up -d

# 步骤 4：查看服务状态
docker-compose ps
# 预期看到 3 个服务都在运行

# 步骤 5：验证 MySQL（注意端口是 3307）
docker exec -it practice-mysql mysql -u root -p123456 -e "SELECT VERSION();"

# 步骤 6：验证 Redis（注意端口是 6380）
docker exec -it practice-redis redis-cli PING

# 步骤 7：验证 Nginx
curl http://localhost:8080
# 预期看到 Nginx 的欢迎页面 HTML

# 步骤 8：查看日志
docker-compose logs

# 步骤 9：停止并清理
docker-compose down -v
rm -rf mysql-data redis-data
```

**完成标准**：
- `docker-compose.yml` 文件语法正确
- 三个服务全部成功启动
- MySQL、Redis、Nginx 都能正常访问

---

## 四、常见错误排查

| 阶段 | 错误现象 | 可能原因 | 解决方案 |
|------|---------|---------|---------|
| **安装** | `Docker Desktop starting...` 一直转圈 | WSL2 未正确配置 | 以管理员身份打开 PowerShell，执行 `wsl --install` 和 `wsl --set-default-version 2` |
| **安装** | `Hardware assisted virtualization...` | BIOS 中未启用虚拟化 | 重启电脑进入 BIOS，启用 Intel VT-x 或 AMD-V |
| **启动** | `Cannot connect to the Docker daemon` | Docker Desktop 未启动 | 打开 Docker Desktop，等待状态变为 Running |
| **启动** | `port is already allocated` | 主机端口被其他程序占用 | 停止本地 MySQL/Redis（`netstat -ano \| findstr 3306` 查找占用进程）；或修改 Docker 端口映射 |
| **启动** | 容器启动后立即退出 | 容器内应用启动失败 | 查看日志：`docker logs 容器名`，根据错误信息排查 |
| **启动** | `Out of memory` 或容器被杀 | Docker / WSL2 内存不足 | 增加 WSL2 内存限制（修改 `.wslconfig` 文件）；或减少同时运行的容器数量 |
| **连接** | 数据库工具连不上 Docker MySQL | 端口映射错误，或 MySQL 未就绪 | 检查 `docker ps` 确认端口映射；查看日志确认 MySQL 已启动；检查防火墙 |
| **连接** | `Access denied for user 'root'@'...'` | 密码错误，或用户权限问题 | 确认 `-e MYSQL_ROOT_PASSWORD` 设置的密码；检查是否使用了 `%` 通配符允许远程连接 |
| **数据** | 容器删除后数据丢失 | 没有挂载数据卷 | 启动时添加 `-v` 参数挂载持久化目录；或使用命名卷 |
| **Compose** | `services.xxx.ports must be a mapping` | docker-compose.yml 语法错误 | 检查 YAML 缩进（不能用 Tab，必须用空格）；检查端口映射格式 |
| **Compose** | `depends_on` 不等待服务就绪 | depends_on 只等容器启动，不等应用就绪 | 使用 `healthcheck` + `condition: service_healthy`；或在应用层添加重试逻辑 |
| **性能** | Docker 占用内存/CPU 过高 | 运行的容器太多，或 WSL2 配置不当 | 停止不需要的容器；限制 WSL2 的 memory 和 processors；给 Docker Desktop 设置资源限制 |

---

## 五、本节小结

```
+---------------------------------------------------------------+
|                    Docker 知识图谱                              |
+---------------------------------------------------------------+
|                                                               |
|   +----------------+                                          |
|   |   为什么用 Docker|                                         |
|   |  环境一致 + 一键启动|                                       |
|   +--------+-------+                                          |
|            |                                                  |
|            v                                                  |
|   +----------------+                                          |
|   |   核心概念      |                                          |
|   |  镜像→容器→    |                                          |
|   |  数据卷        |                                          |
|   +--------+-------+                                          |
|            |                                                  |
|            v                                                  |
|   +----------------+     +----------------+                  |
|   |   安装配置      |<--->|  Docker Desktop|                  |
|   |  Win/Mac/Linux |     |  WSL2 / Hyper-V|                  |
|   +--------+-------+     +----------------+                  |
|            |                                                  |
|            v                                                  |
|   +----------------+                                          |
|   |   常用命令      |                                          |
|   |  run/ps/stop/  |                                          |
|   |  rm/logs/exec  |                                          |
|   +--------+-------+                                          |
|            |                                                  |
|            v                                                  |
|   +----------------+     +----------------+                  |
|   |   服务启动      |<--->|  MySQL + Redis |                  |
|   |  数据持久化    |     |  + Nginx/MinIO |                  |
|   +--------+-------+     +----------------+                  |
|            |                                                  |
|            v                                                  |
|   +----------------+                                          |
|   |   Docker Compose|                                         |
|   |  多容器编排     |                                          |
|   |  一键启动/停止  |                                          |
|   +--------+-------+                                          |
|            |                                                  |
|            v                                                  |
|   +----------------+                                          |
|   |   问题排查      |                                          |
|   |  端口/内存/    |                                          |
|   |  WSL2/权限     |                                          |
|   +----------------+                                          |
|                                                               |
+---------------------------------------------------------------+
```

**核心要点回顾**：

1. Docker 解决了"环境不一致"和"配置复杂"的问题，是现代开发的标准工具。
2. 镜像（Image）是模板，容器（Container）是运行实例，数据卷（Volume）保证数据持久化。
3. Windows 用户需要配置 WSL2，并注意内存限制，防止 Docker 占用过多资源。
4. 启动 MySQL/Redis 时务必挂载数据卷，否则容器删除后数据会丢失。
5. Docker Compose 用 YAML 文件定义多容器应用，一条命令启动整个技术栈。
6. 遇到问题时先看日志（`docker logs`），再检查端口占用和网络配置。

---

## 六、参考文档

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Docker Hub 镜像仓库](https://hub.docker.com/)（搜索和下载镜像）
- [MySQL Docker 镜像说明](https://hub.docker.com/_/mysql)
- [Redis Docker 镜像说明](https://hub.docker.com/_/redis)
- [Nginx Docker 镜像说明](https://hub.docker.com/_/nginx)
- [WSL2 安装指南](https://docs.microsoft.com/zh-cn/windows/wsl/install)（Windows 用户）
- [Docker Desktop Windows 安装指南](https://docs.docker.com/desktop/install/windows-install/)
- [Docker Desktop Mac 安装指南](https://docs.docker.com/desktop/install/mac-install/)
- [MinIO Docker 快速开始](https://min.io/docs/minio/container/index.html)
