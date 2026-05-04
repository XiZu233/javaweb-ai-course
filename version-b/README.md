# 版本B：AI增强实战版 - Tlias Pro 智能人事管理平台

基于 yudao-boot-mini（ruoyi-vue-pro精简版）改造的现代化全栈项目，融入AI功能模块。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | SpringBoot 3.2 + JDK17 |
| ORM框架 | MyBatis-Plus 3.5.9 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.0 |
| 认证框架 | Sa-Token 1.45 |
| API文档 | Knife4j |
| AI框架 | Spring AI |
| 前端框架 | Vue 3.5 + TypeScript |
| 构建工具 | Vite |
| UI组件库 | ElementPlus 2.11 |
| 状态管理 | Pinia |
| 图表 | ECharts |

## AI功能模块

- [x] **NL2SQL智能查询**：自然语言输入自动转换为SQL查询
- [x] **RAG知识库问答**：基于向量检索的企业制度问答助手
- [x] **AI简历解析**：PDF简历自动提取结构化信息

## 快速启动

### Docker Compose 一键启动

```bash
cd version-b
docker-compose up -d
```

访问 http://localhost

默认账号：admin / admin123

### 本地开发启动

**后端**：
```bash
cd tlias-pro-backend
mvn clean install -DskipTests
mvn spring-boot:run -pl tlias-pro-server
```

**前端**：
```bash
cd tlias-pro-frontend
npm install
npm run dev
```

## 项目结构

```
tlias-pro-backend/
├── tlias-pro-server/          # 启动入口
├── tlias-pro-framework/       # 公共框架组件
├── tlias-pro-module-system/   # 系统模块（用户/角色/菜单/部门）
├── tlias-pro-module-infra/    # 基础设施模块
├── tlias-pro-module-hr/       # 人事业务模块
└── tlias-pro-module-ai/       # AI功能模块
```
