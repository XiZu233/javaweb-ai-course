package com.tliaspro.module.ai.controller;

import com.tliaspro.module.ai.service.MockAiService;
import com.tliaspro.module.ai.service.Nl2SqlService;
import com.tliaspro.module.ai.service.RagService;
import com.tliaspro.module.ai.service.ResumeParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private Nl2SqlService nl2SqlService;

    @Autowired
    private RagService ragService;

    @Autowired
    private ResumeParseService resumeParseService;

    @Autowired
    private MockAiService mockAiService;

    // ========== NL2SQL ==========
    @PostMapping("/nl2sql")
    public Map<String, Object> nl2sql(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        try {
            String sql = nl2SqlService.generateSql(question);
            Map<String, Object> result = new HashMap<>();
            result.put("sql", sql);
            result.put("mock", false);
            return result;
        } catch (Exception e) {
            log.error("NL2SQL调用失败，使用Mock：{}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("sql", mockAiService.mockNl2Sql(question));
            result.put("mock", true);
            return result;
        }
    }

    // ========== RAG ==========
    @PostMapping("/rag")
    public Map<String, Object> rag(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        try {
            String answer = ragService.query(question);
            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("mock", false);
            return result;
        } catch (Exception e) {
            log.error("RAG调用失败，使用Mock：{}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("answer", mockAiService.mockRag(question));
            result.put("mock", true);
            return result;
        }
    }

    @PostMapping("/rag/ingest")
    public Map<String, Object> ingestDocument(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String content = body.get("content");
        ragService.ingestDocument(title, content);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "文档摄入成功");
        return result;
    }

    // ========== 简历解析 ==========
    @PostMapping("/resume/parse")
    public Map<String, Object> parseResume(@RequestParam("file") MultipartFile file) {
        try {
            ResumeParseService.ResumeParseResult result = resumeParseService.parse(file);
            Map<String, Object> response = new HashMap<>();
            response.put("data", result);
            response.put("mock", false);
            return response;
        } catch (Exception e) {
            log.error("简历解析失败，使用Mock：{}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("data", mockAiService.mockResumeParse());
            response.put("mock", true);
            return response;
        }
    }

    // ========== 流式对话 ==========
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        // 简化版：直接返回Mock流
        return Flux.just(
                "收到你的问题：",
                message,
                "\n",
                "（AI流式响应演示）",
                "\n",
                "当前为Mock模式，请配置真实API Key后使用完整功能。"
        );
    }
}
