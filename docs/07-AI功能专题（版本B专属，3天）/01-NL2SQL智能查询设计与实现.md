# NL2SQL 智能查询设计与实现

## 学习目标

- 理解 NL2SQL（自然语言转 SQL）的核心思路，能够向非技术人员解释其工作原理
- 掌握 Prompt 工程在 NL2SQL 中的应用，学会设计高质量的 Schema 注入模板
- 能够使用 Spring AI ChatClient 接入 Kimi 大模型，完成从自然语言到 SQL 的端到端转换
- 掌握 JSqlParser 实现 SQL 安全过滤，构建多层安全防护体系
- 能够独立完成 Nl2SqlService 的完整实现，包含 Prompt 构建、LLM 调用、SQL 解析、执行、结果封装
- 理解 NL2SQL 准确率优化的关键技巧，能够根据实际业务场景调整 Schema 描述

---

## 核心知识点

### 1. 为什么需要 NL2SQL

#### 1.1 非技术人员查数据的痛点

想象这样一个场景：公司老板想了解"入职3个月以上的本科前端工程师有多少人"，他不会写 SQL，也不会用 Navicat，只能找开发人员帮忙。开发人员收到需求后，要写 SQL、查数据、把结果截图发过去。一个简单的查询，来回沟通可能要十几分钟。

这就是传统数据查询的痛点：

| 痛点 | 具体表现 |
|------|---------|
| 技术门槛高 | 不会写 SQL，不会用数据库工具 |
| 沟通成本高 | 需求描述 → 开发人员理解 → 写 SQL → 反馈结果 |
| 响应速度慢 | 简单查询也要等开发人员有空 |
| 灵活性差 | 想换个条件查询，又要重新沟通 |

**NL2SQL 的解决方案**：让非技术人员用自然语言直接查询数据库，AI 自动翻译成 SQL 并返回结果。

```
老板输入：查询入职3个月以上的本科前端工程师
        ↓
    AI 翻译
        ↓
SQL 输出：SELECT * FROM emp WHERE job = 2 AND entrydate <= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
        ↓
    执行查询
        ↓
   返回表格结果
```

#### 1.2 NL2SQL 的两种技术方案对比

NL2SQL 领域存在两种主流实现路径，本项目采用方案一。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        NL2SQL 两种方案对比                                   │
├─────────────────────────────┬───────────────────────────────────────────────┤
│      方案一：Text-to-SQL     │         方案二：NL2DSL → SQL                  │
├─────────────────────────────┼───────────────────────────────────────────────┤
│                             │                                               │
│   自然语言 ──→ LLM ──→ SQL  │   自然语言 ──→ LLM ──→ DSL ──→ SQL生成器 ──→ SQL│
│        (端到端，本项目用)    │        (中间表示层，更复杂)                    │
│                             │                                               │
├─────────────────────────────┼───────────────────────────────────────────────┤
│ 优点：                       │ 优点：                                         │
│ - 实现简单，代码量少          │ - DSL 层可做更精细的语义校验                   │
│ - 直接利用 LLM 的推理能力     │ - 更容易集成业务规则                           │
│ - 开发周期短                 │ - 安全性更高（DSL 转 SQL 可控）                 │
├─────────────────────────────┼───────────────────────────────────────────────┤
│ 缺点：                       │ 缺点：                                         │
│ - 完全依赖 LLM 质量           │ - 实现复杂，需要维护 DSL 语法                   │
│ - 需要精心设计 Prompt         │ - 开发周期长                                   │
│ - 安全校验压力大             │ - 需要额外的 SQL 生成器模块                     │
├─────────────────────────────┼───────────────────────────────────────────────┤
│ 适用场景：                   │ 适用场景：                                     │
│ - 快速原型/MVP               │ - 企业级生产环境                               │
│ - 内部工具/后台系统          │ - 金融、医疗等高合规要求场景                    │
│ - 技术验证                   │ - 需要复杂权限控制的场景                        │
└─────────────────────────────┴───────────────────────────────────────────────┘
```

---

### 2. 核心实现流程

NL2SQL 的完整流程可以用以下 ASCII 流程图表示：

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  用户输入    │     │  Prompt 工程     │     │   调用 LLM      │
│ 自然语言问题 │────→│ (Schema + 规则   │────→│  (Kimi API)     │
│             │     │  + 少样本示例)   │     │                 │
└─────────────┘     └─────────────────┘     └────────┬────────┘
                                                     │
                                                     ↓
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  返回结果    │     │  执行 SQL 查询   │     │  SQL 安全校验    │
│ (表格/JSON) │←────│  (MyBatis-Plus)  │←────│ (多层防护体系)   │
│             │     │                 │     │                 │
└─────────────┘     └─────────────────┘     └─────────────────┘
```

**流程说明**：

1. **用户输入**：用户在聊天框中输入自然语言问题，如"查询所有讲师"
2. **Prompt 工程**：将数据库 Schema、安全规则、少样本示例组装成结构化 Prompt
3. **调用 LLM**：通过 Spring AI ChatClient 调用 Kimi API，获取生成的 SQL
4. **SQL 安全校验**：经过 4 层防护体系验证 SQL 的安全性
5. **执行 SQL 查询**：使用 MyBatis-Plus 或 JdbcTemplate 执行查询
6. **返回结果**：将查询结果以表格或 JSON 格式返回给前端展示

---

### 3. Prompt 工程详解

Prompt 工程是 NL2SQL 准确率的核心决定因素。一个好的 Prompt 能让 LLM"理解"你的数据库结构，生成正确的 SQL。

#### 3.1 Zero-shot vs Few-shot 提示

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Zero-shot vs Few-shot 对比                            │
├─────────────────────────────┬───────────────────────────────────────────────┤
│       Zero-shot（零样本）    │          Few-shot（少样本）                    │
├─────────────────────────────┼───────────────────────────────────────────────┤
│                             │                                               │
│  直接给 Schema，让 LLM      │  给 Schema + 几个问答示例，                    │
│  "自己悟"怎么写 SQL         │  让 LLM "照猫画虎"                            │
│                             │                                               │
├─────────────────────────────┼───────────────────────────────────────────────┤
│ 示例：                       │ 示例：                                         │
│                             │                                               │
│  表结构：emp(id, name, job)  │  表结构：emp(id, name, job)                   │
│                             │                                               │
│  问题：查询所有讲师           │  示例1：                                       │
│                             │  Q: 查询所有讲师                               │
│  （LLM 自己推理）             │  A: SELECT * FROM emp WHERE job = 2           │
│                             │                                               │
│                             │  示例2：                                       │
│                             │  Q: 统计每个部门的人数                          │
│                             │  A: SELECT d.name, COUNT(*) FROM emp e        │
│                             │      JOIN dept d ON e.dept_id = d.id          │
│                             │      GROUP BY d.name                          │
│                             │                                               │
│                             │  问题：查询入职3个月以上的员工                  │
│                             │  （LLM 参考示例格式生成）                       │
│                             │                                               │
├─────────────────────────────┼───────────────────────────────────────────────┤
│ 优点：简单，Prompt 短        │ 优点：准确率显著提升，格式更规范               │
│ 缺点：复杂查询容易出错        │ 缺点：Prompt 较长，消耗更多 Token              │
└─────────────────────────────┴───────────────────────────────────────────────┘
```

**建议**：本项目使用 Few-shot，在 Prompt 中嵌入 2-3 个典型问答示例，能显著提升复杂查询（如多表 JOIN、聚合统计）的准确率。

#### 3.2 Schema 注入的最佳实践

Schema 注入是指在 Prompt 中告诉 LLM "我的数据库有哪些表、每个表有哪些字段、字段是什么含义"。Schema 描述越详细，生成的 SQL 越准确。

**不好的 Schema 描述（LLM 容易猜错）**：

```text
表：emp(id, name, gender, job, entrydate, dept_id)
表：dept(id, name)
```

**好的 Schema 描述（LLM 能准确理解）**：

```text
数据库表结构：

【员工表】emp
- id: BIGINT 主键，员工ID
- username: VARCHAR(50) 用户名，登录账号
- password: VARCHAR(100) 密码，BCrypt加密存储
- name: VARCHAR(50) 姓名
- gender: TINYINT 性别，1=男，2=女
- image: VARCHAR(200) 头像URL
- job: TINYINT 职位，1=班主任，2=讲师，3=学工主管，4=教研主管，5=咨询师，6=其他
- entrydate: DATE 入职日期，格式 yyyy-MM-dd
- dept_id: BIGINT 所属部门ID，外键关联 dept.id
- phone: VARCHAR(20) 手机号
- email: VARCHAR(100) 邮箱
- status: TINYINT 状态，0=离职，1=在职
- create_time: DATETIME 创建时间
- update_time: DATETIME 更新时间

【部门表】dept
- id: BIGINT 主键，部门ID
- name: VARCHAR(50) 部门名称
- parent_id: BIGINT 父部门ID，0表示顶级部门
- sort: INT 排序号
- status: TINYINT 状态，0=停用，1=正常
```

**Schema 注入的关键要素**：

| 要素 | 为什么重要 | 示例 |
|------|-----------|------|
| 表名 + 中文注释 | LLM 知道表是做什么的 | `emp (员工表)` |
| 字段名 + 类型 | 确保生成正确的 SQL 语法 | `gender TINYINT` |
| 枚举值映射 | 避免 LLM 用字符串匹配数字 | `1=男，2=女` |
| 外键关系 | 帮助 LLM 生成正确的 JOIN | `dept_id 外键关联 dept.id` |
| 字段业务含义 | 帮助 LLM 理解用户意图 | `entrydate 是入职日期` |

#### 3.3 完整的 Prompt 模板

```java
// ============================================
// 系统 Prompt：定义 LLM 的角色和行为规则
// ============================================
private static final String SYSTEM_PROMPT = """
    你是一位专业的 MySQL 数据库专家。根据提供的数据库 Schema，将用户问题转换为 SQL 查询。
    
    【生成规则】
    1. 仅生成 SELECT 查询语句，禁止任何数据修改操作
    2. 禁止使用 UPDATE / DELETE / INSERT / DROP / ALTER / CREATE / GRANT / TRUNCATE 等关键字
    3. 如果用户问题涉及修改数据，返回 "INVALID_QUERY"
    4. 表名和字段名必须严格使用 Schema 中定义的名称
    5. 对于枚举字段，必须使用 Schema 中定义的数字值，不能使用中文
    6. 日期计算使用 MySQL 标准函数，如 DATE_SUB、DATEDIFF 等
    7. 查询结果默认添加 LIMIT 100 限制，防止返回过多数据
    
    【输出格式】
    只输出纯 SQL 语句，不要包含任何解释、注释或 markdown 代码块标记
    """;

// ============================================
// Schema 上下文：描述数据库结构
// ============================================
private static final String SCHEMA_CONTEXT = """
    数据库表结构：
    
    【员工表】emp
    - id: BIGINT 主键，员工ID
    - username: VARCHAR(50) 用户名，登录账号
    - password: VARCHAR(100) 密码，BCrypt加密存储（查询时勿返回）
    - name: VARCHAR(50) 姓名
    - gender: TINYINT 性别，1=男，2=女
    - image: VARCHAR(200) 头像URL
    - job: TINYINT 职位，1=班主任，2=讲师，3=学工主管，4=教研主管，5=咨询师，6=其他
    - entrydate: DATE 入职日期，格式 yyyy-MM-dd
    - dept_id: BIGINT 所属部门ID，外键关联 dept.id
    - phone: VARCHAR(20) 手机号
    - email: VARCHAR(100) 邮箱
    - status: TINYINT 状态，0=离职，1=在职
    - create_time: DATETIME 创建时间
    - update_time: DATETIME 更新时间
    
    【部门表】dept
    - id: BIGINT 主键，部门ID
    - name: VARCHAR(50) 部门名称
    - parent_id: BIGINT 父部门ID，0表示顶级部门
    - sort: INT 排序号
    - status: TINYINT 状态，0=停用，1=正常
    - create_time: DATETIME 创建时间
    - update_time: DATETIME 更新时间
    """;

// ============================================
// Few-shot 示例：给 LLM 看几个问答对
// ============================================
private static final String FEW_SHOT_EXAMPLES = """
    【示例问答】
    
    Q: 查询所有在职的讲师
    A: SELECT * FROM emp WHERE job = 2 AND status = 1 LIMIT 100
    
    Q: 统计每个部门的人数
    A: SELECT d.name, COUNT(*) as count FROM emp e JOIN dept d ON e.dept_id = d.id WHERE e.status = 1 GROUP BY d.name
    
    Q: 查询2022年以后入职的女员工
    A: SELECT * FROM emp WHERE gender = 2 AND entrydate >= '2022-01-01' AND status = 1 LIMIT 100
    """;
```

---

### 4. Spring AI ChatClient 完整接入

Spring AI 是 Spring 官方提供的 AI 应用开发框架，封装了各种大模型 API 的调用细节。本项目使用 Spring AI 的 `ChatClient` 与 Kimi（Moonshot）API 交互。

#### 4.1 依赖配置

在 `tlias-pro-module-ai/pom.xml` 中添加 Spring AI 依赖：

```xml
<!-- ============================================ -->
<!-- Spring AI OpenAI Starter                     -->
<!-- 注意：Kimi API 兼容 OpenAI 协议，所以使用 OpenAI Starter -->
<!-- ============================================ -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- ============================================ -->
<!-- JSqlParser：SQL 语法解析与安全校验            -->
<!-- ============================================ -->
<dependency>
    <groupId>com.github.jsqlparser</groupId>
    <artifactId>jsqlparser</artifactId>
    <version>4.9</version>
</dependency>
```

#### 4.2 Kimi API 配置

Kimi API 通过环境变量注入，在 `docker-compose.yml` 中配置：

```yaml
backend:
  environment:
    # Spring AI OpenAI 配置（Kimi 兼容 OpenAI 协议）
    SPRING_AI_OPENAI_API_KEY: ${KIMI_API_KEY:-mock-key}  # 从环境变量读取，默认 mock-key
    SPRING_AI_OPENAI_BASE_URL: https://api.moonshot.cn/v1  # Kimi API 基础地址
    SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL: moonshot-v1-8k   # 使用的模型
```

**本地开发时**，可以在 `application.yml` 中配置：

```yaml
spring:
  ai:
    openai:
      api-key: ${KIMI_API_KEY}           # 从环境变量读取，不要硬编码
      base-url: https://api.moonshot.cn/v1
      chat:
        options:
          model: moonshot-v1-8k          # 8k 上下文模型，足够 NL2SQL 使用
          temperature: 0.1               # 低温度，让输出更确定、更严谨
```

> **安全提示**：`api-key` 必须通过环境变量注入，禁止硬编码在代码或配置文件中。Kimi API Key 泄露可能导致账号被盗刷。

#### 4.3 ChatClient 链式调用

Spring AI 的 `ChatClient` 使用流畅的链式 API 设计：

```java
// ============================================
// ChatClient 链式调用示例
// ============================================
@Autowired
private ChatClient chatClient;  // Spring Boot 自动装配，无需手动创建

public String chatWithAi(String userMessage) {
    // 链式调用：构建 Prompt → 发送请求 → 获取响应
    String response = chatClient
        .prompt()                          // 1. 开始构建 Prompt
        .system("你是专业的数据库专家")     // 2. 设置系统消息（定义角色）
        .user(userMessage)                 // 3. 设置用户消息（具体问题）
        .call()                            // 4. 发送请求到 LLM
        .content();                        // 5. 提取响应文本内容
    
    return response;
}
```

**链式调用各方法说明**：

| 方法 | 作用 | 类比 |
|------|------|------|
| `.prompt()` | 开始构建一次对话请求 | 拿起电话准备拨号 |
| `.system(String)` | 设置系统消息，定义 AI 角色和行为规则 | 给 AI 发"入职培训手册" |
| `.user(String)` | 设置用户消息，即具体问题 | 说出你的问题 |
| `.call()` | 发送请求到 LLM API，等待响应 | 按下拨号键，等待对方接听 |
| `.content()` | 从响应中提取文本内容 | 记录对方的回答 |

---

### 5. SQL 安全校验多层防护

NL2SQL 最大的风险是 **SQL 注入** 和 **数据误操作**。如果用户输入"删除所有员工"，LLM 可能生成 `DELETE FROM emp`，这会造成严重数据损失。因此必须构建多层安全防护体系。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SQL 安全校验 4 层防护体系                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐         │
│  │  第 1 层         │    │  第 2 层         │    │  第 3 层         │         │
│  │  Prompt 声明     │───→│  正则表达式过滤   │───→│  JSqlParser     │         │
│  │  "仅 SELECT"    │    │  关键字黑名单    │    │  语法树解析      │         │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘         │
│         ↓                      ↓                      ↓                     │
│    在系统 Prompt         用正则匹配           解析 SQL 语法树               │
│    中明确声明规则        DROP/DELETE/UPDATE    验证是否为 SELECT 语句       │
│                                                                             │
│                              ┌─────────────────┐                           │
│                              │  第 4 层         │                           │
│                              │  执行时防护      │                           │
│                              │  LIMIT + 超时   │                           │
│                              └─────────────────┘                           │
│                                   ↓                                        │
│                            查询添加 LIMIT，                                │
│                            设置执行超时时间                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 5.1 第 1 层：Prompt 中声明安全规则

在系统 Prompt 中明确告诉 LLM "你只能生成 SELECT"：

```java
.system("""
    你是一位专业的 MySQL 数据库专家。
    【安全规则 - 严格遵守】
    1. 仅生成 SELECT 查询语句
    2. 禁止使用 UPDATE / DELETE / INSERT / DROP / ALTER / CREATE / GRANT / TRUNCATE
    3. 如果问题涉及修改数据，返回 "INVALID_QUERY"
    """)
```

**作用**：这是"软约束"，依赖 LLM 的指令遵循能力。大多数现代 LLM（如 Kimi、GPT-4）能很好地遵守这类规则，但不能 100% 保证。

#### 5.2 第 2 层：正则表达式过滤关键字

在拿到 LLM 返回的 SQL 后，先用正则表达式做快速黑名单检查：

```java
// ============================================
// 第 2 层：正则表达式关键字黑名单过滤
// ============================================
private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
    // 匹配危险关键字，不区分大小写
    "\\b(DROP|DELETE|UPDATE|INSERT|ALTER|CREATE|GRANT|TRUNCATE|EXEC|EXECUTE)\\b",
    Pattern.CASE_INSENSITIVE
);

/**
 * 正则表达式安全过滤
 * @param sql LLM 生成的 SQL 语句
 * @throws SecurityException 如果包含危险关键字
 */
private void validateByRegex(String sql) {
    // 使用正则匹配危险关键字
    if (DANGEROUS_SQL_PATTERN.matcher(sql).find()) {
        // 匹配到危险关键字，抛出安全异常
        throw new SecurityException(
            "SQL 包含危险关键字，已被拦截。仅允许 SELECT 查询。"
        );
    }
}
```

**代码逐行注释**：

- `Pattern.compile(..., Pattern.CASE_INSENSITIVE)`：编译正则表达式，不区分大小写（防止 `drop`、`Drop`、`DROP` 绕过）
- `\\b...\\b`：单词边界匹配，确保匹配的是完整单词（避免 `select` 中的 `ec` 被误判）
- `.find()`：在 SQL 字符串中查找是否包含匹配项
- 如果匹配到任何危险关键字，立即抛出 `SecurityException`，阻止后续执行

#### 5.3 第 3 层：JSqlParser 语法树解析验证

JSqlParser 是一个开源的 SQL 解析器，能将 SQL 字符串解析成语法树，从而精确判断 SQL 语句的类型。

```java
import net.sf.jsqlparser.parser.CCJSqlParserUtil;   // SQL 解析工具类
import net.sf.jsqlparser.statement.Statement;        // SQL 语句抽象基类
import net.sf.jsqlparser.statement.select.Select;    // SELECT 语句类型

// ============================================
// 第 3 层：JSqlParser 语法树解析验证
// ============================================
/**
 * JSqlParser 安全校验
 * 将 SQL 解析为语法树，精确判断是否为 SELECT 语句
 * @param sql LLM 生成的 SQL 语句
 * @throws SecurityException 如果不是 SELECT 语句或解析失败
 */
private void validateByJSqlParser(String sql) {
    try {
        // CCJSqlParserUtil.parse() 将 SQL 字符串解析为 Statement 对象
        // 如果 SQL 语法错误，会抛出 JSQLParserException
        Statement statement = CCJSqlParserUtil.parse(sql);
        
        // instanceof 判断解析后的语句是否为 SELECT 类型
        // Statement 的子类包括：Select、Update、Delete、Insert、CreateTable 等
        if (!(statement instanceof Select)) {
            // 不是 SELECT 语句，抛出安全异常
            throw new SecurityException(
                "仅允许 SELECT 查询，禁止 DML/DDL 操作。检测到语句类型：" 
                + statement.getClass().getSimpleName()
            );
        }
        
        // 可选：进一步检查是否包含子查询中的危险操作
        // 例如：SELECT * FROM (DROP TABLE emp) t
        // 这种复杂情况需要遍历语法树，本项目基础版暂不深入
        
    } catch (SecurityException e) {
        // 安全异常直接抛出
        throw e;
    } catch (Exception e) {
        // SQL 解析失败（语法错误），也视为不安全
        throw new SecurityException(
            "SQL 解析失败或包含非法语句：" + e.getMessage()
        );
    }
}
```

**代码逐行注释**：

- `CCJSqlParserUtil.parse(sql)`：将 SQL 字符串解析为 `Statement` 对象，这是 JSqlParser 的核心方法
- `Statement`：所有 SQL 语句的抽象基类，子类包括 `Select`、`Update`、`Delete`、`Insert` 等
- `instanceof Select`：精确判断语句类型，只有 `SELECT` 语句能通过校验
- 异常处理分为两类：安全异常直接抛出；解析异常（如语法错误）也视为不安全

#### 5.4 第 4 层：执行时 LIMIT 限制 + 超时控制

即使 SQL 是 SELECT，也可能返回海量数据导致系统崩溃。执行时需要做最后一层防护。

```java
// ============================================
// 第 4 层：执行时防护
// ============================================
/**
 * 安全执行 SQL 查询
 * @param sql 已校验的 SELECT 语句
 * @return 查询结果列表
 */
private List<Map<String, Object>> executeSafely(String sql) {
    // 1. 强制添加 LIMIT 限制（如果原 SQL 没有 LIMIT）
    String safeSql = addLimitIfMissing(sql, 100);
    
    // 2. 使用 JdbcTemplate 执行查询，设置超时时间
    // queryForList 将结果封装为 List<Map<列名, 列值>>
    return jdbcTemplate.query(safeSql, rs -> {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                // 使用列名作为 key，列值作为 value
                row.put(metaData.getColumnLabel(i), rs.getObject(i));
            }
            results.add(row);
        }
        return results;
    });
}

/**
 * 如果 SQL 没有 LIMIT，自动添加
 * @param sql 原始 SQL
 * @param maxRows 最大返回行数
 * @return 带 LIMIT 的安全 SQL
 */
private String addLimitIfMissing(String sql, int maxRows) {
    // 正则检查 SQL 是否已包含 LIMIT（不区分大小写）
    if (!sql.matches("(?i).*\\bLIMIT\\b.*")) {
        // 没有 LIMIT，在末尾添加
        return sql + " LIMIT " + maxRows;
    }
    return sql;
}
```

**代码逐行注释**：

- `addLimitIfMissing(sql, 100)`：检查 SQL 是否包含 `LIMIT`，如果没有则自动追加 `LIMIT 100`
- `jdbcTemplate.query(...)`：使用 Spring 的 JdbcTemplate 执行查询，比 MyBatis-Plus 更灵活（可直接执行动态 SQL）
- `ResultSetMetaData`：获取结果集的元数据（列名、列类型等），用于动态构建返回结果
- `LinkedHashMap`：保持列的顺序，让前端表格显示时列顺序与 SQL 一致

---

### 6. 完整 Service 实现代码

#### 6.1 Nl2SqlService 完整类

```java
package com.tliaspro.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * NL2SQL 智能查询服务
 * 
 * 核心职责：
 * 1. 接收自然语言问题
 * 2. 构建 Prompt（Schema + 规则 + 示例）
 * 3. 调用 Kimi LLM 生成 SQL
 * 4. 多层安全校验（正则 + JSqlParser）
 * 5. 安全执行查询并返回结果
 * 
 * 安全防护：4 层防护体系
 * - 第 1 层：Prompt 中声明仅 SELECT
 * - 第 2 层：正则表达式过滤危险关键字
 * - 第 3 层：JSqlParser 语法树解析验证
 * - 第 4 层：执行时 LIMIT 限制
 */
@Slf4j
@Service
public class Nl2SqlService {

    // ============================================
    // 依赖注入
    // ============================================
    
    /** Spring AI ChatClient，用于调用 Kimi LLM */
    @Autowired
    private ChatClient chatClient;
    
    /** JdbcTemplate，用于执行动态 SQL 查询 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ============================================
    // 常量定义：Prompt 模板
    // ============================================
    
    /** 
     * 系统 Prompt：定义 AI 角色和安全规则
     * 这是第 1 层安全防护：在 Prompt 中明确声明只能生成 SELECT
     */
    private static final String SYSTEM_PROMPT = """
        你是一位专业的 MySQL 数据库专家。根据提供的数据库 Schema，将用户问题转换为 SQL 查询。
        
        【安全规则 - 严格遵守】
        1. 仅生成 SELECT 查询语句，禁止任何数据修改操作
        2. 禁止使用 UPDATE / DELETE / INSERT / DROP / ALTER / CREATE / GRANT / TRUNCATE
        3. 如果用户问题涉及修改数据，返回 "INVALID_QUERY"
        4. 表名和字段名必须严格使用 Schema 中定义的名称
        5. 对于枚举字段，必须使用 Schema 中定义的数字值，不能使用中文
        6. 日期计算使用 MySQL 标准函数，如 DATE_SUB、DATEDIFF 等
        7. 查询结果默认添加 LIMIT 100 限制
        
        【输出格式】
        只输出纯 SQL 语句，不要包含任何解释、注释或 markdown 代码块标记
        """;

    /** 
     * Schema 上下文：描述数据库表结构
     * 越详细的 Schema 描述，LLM 生成的 SQL 越准确
     */
    private static final String SCHEMA_CONTEXT = """
        数据库表结构：
        
        【员工表】emp
        - id: BIGINT 主键，员工ID
        - username: VARCHAR(50) 用户名，登录账号
        - password: VARCHAR(100) 密码，BCrypt加密存储（查询时勿返回）
        - name: VARCHAR(50) 姓名
        - gender: TINYINT 性别，1=男，2=女
        - image: VARCHAR(200) 头像URL
        - job: TINYINT 职位，1=班主任，2=讲师，3=学工主管，4=教研主管，5=咨询师，6=其他
        - entrydate: DATE 入职日期，格式 yyyy-MM-dd
        - dept_id: BIGINT 所属部门ID，外键关联 dept.id
        - phone: VARCHAR(20) 手机号
        - email: VARCHAR(100) 邮箱
        - status: TINYINT 状态，0=离职，1=在职
        - create_time: DATETIME 创建时间
        - update_time: DATETIME 更新时间
        
        【部门表】dept
        - id: BIGINT 主键，部门ID
        - name: VARCHAR(50) 部门名称
        - parent_id: BIGINT 父部门ID，0表示顶级部门
        - sort: INT 排序号
        - status: TINYINT 状态，0=停用，1=正常
        - create_time: DATETIME 创建时间
        - update_time: DATETIME 更新时间
        """;

    /** 
     * Few-shot 示例：给 LLM 看几个问答对，提升准确率
     * 这是 Prompt 工程的核心技巧之一
     */
    private static final String FEW_SHOT_EXAMPLES = """
        【示例问答】
        
        Q: 查询所有在职的讲师
        A: SELECT * FROM emp WHERE job = 2 AND status = 1 LIMIT 100
        
        Q: 统计每个部门的人数
        A: SELECT d.name, COUNT(*) as count FROM emp e JOIN dept d ON e.dept_id = d.id WHERE e.status = 1 GROUP BY d.name
        
        Q: 查询2022年以后入职的女员工
        A: SELECT * FROM emp WHERE gender = 2 AND entrydate >= '2022-01-01' AND status = 1 LIMIT 100
        """;

    // ============================================
    // 常量定义：安全校验
    // ============================================
    
    /** 
     * 危险 SQL 关键字正则表达式
     * 第 2 层安全防护：快速黑名单检查
     * \\b 表示单词边界，防止部分匹配（如 "select" 中的 "ec"）
     * CASE_INSENSITIVE 表示不区分大小写
     */
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
        "\\b(DROP|DELETE|UPDATE|INSERT|ALTER|CREATE|GRANT|TRUNCATE|EXEC|EXECUTE)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // ============================================
    // 核心方法：自然语言转 SQL 并执行
    // ============================================
    
    /**
     * NL2SQL 主入口方法
     * 完整流程：Prompt 构建 → LLM 调用 → 安全校验 → SQL 执行 → 结果封装
     * 
     * @param question 用户的自然语言问题，如"查询所有讲师"
     * @return Nl2SqlResult 包含生成的 SQL 和查询结果
     */
    public Nl2SqlResult queryByNaturalLanguage(String question) {
        // 1. 记录用户输入，便于后续审计和调试
        log.info("NL2SQL 查询请求：question={}", question);
        
        // 2. 构建完整 Prompt（系统消息 + Schema + 示例 + 用户问题）
        String fullPrompt = buildPrompt(question);
        log.debug("完整 Prompt：{}", fullPrompt);
        
        // 3. 调用 Kimi LLM 生成 SQL
        String generatedSql = callLlm(fullPrompt);
        log.info("AI 生成 SQL：{}", generatedSql);
        
        // 4. 多层安全校验（第 2 层 + 第 3 层）
        validateSqlSafety(generatedSql);
        
        // 5. 安全执行 SQL 查询（第 4 层防护）
        List<Map<String, Object>> queryResult = executeQuerySafely(generatedSql);
        log.info("查询结果行数：{}", queryResult.size());
        
        // 6. 封装结果返回
        return new Nl2SqlResult(generatedSql, queryResult);
    }

    /**
     * 仅生成 SQL，不执行（用于前端预览）
     * 
     * @param question 用户的自然语言问题
     * @return 生成的 SQL 字符串
     */
    public String generateSql(String question) {
        log.info("NL2SQL 生成 SQL：question={}", question);
        
        // 构建 Prompt 并调用 LLM
        String fullPrompt = buildPrompt(question);
        String generatedSql = callLlm(fullPrompt);
        
        // 安全校验（生成时也要校验）
        validateSqlSafety(generatedSql);
        
        log.info("AI 生成 SQL：{}", generatedSql);
        return generatedSql;
    }

    // ============================================
    // 私有方法：Prompt 构建
    // ============================================
    
    /**
     * 构建完整的用户 Prompt
     * 将 Schema、示例、用户问题组装成一条消息
     * 
     * @param question 用户问题
     * @return 完整的 Prompt 字符串
     */
    private String buildPrompt(String question) {
        // 使用 StringBuilder 拼接，效率更高
        StringBuilder prompt = new StringBuilder();
        
        // 先放 Schema 描述
        prompt.append(SCHEMA_CONTEXT).append("\n\n");
        
        // 再放 Few-shot 示例
        prompt.append(FEW_SHOT_EXAMPLES).append("\n\n");
        
        // 最后放用户问题
        prompt.append("Q: ").append(question).append("\n");
        prompt.append("A: ");
        
        return prompt.toString();
    }

    // ============================================
    // 私有方法：LLM 调用
    // ============================================
    
    /**
     * 调用 Kimi LLM 生成 SQL
     * 使用 Spring AI ChatClient 的链式 API
     * 
     * @param prompt 完整的 Prompt 字符串
     * @return LLM 生成的 SQL 字符串
     */
    private String callLlm(String prompt) {
        // ChatClient 链式调用
        String response = chatClient
            .prompt()                    // 开始构建 Prompt
            .system(SYSTEM_PROMPT)       // 设置系统消息（角色定义 + 安全规则）
            .user(prompt)                // 设置用户消息（Schema + 示例 + 问题）
            .call()                      // 发送请求到 Kimi API
            .content();                  // 提取响应文本
        
        // 清理响应：去除首尾空白和 markdown 代码块标记
        String sql = response.trim();
        sql = sql.replaceAll("^```sql\\s*", "");   // 去除开头的 ```sql
        sql = sql.replaceAll("^```\\s*", "");      // 去除开头的 ```
        sql = sql.replaceAll("\\s*```$", "");      // 去除结尾的 ```
        
        return sql.trim();
    }

    // ============================================
    // 私有方法：安全校验（第 2 层 + 第 3 层）
    // ============================================
    
    /**
     * SQL 安全校验入口
     * 依次执行正则过滤和 JSqlParser 语法解析
     * 
     * @param sql LLM 生成的 SQL 语句
     * @throws SecurityException 如果 SQL 不安全
     */
    private void validateSqlSafety(String sql) {
        // 第 2 层：正则表达式关键字黑名单过滤
        validateByRegex(sql);
        
        // 第 3 层：JSqlParser 语法树解析验证
        validateByJSqlParser(sql);
    }

    /**
     * 第 2 层安全校验：正则表达式过滤
     * 快速检查是否包含危险关键字
     * 
     * @param sql SQL 语句
     * @throws SecurityException 如果包含危险关键字
     */
    private void validateByRegex(String sql) {
        // 使用预编译的正则表达式匹配危险关键字
        if (DANGEROUS_SQL_PATTERN.matcher(sql).find()) {
            log.warn("SQL 安全拦截（正则层）：包含危险关键字，sql={}", sql);
            throw new SecurityException(
                "SQL 包含危险关键字，已被拦截。仅允许 SELECT 查询。"
            );
        }
    }

    /**
     * 第 3 层安全校验：JSqlParser 语法树解析
     * 精确判断 SQL 语句类型，只有 SELECT 能通过
     * 
     * @param sql SQL 语句
     * @throws SecurityException 如果不是 SELECT 或解析失败
     */
    private void validateByJSqlParser(String sql) {
        try {
            // 将 SQL 字符串解析为语法树
            Statement statement = CCJSqlParserUtil.parse(sql);
            
            // 精确判断是否为 SELECT 语句
            if (!(statement instanceof Select)) {
                log.warn("SQL 安全拦截（JSqlParser层）：非 SELECT 语句，type={}", 
                    statement.getClass().getSimpleName());
                throw new SecurityException(
                    "仅允许 SELECT 查询，禁止 DML/DDL 操作。"
                );
            }
            
        } catch (SecurityException e) {
            // 安全异常直接向上抛出
            throw e;
        } catch (Exception e) {
            // SQL 语法解析失败，视为不安全
            log.warn("SQL 解析失败：{}", e.getMessage());
            throw new SecurityException(
                "SQL 解析失败或包含非法语句：" + e.getMessage()
            );
        }
    }

    // ============================================
    // 私有方法：安全执行查询（第 4 层）
    // ============================================
    
    /**
     * 安全执行 SQL 查询
     * 自动添加 LIMIT 限制，使用 JdbcTemplate 执行
     * 
     * @param sql 已通过安全校验的 SELECT 语句
     * @return 查询结果列表，每行是一个 Map<列名, 列值>
     */
    private List<Map<String, Object>> executeQuerySafely(String sql) {
        // 强制添加 LIMIT 限制（如果原 SQL 没有）
        String safeSql = addLimitIfMissing(sql, 100);
        log.debug("安全 SQL（带 LIMIT）：{}", safeSql);
        
        // 使用 JdbcTemplate 执行查询
        // query 方法的第二个参数是 ResultSetExtractor，用于手动处理结果集
        return jdbcTemplate.query(safeSql, rs -> {
            List<Map<String, Object>> results = new ArrayList<>();
            
            // 获取结果集的元数据（列名、列数等信息）
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 逐行读取结果
            while (rs.next()) {
                // 使用 LinkedHashMap 保持列的顺序
                Map<String, Object> row = new LinkedHashMap<>();
                
                // 遍历每一列
                for (int i = 1; i <= columnCount; i++) {
                    // getColumnLabel 获取列别名（如果有），getColumnName 获取原始列名
                    String columnName = metaData.getColumnLabel(i);
                    // getObject 获取列值，自动映射为对应的 Java 类型
                    Object columnValue = rs.getObject(i);
                    row.put(columnName, columnValue);
                }
                
                results.add(row);
            }
            
            return results;
        });
    }

    /**
     * 如果 SQL 没有 LIMIT，自动添加
     * 使用正则表达式检查，不区分大小写
     * 
     * @param sql 原始 SQL
     * @param maxRows 最大返回行数
     * @return 带 LIMIT 的安全 SQL
     */
    private String addLimitIfMissing(String sql, int maxRows) {
        // (?i) 表示不区分大小写，\\bLIMIT\\b 匹配单词 LIMIT
        if (!sql.matches("(?i).*\\bLIMIT\\b.*")) {
            return sql + " LIMIT " + maxRows;
        }
        return sql;
    }

    // ============================================
    // 结果封装类
    // ============================================
    
    /**
     * NL2SQL 查询结果封装类
     * 包含生成的 SQL 和查询结果数据
     */
    public static class Nl2SqlResult {
        /** AI 生成的 SQL 语句 */
        private final String sql;
        /** 查询结果，每行是一个 Map<列名, 列值> */
        private final List<Map<String, Object>> data;
        
        public Nl2SqlResult(String sql, List<Map<String, Object>> data) {
            this.sql = sql;
            this.data = data;
        }
        
        public String getSql() {
            return sql;
        }
        
        public List<Map<String, Object>> getData() {
            return data;
        }
    }
}
```

#### 6.2 Controller 接口

```java
package com.tliaspro.module.ai.controller;

import com.tliaspro.module.ai.service.Nl2SqlService;
import com.tliaspro.module.ai.service.Nl2SqlService.Nl2SqlResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 功能统一控制器
 * 
 * 提供 NL2SQL、RAG、简历解析等 AI 能力的 REST API 接口
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    /** 注入 NL2SQL 服务 */
    @Autowired
    private Nl2SqlService nl2SqlService;

    // ============================================
    // NL2SQL 接口
    // ============================================
    
    /**
     * NL2SQL 智能查询接口
     * 接收自然语言问题，返回生成的 SQL 和查询结果
     * 
     * 请求示例：
     * POST /ai/nl2sql
     * Content-Type: application/json
     * 
     * {
     *   "question": "查询所有在职的讲师"
     * }
     * 
     * 响应示例：
     * {
     *   "success": true,
     *   "data": {
     *     "sql": "SELECT * FROM emp WHERE job = 2 AND status = 1 LIMIT 100",
     *     "data": [
     *       {"id": 3, "name": "李四", "gender": 2, "job": 2, ...},
     *       ...
     *     ]
     *   }
     * }
     * 
     * @param body 请求体，包含 "question" 字段
     * @return 包含 sql 和 data 的响应
     */
    @PostMapping("/nl2sql")
    public Map<String, Object> nl2sql(@RequestBody Map<String, String> body) {
        // 从请求体中提取用户问题
        String question = body.get("question");
        
        // 参数校验：问题不能为空
        if (question == null || question.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "问题不能为空");
            return error;
        }
        
        try {
            // 调用 Service 层：自然语言 → SQL → 执行 → 结果
            Nl2SqlResult result = nl2SqlService.queryByNaturalLanguage(question);
            
            // 封装成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sql", result.getSql());      // 生成的 SQL
            response.put("data", result.getData());    // 查询结果
            return response;
            
        } catch (SecurityException e) {
            // 安全异常：SQL 被拦截
            log.warn("NL2SQL 安全拦截：{}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询被安全系统拦截：" + e.getMessage());
            return error;
            
        } catch (Exception e) {
            // 其他异常：LLM 调用失败、数据库连接失败等
            log.error("NL2SQL 调用失败：{}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败：" + e.getMessage());
            return error;
        }
    }

    /**
     * NL2SQL SQL 预览接口
     * 仅生成 SQL，不执行查询（用于前端展示 SQL 预览）
     * 
     * @param body 请求体，包含 "question" 字段
     * @return 包含生成的 SQL
     */
    @PostMapping("/nl2sql/preview")
    public Map<String, Object> nl2sqlPreview(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        
        if (question == null || question.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "问题不能为空");
            return error;
        }
        
        try {
            String sql = nl2SqlService.generateSql(question);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sql", sql);
            return response;
            
        } catch (SecurityException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "SQL 被安全系统拦截：" + e.getMessage());
            return error;
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "生成失败：" + e.getMessage());
            return error;
        }
    }
}
```

#### 6.3 前端交互实现（Vue3 + ElementPlus）

```vue
<!--
  NL2SQL 智能查询页面
  技术栈：Vue3 + ElementPlus + Axios
  功能：自然语言输入 → SQL 预览 → 结果展示
-->
<template>
  <div class="nl2sql-container">
    <!-- 页面标题 -->
    <h2>智能数据查询</h2>
    <p class="subtitle">用自然语言查询数据库，AI 自动转换为 SQL</p>
    
    <!-- 输入区域 -->
    <el-card class="input-card">
      <el-input
        v-model="question"
        type="textarea"
        :rows="3"
        placeholder="例如：查询所有在职的讲师"
        @keyup.enter.ctrl="handleQuery"
      />
      
      <!-- 快捷示例按钮 -->
      <div class="examples">
        <span>试试这些：</span>
        <el-tag 
          v-for="ex in examples" 
          :key="ex"
          class="example-tag"
          @click="question = ex"
        >
          {{ ex }}
        </el-tag>
      </div>
      
      <!-- 操作按钮 -->
      <div class="actions">
        <el-button 
          type="primary" 
          :loading="loading"
          @click="handleQuery"
        >
          生成 SQL 并查询
        </el-button>
        <el-button 
          :loading="previewLoading"
          @click="handlePreview"
        >
          仅预览 SQL
        </el-button>
      </div>
    </el-card>
    
    <!-- SQL 预览区域 -->
    <el-card v-if="generatedSql" class="sql-card">
      <template #header>
        <span>生成的 SQL</span>
        <el-button 
          text 
          size="small"
          @click="copySql"
        >
          复制
        </el-button>
      </template>
      <pre class="sql-code">{{ generatedSql }}</pre>
    </el-card>
    
    <!-- 查询结果表格 -->
    <el-card v-if="queryResult.length > 0" class="result-card">
      <template #header>
        <span>查询结果（共 {{ queryResult.length }} 条）</span>
      </template>
      
      <!-- 动态列表格 -->
      <el-table :data="queryResult" border stripe>
        <el-table-column
          v-for="col in resultColumns"
          :key="col"
          :prop="col"
          :label="col"
          min-width="120"
        />
      </el-table>
    </el-card>
    
    <!-- 空状态 -->
    <el-empty 
      v-if="!loading && !generatedSql && queried"
      description="未查询到数据"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

// ============================================
// 响应式数据
// ============================================

/** 用户输入的自然语言问题 */
const question = ref('')

/** 生成的 SQL 语句 */
const generatedSql = ref('')

/** 查询结果数据 */
const queryResult = ref([])

/** 是否正在查询中 */
const loading = ref(false)

/** 是否正在预览 SQL */
const previewLoading = ref(false)

/** 是否已经执行过查询（用于控制空状态显示） */
const queried = ref(false)

/** 快捷示例问题列表 */
const examples = [
  '查询所有在职的讲师',
  '统计每个部门的人数',
  '查询2022年以后入职的女员工',
  '查询学工部有哪些人'
]

// ============================================
// 计算属性
// ============================================

/**
 * 从查询结果中提取列名
 * 用于动态渲染表格列
 */
const resultColumns = computed(() => {
  if (queryResult.value.length === 0) return []
  // 取第一行的所有 key 作为列名
  return Object.keys(queryResult.value[0])
})

// ============================================
// 方法
// ============================================

/**
 * 生成 SQL 并执行查询
 * 调用后端 /ai/nl2sql 接口
 */
const handleQuery = async () => {
  // 校验输入不能为空
  if (!question.value.trim()) {
    ElMessage.warning('请输入查询问题')
    return
  }
  
  loading.value = true
  queried.value = true
  
  try {
    // 发送 POST 请求到后端
    const response = await axios.post('/api/ai/nl2sql', {
      question: question.value.trim()
    })
    
    const result = response.data
    
    if (result.success) {
      // 成功：显示 SQL 和结果
      generatedSql.value = result.sql
      queryResult.value = result.data || []
      ElMessage.success('查询成功')
    } else {
      // 后端返回业务错误
      ElMessage.error(result.message || '查询失败')
      generatedSql.value = ''
      queryResult.value = []
    }
    
  } catch (error) {
    // 网络错误或服务器错误
    ElMessage.error('请求失败：' + (error.response?.data?.message || error.message))
    generatedSql.value = ''
    queryResult.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 仅预览 SQL，不执行查询
 * 调用后端 /ai/nl2sql/preview 接口
 */
const handlePreview = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入查询问题')
    return
  }
  
  previewLoading.value = true
  
  try {
    const response = await axios.post('/api/ai/nl2sql/preview', {
      question: question.value.trim()
    })
    
    const result = response.data
    
    if (result.success) {
      generatedSql.value = result.sql
      ElMessage.success('SQL 生成成功')
    } else {
      ElMessage.error(result.message || '生成失败')
    }
    
  } catch (error) {
    ElMessage.error('请求失败：' + (error.response?.data?.message || error.message))
  } finally {
    previewLoading.value = false
  }
}

/**
 * 复制 SQL 到剪贴板
 */
const copySql = () => {
  navigator.clipboard.writeText(generatedSql.value)
  ElMessage.success('SQL 已复制')
}
</script>

<style scoped>
.nl2sql-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.subtitle {
  color: #666;
  margin-bottom: 20px;
}

.input-card {
  margin-bottom: 20px;
}

.examples {
  margin-top: 12px;
}

.example-tag {
  margin-right: 8px;
  cursor: pointer;
}

.actions {
  margin-top: 16px;
  text-align: right;
}

.sql-card {
  margin-bottom: 20px;
}

.sql-code {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
}

.result-card {
  margin-bottom: 20px;
}
</style>
```

---

### 7. 准确率优化技巧

NL2SQL 的准确率直接决定用户体验。以下是经过实践验证的优化技巧：

#### 7.1 Schema 描述优化

| 优化技巧 | 效果 | 示例 |
|---------|------|------|
| 添加字段注释 | 帮助 LLM 理解字段含义 | `job: TINYINT 职位，1=班主任，2=讲师` |
| 标注枚举值映射 | 避免 LLM 用中文匹配数字 | `gender: TINYINT 性别，1=男，2=女` |
| 标注外键关系 | 帮助生成正确的 JOIN | `dept_id 外键关联 dept.id` |
| 标注日期格式 | 帮助生成正确的日期比较 | `entrydate: DATE 入职日期，格式 yyyy-MM-dd` |
| 标注敏感字段 | 避免返回敏感信息 | `password: 密码（查询时勿返回）` |

#### 7.2 Few-shot 示例设计

少样本示例的质量比数量更重要。建议遵循以下原则：

1. **覆盖常见查询类型**：单表查询、多表 JOIN、聚合统计、条件筛选
2. **体现 Schema 的特殊规则**：如枚举值必须使用数字、日期函数的使用
3. **示例要简洁**：每个示例控制在 3 行以内，避免 Prompt 过长

#### 7.3 温度参数调优

在 `application.yml` 中设置 `temperature`：

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          temperature: 0.1   # 低温度，输出更确定、更严谨
```

- `temperature = 0`：最确定，每次输出几乎相同（适合生产环境）
- `temperature = 0.1-0.3`：较低随机性，适合 NL2SQL（推荐）
- `temperature = 0.7+`：高随机性，适合创意写作（不适合 NL2SQL）

---

## 动手练习

### 练习 1：完善 Schema 描述

当前 `Nl2SqlService` 中的 `SCHEMA_CONTEXT` 只包含 `emp` 和 `dept` 两张表。请根据 `tlias-pro_init.sql` 中的定义，补充 `operate_log`（操作日志表）和 `ai_document`（AI 知识库文档表）的 Schema 描述，并测试以下查询：

- "查询最近10条操作日志"
- "知识库里有哪些文档"

**提示**：注意字段类型的准确描述，特别是日期类型和枚举类型。

### 练习 2：增强安全校验

当前的安全校验只检查了顶层语句是否为 `SELECT`。请增强 `validateByJSqlParser` 方法，增加对以下情况的检测：

- SQL 中包含子查询，且子查询中包含 `DELETE` 或 `DROP`
- SQL 中包含 `UNION`，且第二个 `SELECT` 后面跟了危险操作

**提示**：可以使用 JSqlParser 的 `SelectVisitor` 遍历语法树，检查所有子查询。

### 练习 3：实现查询历史记录

使用 Redis 缓存用户最近的 10 条查询记录（自然语言问题 + 生成的 SQL + 查询时间），并在前端页面展示"查询历史"列表，支持点击历史记录快速重新查询。

**提示**：使用 Redis 的 `List` 数据结构，`LPUSH` 添加记录，`LRANGE` 获取最近 10 条，`LTRIM` 保持列表长度。

---

## 常见错误排查

| 阶段 | 错误现象 | 根因分析 | 解决方案 |
|------|---------|---------|---------|
| **Prompt 构建** | LLM 生成的 SQL 字段名错误 | Schema 描述不完整，LLM "猜"了字段名 | 检查 Schema 中是否包含所有表和字段；确保字段名拼写正确 |
| **Prompt 构建** | LLM 用中文匹配枚举值 | 枚举值映射未在 Schema 中说明 | 在 Schema 中明确标注 `gender: 1=男，2=女` 等映射关系 |
| **LLM 调用** | API 返回 429 Too Many Requests | 请求频率超过 Kimi API 限制 | 添加重试机制（指数退避）；降低调用频率；考虑使用缓存 |
| **LLM 调用** | API 返回 401 Unauthorized | API Key 无效或过期 | 检查 `KIMI_API_KEY` 环境变量是否正确设置；确认 Key 未过期 |
| **LLM 调用** | LLM 返回 markdown 代码块 | LLM 输出格式不稳定 | 在 Prompt 中明确要求"只输出纯 SQL"；后端用正则去除 ```sql 标记 |
| **安全校验** | 合法的 SELECT 被误拦截 | 正则表达式过于宽泛 | 检查正则是否匹配了字段名中的关键字（如 `status` 包含 `at`） |
| **安全校验** | JSqlParser 解析失败 | LLM 生成了语法错误的 SQL | 添加异常捕获，返回友好提示；在 Prompt 中要求"标准 MySQL 语法" |
| **SQL 执行** | 查询结果为空 | SQL 条件过于严格或数据不存在 | 检查生成的 SQL 条件；确认数据库中有匹配的数据 |
| **SQL 执行** | 查询超时 | SQL 没有 LIMIT，返回数据量过大 | 确保 `addLimitIfMissing` 方法正常工作；检查 JdbcTemplate 超时配置 |
| **前端展示** | 表格列顺序混乱 | 使用了 HashMap 而不是 LinkedHashMap | 将 `HashMap` 改为 `LinkedHashMap`，保持插入顺序 |
| **前端展示** | 日期显示为时间戳 | JSON 序列化时日期格式问题 | 在 `application.yml` 中配置 Jackson 日期格式：`spring.jackson.date-format: yyyy-MM-dd HH:mm:ss` |

---

## 本节小结

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NL2SQL 智能查询 知识图谱                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                              ┌─────────────┐                               │
│                              │   NL2SQL    │                               │
│                              │  智能查询    │                               │
│                              └──────┬──────┘                               │
│                                     │                                       │
│            ┌────────────────────────┼────────────────────────┐             │
│            │                        │                        │             │
│            ↓                        ↓                        ↓             │
│   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐       │
│   │   Prompt 工程    │    │   安全校验体系   │    │   工程化实现     │       │
│   └────────┬────────┘    └────────┬────────┘    └────────┬────────┘       │
│            │                      │                      │                 │
│   ┌────────┴────────┐    ┌────────┴────────┐    ┌────────┴────────┐       │
│   │ - Schema 注入    │    │ - Prompt 声明    │    │ - ChatClient    │       │
│   │ - 枚举值映射     │    │ - 正则过滤       │    │ - Controller    │       │
│   │ - Few-shot 示例  │    │ - JSqlParser    │    │ - Vue3 前端     │       │
│   │ - 温度参数调优   │    │ - LIMIT 限制    │    │ - 异常处理      │       │
│   └─────────────────┘    └─────────────────┘    └─────────────────┘       │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────┐     │
│   │                        核心结论                                  │     │
│   │  NL2SQL 的核心挑战不是"生成 SQL"，而是"安全地生成正确的 SQL"        │     │
│   │  Prompt 工程决定准确率，安全过滤决定可靠性                         │     │
│   └─────────────────────────────────────────────────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**核心要点回顾**：

1. **NL2SQL 解决了非技术人员查数据的技术门槛问题**，让老板、HR、运营等角色能直接用自然语言查询数据库
2. **Prompt 工程是准确率的关键**，Schema 描述越详细（字段注释、枚举映射、外键关系），生成的 SQL 越准确
3. **安全是 NL2SQL 的生命线**，必须构建多层防护体系（Prompt 声明 → 正则过滤 → JSqlParser → LIMIT 限制），任何一层都不能省略
4. **Spring AI ChatClient 简化了 LLM 调用**，链式 API `.prompt().system().user().call().content()` 清晰易用
5. **Few-shot 示例能显著提升复杂查询的准确率**，建议在 Prompt 中嵌入 2-3 个典型问答对

---

## 参考文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Kimi API 文档](https://platform.moonshot.cn/docs)
- [JSqlParser GitHub](https://github.com/JSQLParser/JSqlParser)
- [OpenAI API 兼容说明](https://platform.moonshot.cn/docs/api/chat)
- [MySQL DATE_SUB 函数](https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html#function_date-sub)
- [Spring JdbcTemplate 文档](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html)
- [Element Plus 官方文档](https://element-plus.org/)
