# JavaWeb+AI 实训课程从零搭建计划

## Context

原200集JavaWeb视频课程（Tlias人事系统）技术栈落后当前企业需求3-5年。本计划目标是从零搭建一套**可自学、可部署**的双版本实训课程资料，上传至GitHub供学生独立完成学习。

- **版本A（传统实训版）**：保留原课程核心原理，技术栈为 SpringBoot2 + Vue2 + 原生MyBatis，面向零基础学生建立Web开发全局认知
- **版本B（AI增强实战版）**：基于 yudao-boot-mini（ruoyi-vue-pro精简版）改造，技术栈为 SpringBoot3 + Vue3 + TS + MyBatis-Plus + Redis + Sa-Token，叠加NL2SQL/RAG/AI简历解析三大AI功能，面向进阶学生产出简历级项目

**关键决策确认**：单仓库双目录结构 | 版本B基于yudao-boot-mini改造 | Docker Compose本地一键启动

---

## 一、最终GitHub仓库结构

```
tlias-training/
├── README.md                          # 总入口：项目介绍、快速开始、双版本对比
├── docs/                              # 完整课程文档（Markdown，可自学）
│   ├── 00-导学/
│   │   ├── 00-课程介绍与环境准备.md
│   │   ├── 01-Git入门与仓库管理.md
│   │   └── 02-Docker环境一键启动.md
│   ├── 01-前端基础（2天）/
│   │   ├── 01-HTML-CSS基础.md
│   │   ├── 02-JavaScript核心语法.md
│   │   ├── 03-Vue2快速入门（版本A）.md
│   │   ├── 04-Vue3快速入门（版本B）.md
│   │   ├── 05-Ajax与前后端交互.md
│   │   └── 动手练习/
│   ├── 02-后端基础（4天）/
│   │   ├── 01-Maven依赖管理.md
│   │   ├── 02-SpringBoot分层架构与IOC-DI.md
│   │   ├── 03-HTTP协议详解.md
│   │   ├── 04-MySQL设计与SQL操作.md
│   │   ├── 05-JDBC到MyBatis原理.md
│   │   ├── 06-MyBatis-Plus快速开发（版本B）.md
│   │   ├── 07-工程规范与Restful-API.md
│   │   └── 动手练习/
│   ├── 03-后端实战（6天）/
│   │   ├── 01-部门管理CRUD.md
│   │   ├── 02-员工管理分页查询.md
│   │   ├── 03-条件搜索与动态SQL.md
│   │   ├── 04-文件上传与云存储.md
│   │   ├── 05-多表关系与设计.md
│   │   ├── 06-事务管理与AOP日志.md
│   │   ├── 07-全局异常处理.md
│   │   └── 动手练习/
│   ├── 04-后端进阶（2天）/
│   │   ├── 01-JWT登录认证原理.md
│   │   ├── 02-Filter与Interceptor（版本A）.md
│   │   ├── 03-Sa-Token权限框架（版本B）.md
│   │   ├── 04-SpringAOP核心概念.md
│   │   ├── 05-SpringBoot自动配置原理.md
│   │   └── 动手练习/
│   ├── 05-前端实战（4天）/
│   │   ├── 01-Vue2工程化与ElementUI（版本A）.md
│   │   ├── 02-Vue3工程化与ElementPlus（版本B）.md
│   │   ├── 03-部门管理页面开发.md
│   │   ├── 04-员工管理页面开发.md
│   │   ├── 05-登录与路由守卫.md
│   │   └── 动手练习/
│   ├── 06-部署运维（2天）/
│   │   ├── 01-Linux基础命令.md
│   │   ├── 02-Docker核心概念与命令.md
│   │   ├── 03-Docker-Compose部署.md
│   │   ├── 04-CI-CD入门（版本B）.md
│   │   └── 动手练习/
│   ├── 07-AI功能专题（版本B专属，3天）/
│   │   ├── 01-NL2SQL智能查询设计与实现.md
│   │   ├── 02-RAG制度知识库问答.md
│   │   ├── 03-AI简历解析与自动入库.md
│   │   ├── 04-Prompt工程与安全过滤.md
│   │   └── 动手练习/
│   └── 99-附录/
│       ├── A-常用SQL脚本.md
│       ├── B-API接口文档.md
│       ├── C-环境常见问题排查.md
│       └── D-简历融入话术模板.md
├── version-a/                         # 传统实训版代码
│   ├── README.md
│   ├── tlias-backend/                 # SpringBoot2后端
│   │   ├── pom.xml
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/com/tlias/
│   │   │       │   ├── controller/
│   │   │       │   ├── service/
│   │   │       │   ├── mapper/
│   │   │       │   ├── pojo/
│   │   │       │   ├── utils/
│   │   │       │   ├── filter/
│   │   │       │   ├── aspect/
│   │   │       │   ├── exception/
│   │   │       │   └── TliasApplication.java
│   │   │       └── resources/
│   │   │           ├── application.yml
│   │   │           └── mapper/*.xml
│   │   └── Dockerfile
│   ├── tlias-frontend/                # Vue2前端
│   │   ├── package.json
│   │   ├── vue.config.js
│   │   └── src/
│   │       ├── views/
│   │       ├── components/
│   │       ├── router/
│   │       ├── store/
│   │       ├── api/
│   │       ├── utils/
│   │       └── App.vue
│   ├── sql/
│   │   └── tlias_init.sql             # 数据库初始化脚本
│   └── docker-compose.yml             # 版本A一键启动
├── version-b/                         # AI增强实战版代码
│   ├── README.md
│   ├── tlias-pro-backend/             # 基于yudao-boot-mini改造
│   │   ├── pom.xml
│   │   ├── tlias-pro-server/          # 启动入口
│   │   ├── tlias-pro-framework/       # 公共框架（原yudao-framework改造）
│   │   ├── tlias-pro-module-system/   # 系统模块（用户/角色/菜单/部门）
│   │   ├── tlias-pro-module-infra/    # 基础设施（代码生成/文件/日志）
│   │   ├── tlias-pro-module-hr/       # 人事业务模块（新增：员工/岗位/考勤）
│   │   └── tlias-pro-module-ai/       # AI功能模块（新增：NL2SQL/RAG/简历解析）
│   ├── tlias-pro-frontend/            # Vue3前端（基于yudao-ui-admin-vue3改造）
│   │   ├── package.json
│   │   ├── vite.config.ts
│   │   └── src/
│   │       ├── views/hr/              # 人事管理页面
│   │       ├── views/ai/              # AI功能页面
│   │       ├── api/                   # API封装
│   │       └── ...
│   ├── sql/
│   │   └── tlias-pro_init.sql         # 数据库初始化脚本（含AI模块表）
│   └── docker-compose.yml             # 版本B一键启动（MySQL8+Redis+后端+前端）
├── shared/                            # 两个版本共享资源
│   ├── docker/
│   │   ├── mysql-init/                # MySQL初始化脚本
│   │   └── nginx.conf                 # Nginx配置模板
│   └── assets/
│       └── images/                    # 文档配图
└── .github/
    └── workflows/
        └── ci.yml                     # GitHub Actions CI配置（版本B）
```

---

## 二、版本A实施计划（传统实训版）

### 2.1 后端工程（tlias-backend）

**技术栈**：SpringBoot 2.7.x + JDK8 + 原生MyBatis（XML配置）+ MySQL8 + JWT + PageHelper + 阿里云OSS

| 步骤 | 任务 | 关键文件 | 说明 |
|------|------|----------|------|
| A-1 | 初始化SpringBoot项目 | `version-a/tlias-backend/pom.xml` | spring-boot-starter-parent 2.7.18，引入web、mybatis、mysql、lombok |
| A-2 | 配置MyBatis与数据源 | `application.yml`, `MybatisConfig.java` | 原生MyBatis配置，XML映射文件路径 |
| A-3 | 实现部门管理CRUD | `DeptController.java`, `DeptService.java`, `DeptMapper.java`, `DeptMapper.xml` | 列表查询、新增、删除、修改 |
| A-4 | 实现员工管理分页 | `EmpController.java`, `EmpService.java`, `EmpMapper.java` | PageHelper分页插件，条件动态SQL |
| A-5 | 实现文件上传 | `UploadController.java`, `AliyunOSSUtil.java` | 本地存储 + 阿里云OSS上传 |
| A-6 | 实现JWT登录认证 | `JwtUtils.java`, `LoginController.java` | JWT生成与校验，登录接口 |
| A-7 | 实现Filter拦截器 | `TokenFilter.java` | 拦截请求，校验Token有效性 |
| A-8 | 实现AOP操作日志 | `LogAspect.java`, `OperateLog.java`, `OperateLogMapper.java` | 环绕通知，记录方法名、参数、返回值、耗时 |
| A-9 | 全局异常处理 | `GlobalExceptionHandler.java` | @RestControllerAdvice统一处理异常 |
| A-10 | 多表查询 | `EmpMapper.xml` | 员工-部门关联查询，内连接/外连接 |

### 2.2 前端工程（tlias-frontend）

**技术栈**：Vue 2.6 + Vue CLI 5 + ElementUI 2.15 + Axios + Vue Router 3 + Vuex

| 步骤 | 任务 | 关键文件 | 说明 |
|------|------|----------|------|
| A-11 | 初始化Vue2项目 | `version-a/tlias-frontend/package.json` | vue-cli创建，引入element-ui、axios、vue-router、vuex |
| A-12 | 配置Axios拦截器 | `version-a/tlias-frontend/src/utils/request.js` | 请求拦截注入Token，响应拦截处理401 |
| A-13 | 部门管理页面 | `views/dept/index.vue` | 部门列表表格、新增/编辑对话框 |
| A-14 | 员工管理页面 | `views/emp/index.vue` | 分页表格、条件搜索表单、新增/编辑/删除 |
| A-15 | 登录页面 | `views/login/index.vue` | 表单校验、登录API调用、Token存储(localStorage) |
| A-16 | 路由与导航 | `router/index.js`, `layout/` | 侧边栏菜单、路由守卫（未登录跳转） |

### 2.3 数据库设计

**核心表**（`version-a/sql/tlias_init.sql`）：
- `dept`（部门表）：id, name, create_time, update_time
- `emp`（员工表）：id, username, password, name, gender, image, job, entrydate, dept_id, create_time, update_time
- `emp_expr`（工作经历表）：id, emp_id, begin, end, company, job
- `operate_log`（操作日志表）：id, operate_emp_id, operate_time, class_name, method_name, method_params, return_value, cost_time

### 2.4 Docker Compose部署

`version-a/docker-compose.yml` 包含：
- MySQL 8.0（端口3306，预执行tlias_init.sql）
- 后端SpringBoot（端口8080，依赖MySQL）
- 前端Nginx（端口80，静态资源代理）

---

## 三、版本B实施计划（AI增强实战版）

### 3.1 底座改造（基于yudao-boot-mini）

**来源**：https://github.com/yudaocode/yudao-boot-mini（master-jdk17分支，SpringBoot3.2 + JDK17）

| 步骤 | 任务 | 关键文件 | 说明 |
|------|------|----------|------|
| B-1 | Clone并裁剪底座 | `version-b/tlias-pro-backend/` | 仅保留system+infra模块，删除其他业务模块 |
| B-2 | 包名品牌化 | 全局替换 | `com.ruoyi` → `com.tliaspro`，`yudao` → `tliaspro` |
| B-3 | 配置Sa-Token | `application.yml` | 替换Spring Security为Sa-Token（可选：保留原安全配置） |
| B-4 | 验证底座可启动 | `TliasProServerApplication.java` | 确保裁剪后项目能正常启动、登录 |

### 3.2 人事业务模块扩展（tlias-pro-module-hr）

新建模块，参考vhr2.0业务域设计：

| 步骤 | 任务 | 说明 |
|------|------|------|
| B-5 | 部门管理 | 在system模块dept基础上扩展，支持树形结构 |
| B-6 | 员工管理 | 扩展emp表，增加工作经历、教育背景子表 |
| B-7 | 岗位/职称管理 | 参考vhr2.0的position、joblevel表 |
| B-8 | 考勤记录 | 简单考勤签到功能 |
| B-9 | 数据统计 | ECharts图表：部门人数分布、入职趋势 |

### 3.3 AI功能模块（tlias-pro-module-ai）

新建独立模块，技术选型：Spring AI + Kimi API（兼容OpenAI协议）

| 步骤 | 任务 | 关键类 | 说明 |
|------|------|--------|------|
| B-10 | 模块初始化 | `tlias-pro-module-ai/pom.xml` | 引入spring-ai-openai-starter、pdfbox、jsqlparser |
| B-11 | AI配置与客户端 | `config/AiConfig.java`, `service/KimiClient.java` | 配置base-url=https://api.moonshot.cn/v1，ChatClient构建 |
| B-12 | NL2SQL智能查询 | `service/Nl2SqlService.java`, `security/SqlSecurityFilter.java` | Prompt注入表结构 → LLM生成SQL → JSqlParser AST校验（仅SELECT）→ 执行返回 |
| B-13 | RAG知识库问答 | `service/RagService.java`, `service/DocumentIngestionService.java` | PDF/Word上传 → 文本分块 → Embedding → PGVector存储 → 语义检索 → 增强生成 |
| B-14 | AI简历解析 | `service/ResumeParseService.java`, `controller/ResumeParseController.java` | PDFBox文本提取 → Prompt结构化抽取 → BeanOutputConverter映射Java对象 → 前端预览确认 |
| B-15 | AI对话控制器 | `controller/AiChatController.java` | 流式SSE响应，前端实时显示 |
| B-16 | Mock兜底机制 | `service/MockAiService.java` | API失败时返回预设假数据，确保演示稳定 |

### 3.4 前端工程（tlias-pro-frontend）

基于yudao-ui-admin-vue3改造，新增AI页面：

| 步骤 | 任务 | 关键文件 | 说明 |
|------|------|----------|------|
| B-17 | 改造基础配置 | `vite.config.ts`, `package.json` | 更新API基础地址、项目名称 |
| B-18 | 人事管理页面 | `views/hr/` | 部门/员工/岗位管理页面 |
| B-19 | NL2SQL面板 | `views/ai/Nl2SqlPanel.vue` | 自然语言输入框 + 示例标签 + 结果表格 |
| B-20 | RAG知识库聊天 | `views/ai/RagChat.vue`, `components/AiChatBall.vue` | 悬浮AI助手球，Markdown渲染，流式输出 |
| B-21 | 简历解析上传 | `views/ai/ResumeUploader.vue` | 拖拽上传PDF，AI解析预览，字段人工修正 |
| B-22 | 数据统计图表 | `views/hr/Statistics.vue` | ECharts：部门分布饼图、入职趋势折线图 |

### 3.5 Docker Compose部署（含全部服务）

`version-b/docker-compose.yml` 包含：
- MySQL 8.0（端口3306）
- Redis 7.0（端口6379）
- MinIO（端口9000/9001，对象存储备选）
- 后端SpringBoot3（端口48080）
- 前端Nginx（端口80）

---

## 四、课程文档编写规范

每篇文档采用统一模板：

```markdown
# 章节标题

## 学习目标
- 学完本节后，你将能够...

## 核心知识点
### 1. 知识点A
理论解释 + 代码示例

### 2. 知识点B
...

## 动手练习
### 练习1：...
[步骤描述]

## 常见错误排查
| 错误现象 | 原因 | 解决方案 |
|---------|------|---------|
| ... | ... | ... |

## 本节小结
...
```

---

## 五、分阶段执行顺序

### 阶段1：基础设施搭建（第1-2轮）
1. 创建GitHub仓库，初始化目录结构
2. 编写根目录README.md（双版本对比、快速开始）
3. 编写Docker Compose基础配置（shared/docker/）
4. 验证Docker Compose在本地可一键启动

### 阶段2：版本A代码开发（第3-6轮）
1. 搭建tlias-backend（SpringBoot2 + 原生MyBatis）
2. 搭建tlias-frontend（Vue2 + ElementUI）
3. 实现完整CRUD + 登录认证 + AOP日志
4. 编写版本A对应的课程文档（01-06章节）
5. 验证版本A docker-compose可完整启动

### 阶段3：版本B底座改造（第7-8轮）
1. Clone yudao-boot-mini，裁剪保留system+infra
2. 包名品牌化，验证可启动
3. 扩展人事业务模块（tlias-pro-module-hr）
4. 搭建tlias-pro-frontend（Vue3 + ElementPlus）

### 阶段4：版本B AI模块开发（第9-11轮）
1. 创建tlias-pro-module-ai模块
2. 实现NL2SQL + RAG + 简历解析
3. 开发前端AI页面组件
4. 实现Mock兜底机制
5. 编写版本B专属课程文档（07-AI专题）

### 阶段5：文档完善与部署验证（第12轮）
1. 补全所有课程文档
2. 统一代码风格与注释
3. 验证两个版本docker-compose均可一键启动
4. 编写GitHub Actions CI配置
5. 最终README美化（徽章、截图、在线演示说明）

---

## 六、验证方法

### 6.1 本地验证清单

**版本A验证**：
```bash
cd version-a
docker-compose up -d
# 验证：
# 1. 访问 http://localhost 看到登录页
# 2. admin/admin登录成功
# 3. 部门列表显示正常
# 4. 员工分页查询正常
# 5. 新增/编辑/删除功能正常
# 6. 文件上传正常
# 7. JWT Token校验正常（清空localStorage后刷新跳转登录页）
```

**版本B验证**：
```bash
cd version-b
docker-compose up -d
# 验证：
# 1. 访问 http://localhost 看到登录页
# 2. admin/admin123登录成功（yudao默认账号）
# 3. 部门/员工管理CRUD正常
# 4. AI-NL2SQL面板输入"查询入职3个月以上的员工"返回正确结果
# 5. AI-RAG上传PDF后可问答
# 6. AI-简历解析上传PDF后正确提取信息
# 7. Redis缓存生效（可通过redis-cli验证）
```

### 6.2 CI验证
- GitHub Actions每次push自动运行Maven编译测试
- 前端npm run build无报错
- Docker镜像构建成功

---

## 七、关键风险与应对

| 风险 | 应对策略 |
|------|---------|
| yudao-boot-mini裁剪后启动失败 | 保留完整pom依赖，逐步删除非必要starter，每删一个验证启动 |
| AI API调用不稳定 | 实现三层兜底：Mock模式 → 本地Ollama → 预设假数据 |
| Vue3+TS前端编译报错 | 保持yudao-ui-admin-vue3原有配置，最小化改造 |
| 文档编写工作量巨大 | 复用现有实训方案report.md内容，结构化改写为章节文档 |
| 数据库初始化脚本冲突 | 版本A和版本B使用不同数据库名（tlias_db / tlias_pro_db） |
