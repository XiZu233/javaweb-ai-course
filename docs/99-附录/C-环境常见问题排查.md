# 附录 C：环境常见问题排查

## 后端问题

### Maven 依赖下载失败

**现象**：`Could not find artifact xxx`

**解决**：
1. 检查网络连接
2. 配置阿里云镜像（settings.xml）：
```xml
<mirror>
    <id>aliyun</id>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
    <mirrorOf>central</mirrorOf>
</mirror>
```
3. 删除本地仓库 `.m2/repository` 中对应的文件夹，重新下载

### 端口被占用

**现象**：`Port 8080 was already in use`

**解决**：
```bash
# 查找占用端口的进程
netstat -ano | findstr :8080
# Windows 结束进程
taskkill /PID <PID> /F
```

### MySQL 连接失败

**现象**：`Communications link failure`

**解决**：
1. 检查 MySQL 是否启动
2. 检查连接地址、端口、用户名密码
3. 检查防火墙是否放行 3306 端口

## 前端问题

### npm install 失败

**现象**：下载依赖超时或报错

**解决**：
```bash
# 配置淘宝镜像
npm config set registry https://registry.npmmirror.com

# 或使用 nrm 切换
npx nrm use taobao
```

### 跨域错误

**现象**：`CORS policy: No 'Access-Control-Allow-Origin'`

**解决**：
1. 开发环境：检查 vue.config.js proxy 配置
2. 生产环境：后端开启 CORS 支持

## Docker 问题

### 容器启动后立即退出

**现象**：`docker ps` 看不到运行中的容器

**解决**：
```bash
docker logs <container_name>  # 查看日志定位问题
```

### 数据卷权限问题

**现象**：MySQL 容器无法写入数据

**解决**：Windows 下 Docker Desktop 已处理权限映射，Linux 下可能需要 `chmod 777`。

