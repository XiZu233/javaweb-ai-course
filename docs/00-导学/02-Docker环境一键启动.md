# Docker 环境一键启动

## 学习目标

- 理解 Docker 和 Docker Compose 的基本概念
- 能够使用 Docker Compose 一键启动完整开发环境
- 掌握常用 Docker 命令

## 核心知识点

### 1. 为什么用 Docker

开发中最痛苦的事情就是**"环境配置"**。Docker 通过容器化技术，让应用运行在标准化的环境中，实现"一次构建，到处运行"。

### 2. Docker 核心概念

| 概念 | 说明 |
|------|------|
| 镜像 (Image) | 应用的打包模板，类似 class 文件 |
| 容器 (Container) | 镜像的运行实例，类似对象 |
| Dockerfile | 定义镜像构建步骤的脚本 |
| Docker Compose | 管理多个容器的编排工具 |

### 3. 一键启动项目

**版本A**：
```bash
cd version-a
docker-compose up -d
# 等待服务启动...
# 访问 http://localhost
```

**版本B**：
```bash
cd version-b
docker-compose up -d
# 等待服务启动...
# 访问 http://localhost
```

### 4. 常用 Docker 命令

```bash
# 查看运行中的容器
docker ps

# 查看日志
docker logs -f tlias-backend

# 进入容器内部
docker exec -it tlias-mysql bash

# 停止所有服务
docker-compose down

# 重启服务
docker-compose restart

# 查看容器资源占用
docker stats
```

### 5. 容器服务说明

| 容器 | 端口 | 用途 |
|------|------|------|
| tlias-mysql | 3306 | MySQL 数据库 |
| tlias-backend | 8080 | SpringBoot 后端 |
| tlias-frontend | 80 | Vue 前端（Nginx） |
| tlias-pro-redis | 6379 | Redis 缓存（版本B） |

## 动手练习

1. 安装 Docker Desktop
2. 执行 `docker-compose up -d` 启动版本A
3. 使用 `docker ps` 查看运行中的容器
4. 使用 `docker logs` 查看后端启动日志
5. 使用 `docker exec -it tlias-mysql bash` 进入 MySQL 容器，执行 `mysql -uroot -p` 登录

## 常见错误排查

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| port is already allocated | 端口被占用 | 修改 docker-compose.yml 中的端口映射 |
| Connection refused | 服务未启动完成 | 等待几秒后重试 |
| Access denied for user | 密码错误 | 检查 docker-compose.yml 中的 MYSQL_ROOT_PASSWORD |

## 本节小结

Docker 让环境搭建从"天级别"缩短到"分钟级别"。掌握 Docker，你就掌握了现代开发运维的钥匙。
