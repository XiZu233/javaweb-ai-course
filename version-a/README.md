# 版本A：传统实训版 - Tlias人事管理系统

基于 SpringBoot2 + Vue2 + 原生MyBatis 的经典Web开发教学项目。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | SpringBoot 2.7.18 |
| ORM框架 | 原生MyBatis 3.5（XML配置方式） |
| 数据库 | MySQL 8.0 |
| 分页插件 | PageHelper 5.3 |
| 认证方案 | JWT + 手写Filter |
| 日志记录 | AOP环绕通知 |
| 前端框架 | Vue 2.6 + ElementUI 2.15 |
| 构建工具 | Vue CLI 5 |
| 状态管理 | Vuex |
| 路由 | Vue Router 3 |
| HTTP客户端 | Axios |

## 功能模块

- [x] 部门管理（列表、新增、修改、删除）
- [x] 员工管理（分页查询、条件搜索、新增、修改、删除）
- [x] 工作经历（新增员工时批量保存）
- [x] 文件上传（头像上传 - 本地存储 + 阿里云OSS）
- [x] 登录认证（JWT令牌 + Filter校验）
- [x] 操作日志（AOP自动记录）
- [x] 数据统计（职位分布、性别比例）

## 快速启动

### Docker Compose 一键启动

```bash
docker-compose up -d
```

访问 http://localhost

默认账号：admin / 123456

### 本地开发启动

**后端**：
```bash
cd tlias-backend
mvn spring-boot:run
```

**前端**：
```bash
cd tlias-frontend
npm install
npm run serve
```

## 数据库初始化

```bash
# 执行SQL脚本
mysql -uroot -p < sql/tlias_init.sql
```

## 项目结构

```
tlias-backend/
├── src/main/java/com/tlias/
│   ├── TliasApplication.java      # 启动类
│   ├── controller/                 # 控制层
│   ├── service/                    # 业务层
│   ├── mapper/                     # 数据访问层（接口）
│   ├── pojo/                       # 实体类
│   ├── utils/                      # 工具类
│   ├── filter/                     # 过滤器（Token校验）
│   ├── aspect/                     # AOP切面（日志记录）
│   └── exception/                  # 全局异常处理
└── src/main/resources/
    ├── application.yml             # 配置文件
    └── mapper/*.xml                # MyBatis映射文件
```
