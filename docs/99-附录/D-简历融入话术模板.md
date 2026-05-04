# 附录 D：简历融入话术模板

## 版本 A（传统实训版）

### 项目描述

**Tlias 人事管理系统**

基于 SpringBoot + Vue 的前后端分离人事管理系统，实现了部门/员工管理、文件上传、JWT 登录认证、AOP 操作日志等核心功能。

### 技术栈

- 后端：SpringBoot 2.7、MyBatis、MySQL 8、JWT、PageHelper
- 前端：Vue 2.6、ElementUI、Axios、Vue Router
- 运维：Docker、Docker Compose、Nginx

### 要点话术

1. **分层架构**：采用 Controller/Service/Mapper 三层架构，职责分离，代码解耦
2. **动态 SQL**：使用 MyBatis XML 动态 SQL（<where>、<if>、<foreach>）实现多条件组合查询
3. **分页查询**：集成 PageHelper 实现后端分页，提升大数据量查询性能
4. **登录认证**：基于 JWT Token 实现无状态认证，Filter 拦截校验
5. **AOP 日志**：自定义注解 + 切面编程记录操作日志，便于审计追踪
6. **全局异常**：统一异常处理器封装错误响应，避免敏感信息泄露
7. **容器化部署**：Docker Compose 一键部署，MySQL + 后端 + 前端完整技术栈

---

## 版本 B（AI 增强实战版）

### 项目描述

**Tlias Pro AI 智能人事管理系统**

基于 SpringBoot3 + Vue3 的企业级人事管理系统，在传统 CRUD 基础上集成 NL2SQL 智能查询、RAG 知识库问答、AI 简历解析三大 AI 功能模块。

### 技术栈

- 后端：SpringBoot 3.2、MyBatis-Plus、MySQL 8、Redis、Sa-Token、Spring AI
- 前端：Vue 3.5、TypeScript、ElementPlus、Pinia、Vite、ECharts
- AI：Kimi API（OpenAI 兼容协议）、NL2SQL、RAG、简历解析
- 运维：Docker Compose、GitHub Actions CI/CD

### 要点话术

1. **企业级底座**：基于 yudao-boot-mini 改造，多模块架构（system/infra/hr/ai），支持高扩展
2. **AI 智能查询（NL2SQL）**：用户用自然语言提问，系统自动生成并执行 SQL，JSqlParser 做 AST 安全校验
3. **RAG 知识库**：PDF/Word 文档分块入库，语义检索 + LLM 增强生成，实现制度文档智能问答
4. **AI 简历解析**：PDFBox 提取文本 → LLM 结构化抽取 → BeanOutputConverter 映射 Java 对象
5. **流式对话**：SSE 实现 AI 对话实时流式输出，前端 Markdown 渲染
6. **Mock 兜底**：API 异常时自动切换 Mock 数据，保障演示稳定性
7. **权限体系**：Sa-Token + RBAC，细粒度接口权限控制
8. **CI/CD**：GitHub Actions 自动编译测试，Docker 镜像构建

## 面试常见问题

**Q：JWT 和 Session 有什么区别？**
> JWT 是无状态的，服务端不存储会话信息，适合分布式和微服务。Session 是有状态的，需要服务端存储，适合单体应用。

**Q：MyBatis 和 MyBatis-Plus 有什么区别？**
> MyBatis-Plus 在 MyBatis 基础上提供了通用 CRUD、条件构造器、分页插件、代码生成器等增强功能，减少了样板代码。

**Q：NL2SQL 如何防止 SQL 注入？**
> 使用 JSqlParser 对 LLM 生成的 SQL 做 AST 语法解析，只允许 SELECT 语句，禁止 DELETE/DROP 等危险操作。

