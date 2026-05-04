package com.tliaspro.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI服务Mock兜底
 * 当真实AI API调用失败时，返回预设的合理假数据，确保演示不翻车
 */
@Slf4j
@Service
public class MockAiService {

    public String mockNl2Sql(String question) {
        log.warn("使用Mock NL2SQL响应：{}", question);
        return "SELECT * FROM emp WHERE gender = 1 LIMIT 10";
    }

    public String mockRag(String question) {
        log.warn("使用Mock RAG响应：{}", question);
        return "根据公司《员工手册》第3章规定：员工年假天数根据工龄计算，入职满1年享受5天年假，满10年享受10天。请假需提前在OA系统提交申请。";
    }

    public ResumeParseService.ResumeParseResult mockResumeParse() {
        log.warn("使用Mock简历解析响应");
        return new ResumeParseService.ResumeParseResult(
                "张三",
                "13800138000",
                "zhangsan@example.com",
                List.of(new ResumeParseService.Education(
                        "北京大学", "计算机科学", "本科", "2018-2022"
                )),
                List.of(new ResumeParseService.WorkExperience(
                        "ABC科技", "Java开发工程师", "2022-至今", "负责后端系统开发"
                )),
                List.of("Java", "SpringBoot", "MySQL", "Redis")
        );
    }
}
