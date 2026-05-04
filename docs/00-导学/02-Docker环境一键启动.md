# Docker 环境一键启动

## 学习目标

- 能够使用 Docker Compose 一键启动完整开发环境
- 理解 docker-compose.yml 中各服务的作用
- 能够排查容器启动过程中的常见问题

## 核心知识点

### 1. Docker Compose 是什么

Docker Compose 是 Docker 官方的多容器编排工具。通过一个 YAML 文件定义所有服务，一条命令启动整个应用栈。

**对比手动启动**：

| 方式 | 命令数量 | 复杂度 | 可复现性 |
|------|---------|--------|---------|
| 手动逐个启动容器 | 多个 | 高 | 低 |
| Docker Compose | 1个 | 低 | 高 |

### 2. 版本 A 一键启动

```bash
cd version-a

# 构建并启动所有服务（后台运行）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend

# 停止所有服务
docker-compose down
```

**启动的服务**：
- MySQL 8.0（端口 3306）
- SpringBoot 后端（端口 8080）
- Nginx 前端（端口 80）

### 3. 版本 B 一键启动

```bash
cd version-b

# 启动（需要设置 AI API Key）
export KIMI_API_KEY=your-api-key
docker-compose up -d

# 或使用 mock 模式（无需 API Key）
docker-compose up -d

# 查看服务状态
docker-compose ps
```

**启动的服务**：
- MySQL 8.0（端口 3306）
- Redis 7（端口 6379）
- SpringBoot3 后端（端口 48080）
- Nginx 前端（端口 80）

### 4. 首次启动流程

```bash
# 1. 确保 Docker Desktop 已启动

# 2. 进入项目目录
cd version-a   # 或 version-b

# 3. 前端需要先打包（如果 dist 目录不存在）
cd tlias-frontend && npm install && npm run build && cd ..

# 4. 启动所有服务
docker-compose up -d

# 5. 等待初始化完成（约 30 秒）
docker-compose logs -f mysql
# 当看到 "ready for connections" 表示 MySQL 就绪

# 6. 访问验证
# 打开浏览器访问 http://localhost
```

### 5. 常用排查命令

```bash
# 查看所有运行中的容器
docker ps

# 进入容器内部
docker exec -it tlias-mysql bash

# 查看容器日志
docker logs -f tlias-backend

# 重启单个服务
docker-compose restart backend

# 重建镜像（代码修改后）
docker-compose up -d --build backend
```

### 6. 数据持久化说明

MySQL 数据通过命名卷持久化：

```yaml
volumes:
  mysql_data:/var/lib/mysql
```

即使执行 `docker-compose down`，数据也不会丢失。只有执行 `docker-compose down -v` 才会删除数据卷。

## 动手练习

### 练习 1：完整启动流程

1. 清理所有 Docker 容器和数据卷
2. 按照"首次启动流程"完整操作一遍
3. 访问 http://localhost 验证系统可正常登录和使用

### 练习 2：日志排查

1. 故意修改后端配置使其连接数据库失败
2. 重启后端服务，观察日志输出
3. 根据日志定位并修复问题

## 常见错误排查

| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| port is already allocated | 端口被本地服务占用 | 停止本地 MySQL/8080 服务，或修改端口映射 |
| 前端页面空白 | dist 目录不存在或为空 | 先执行 npm run build |
| 数据库连接失败 | MySQL 未就绪后端先启动 | 确保 MySQL 健康检查通过后再启动后端 |
| 容器不断重启 | 应用启动报错 | docker logs 查看具体错误 |

## 本节小结

Docker Compose 是本地开发和演示的最佳伴侣。一条 `docker-compose up -d` 就能拉起完整的技术栈，让环境搭建不再是学习的拦路虎。

## 参考文档

- [Docker Compose 官方文档](https://docs.docker.com/compose/)

