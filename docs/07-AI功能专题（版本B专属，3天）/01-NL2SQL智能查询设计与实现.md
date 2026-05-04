# NL2SQL 智能查询设计与实现

## 学习目标

- 理解自然语言转 SQL 的核心思路
- 掌握 Prompt 工程在 NL2SQL 中的应用
- 能够使用 JSqlParser 实现 SQL 安全过滤
- 实现一个可演示的 NL2SQL 功能模块

## 核心知识点

### 1. NL2SQL 是什么

NL2SQL（Natural Language to SQL）是指将人类自然语言问题自动转换为数据库 SQL 查询语句的技术。

**示例**：
```
用户输入：查询入职3个月以上的本科前端工程师
AI 输出：SELECT * FROM emp WHERE job = 2 AND entrydate <= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
```

### 2. 核心实现流程

```
用户提问
    ↓
Prompt 工程（注入表结构 Schema）
    ↓
调用 LLM 生成 SQL
    ↓
SQL 安全校验（仅允许 SELECT）
    ↓
执行查询并返回结果
```

### 3. Prompt 工程模板

```java
String systemPrompt = """
    你是一位专业的MySQL数据库专家。根据提供的数据库Schema，将用户问题转换为SQL查询。
    规则：
    1. 仅生成SELECT查询语句
    2. 禁止使用UPDATE/DELETE/INSERT/DROP/ALTER/CREATE/GRANT/TRUNCATE
    3. 如果问题涉及修改数据，返回"INVALID_QUERY"
    4. 表名和字段名必须严格使用Schema中定义的名称
    """;

String userPrompt = SCHEMA_CONTEXT + "\n\n用户问题：" + question;
```

### 4. SQL 安全过滤

```java
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

private void validateSafe(String sql) {
    Statement statement = CCJSqlParserUtil.parse(sql);
    if (!(statement instanceof Select)) {
        throw new SecurityException("仅允许SELECT查询，禁止DML/DDL操作");
    }
}
```

### 5. 前端交互设计

- 自然语言输入框（带示例提示）
- "生成SQL" 按钮
- SQL 预览区域
- 查询结果表格

## 动手练习

1. 在 `tlias-pro-module-ai` 中完成 `Nl2SqlService` 的实现
2. 配置 Kimi API Key（在 `application.yml` 中）
3. 测试以下查询：
   - "查询所有讲师"
   - "统计每个部门的人数"
   - "删除所有员工"（应被拦截）

## 常见错误排查

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| API 429 | 请求频率过高 | 添加重试机制，降低调用频率 |
| SQL 解析失败 | LLM 返回非标准 SQL | 添加异常捕获，返回友好提示 |
| 字段名错误 | Schema 注入不完整 | 确保所有表和字段都写入 Prompt |

## 本节小结

NL2SQL 的核心挑战不是"生成 SQL"，而是"安全地生成正确的 SQL"。Prompt 工程决定准确率，安全过滤决定可靠性。
