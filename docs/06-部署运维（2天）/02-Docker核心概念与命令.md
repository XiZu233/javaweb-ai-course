# Docker 核心概念与命令

## 学习目标

- 理解 Docker 镜像、容器、仓库的核心概念
- 掌握常用的 Docker 命令
- 能够编写简单的 Dockerfile

## 核心知识点

### 1. Docker 是什么

Docker 是一种容器化技术，将应用及其依赖打包成一个独立的容器，在任何环境中都能一致运行。

**对比虚拟机**：

| 特性 | 虚拟机 | Docker 容器 |
|------|--------|------------|
| 启动速度 | 分钟级 | 秒级 |
| 资源占用 | 占用整个操作系统 | 共享宿主机内核 |
| 隔离级别 | 硬件级隔离 | 进程级隔离 |
| 大小 | GB 级 | MB 级 |

### 2. 核心概念

- **镜像（Image）**：只读模板，包含运行应用所需的所有内容
- **容器（Container）**：镜像的运行实例，可以被创建、启动、停止、删除
- **仓库（Registry）**：存储和分发镜像的服务，如 Docker Hub
- **Dockerfile**：定义镜像构建步骤的文本文件

### 3. 常用命令

```bash
# 镜像操作
docker pull nginx:latest       # 拉取镜像
docker images                  # 查看本地镜像
docker rmi image_id            # 删除镜像

# 容器操作
docker run -d -p 80:80 --name my-nginx nginx    # 运行容器
docker ps                      # 查看运行中的容器
docker ps -a                   # 查看所有容器
docker stop my-nginx           # 停止容器
docker start my-nginx          # 启动容器
docker rm my-nginx             # 删除容器
docker logs -f my-nginx        # 查看容器日志

# 进入容器内部
docker exec -it my-nginx /bin/sh

# 构建镜像
docker build -t myapp:1.0 .
```

### 4. Dockerfile 编写

版本 A 后端 Dockerfile 示例：

```dockerfile
# 基础镜像
FROM openjdk:8-jdk-alpine

# 工作目录
WORKDIR /app

# 复制 jar 包
COPY target/tlias-backend-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**常用指令**：

| 指令 | 说明 |
|------|------|
| FROM | 基础镜像 |
| WORKDIR | 设置工作目录 |
| COPY | 复制文件到镜像 |
| RUN | 执行命令 |
| EXPOSE | 声明暴露端口 |
| ENTRYPOINT | 容器启动时执行的命令 |

### 5. 数据持久化

容器中的数据默认随容器删除而丢失，需要挂载数据卷：

```bash
# 挂载主机目录
docker run -v /host/data:/container/data mysql:8.0

# 使用命名卷
docker run -v mysql_data:/var/lib/mysql mysql:8.0
```

### 6. 网络

Docker 默认创建 bridge 网络，容器间可以通过容器名互相访问：

```bash
# 查看网络
docker network ls

# 创建自定义网络
docker network create my-network

# 运行容器并指定网络
docker run --network my-network --name mysql mysql:8.0
docker run --network my-network --name backend myapp:1.0
# backend 容器内可通过 mysql 主机名访问数据库
```

## 动手练习

### 练习 1：运行 Nginx 容器

1. 拉取 nginx 镜像
2. 运行容器并映射端口 80
3. 浏览器访问 http://localhost 验证

### 练习 2：构建 SpringBoot 镜像

1. 使用 Maven 打包 jar：`mvn clean package -DskipTests`
2. 编写 Dockerfile
3. 构建镜像：`docker build -t tlias-backend:1.0 .`
4. 运行容器并测试接口

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| port is already allocated | 端口被占用 | 更换端口或停止占用端口的容器 |
| No such file or directory | Dockerfile 中 COPY 路径错误 | 检查 COPY 的源文件路径 |
| 容器启动后立即退出 | 主进程执行完或报错 | 查看 docker logs 排查 |

## 本节小结

Docker 解决了"在我机器上能跑"的问题。通过镜像打包应用，通过容器运行应用，通过数据卷持久化数据，是现代应用部署的标准方式。

## 参考文档

- [Docker 官方文档](https://docs.docker.com/)
- [Dockerfile 参考](https://docs.docker.com/engine/reference/builder/)

