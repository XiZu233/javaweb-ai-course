package com.tliaspro.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {

    @Autowired
    private ChatClient chatClient;

    // 简化版：使用内存存储文档片段（教学演示用）
    // 生产环境应使用向量数据库如Milvus/PGVector
    private final List<DocumentChunk> documentStore = new ArrayList<>();

    public void ingestDocument(String title, String content) {
        log.info("摄入文档：{}", title);
        // 简单按段落分块
        String[] paragraphs = content.split("\n\n");
        for (int i = 0; i < paragraphs.length; i++) {
            String chunk = paragraphs[i].trim();
            if (chunk.length() > 20) {
                documentStore.add(new DocumentChunk(title, i, chunk));
            }
        }
        log.info("文档分块完成，共{}块", documentStore.size());
    }

    public String query(String question) {
        log.info("RAG查询：{}", question);

        // 1. 检索相关片段（简化版：关键词匹配）
        List<DocumentChunk> relevantChunks = retrieveRelevant(question);

        if (relevantChunks.isEmpty()) {
            return "根据现有制度文件无法回答该问题，建议咨询HR。";
        }

        // 2. 构建上下文
        String context = relevantChunks.stream()
                .map(c -> "[" + c.source + "第" + (c.index + 1) + "段] " + c.content)
                .collect(Collectors.joining("\n---\n"));

        // 3. 增强生成
        return chatClient.prompt()
                .system("""
                        你是企业制度知识库助手。请基于提供的参考文档片段回答问题。
                        如果片段中没有相关信息，请明确说明"根据现有资料无法回答"。
                        不要编造信息，不要引用片段中不存在的内容。
                        """)
                .user("参考文档片段：\n" + context + "\n\n用户问题：" + question)
                .call()
                .content();
    }

    private List<DocumentChunk> retrieveRelevant(String question) {
        // 简化版检索：基于关键词匹配度排序
        String[] keywords = question.toLowerCase().split("\\s+");
        return documentStore.stream()
                .sorted((a, b) -> {
                    int scoreA = score(a, keywords);
                    int scoreB = score(b, keywords);
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(3)
                .collect(Collectors.toList());
    }

    private int score(DocumentChunk chunk, String[] keywords) {
        String content = chunk.content.toLowerCase();
        int score = 0;
        for (String kw : keywords) {
            if (content.contains(kw)) score++;
        }
        return score;
    }

    private record DocumentChunk(String source, int index, String content) {}
}
