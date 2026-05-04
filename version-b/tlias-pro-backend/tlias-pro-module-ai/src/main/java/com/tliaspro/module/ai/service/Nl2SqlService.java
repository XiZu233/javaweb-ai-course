package com.tliaspro.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Nl2SqlService {

    @Autowired
    private ChatClient chatClient;

    private static final String SCHEMA_CONTEXT = """
            数据库表结构：
            - emp (员工表): id, username, password, name, gender(1男2女), image, job(1班主任2讲师3学工主管4教研主管5咨询师6其他), entrydate, dept_id, create_time, update_time
            - dept (部门表): id, name, create_time, update_time
            - emp_expr (工作经历表): id, emp_id, begin, end, company, job
            """;

    public String generateSql(String question) {
        log.info("NL2SQL查询：{}", question);

        String response = chatClient.prompt()
                .system("""
                        你是一位专业的MySQL数据库专家。根据提供的数据库Schema，将用户问题转换为SQL查询。
                        规则：
                        1. 仅生成SELECT查询语句
                        2. 禁止使用UPDATE/DELETE/INSERT/DROP/ALTER/CREATE/GRANT/TRUNCATE
                        3. 如果问题涉及修改数据，返回"INVALID_QUERY"
                        4. 表名和字段名必须严格使用Schema中定义的名称
                        """)
                .user(SCHEMA_CONTEXT + "\n\n用户问题：" + question)
                .call()
                .content();

        String sql = response.trim();
        log.info("AI生成SQL：{}", sql);

        // 安全校验
        validateSafe(sql);

        return sql;
    }

    private void validateSafe(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new SecurityException("仅允许SELECT查询，禁止DML/DDL操作");
            }
        } catch (Exception e) {
            throw new SecurityException("SQL解析失败或包含非法语句：" + e.getMessage());
        }
    }
}
