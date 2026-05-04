package com.tliaspro.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
public class ResumeParseService {

    @Autowired
    private ChatClient chatClient;

    public ResumeParseResult parse(MultipartFile file) throws IOException {
        log.info("解析简历：{}", file.getOriginalFilename());

        // 1. PDF文本提取
        String text = extractText(file.getInputStream());
        log.debug("提取文本长度：{}", text.length());

        // 2. 使用Spring AI结构化输出
        BeanOutputConverter<ResumeParseResult> converter =
                new BeanOutputConverter<>(ResumeParseResult.class);

        String jsonSchema = converter.getFormat();

        String response = chatClient.prompt()
                .system("""
                        你是一位专业的HR简历解析专家。请从简历文本中提取结构化信息。
                        严格按JSON格式输出，不要包含任何解释性文字。
                        如果某项信息无法提取，使用null或空数组。
                        """)
                .user("请解析以下简历内容：\n\n" + text + "\n\n请按以下JSON Schema输出：\n" + jsonSchema)
                .call()
                .content();

        return converter.convert(response);
    }

    private String extractText(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document).trim();
        }
    }

    public record ResumeParseResult(
            String name,
            String phone,
            String email,
            List<Education> education,
            List<WorkExperience> workExperience,
            List<String> skills
    ) {}

    public record Education(
            String school,
            String major,
            String degree,
            String duration
    ) {}

    public record WorkExperience(
            String company,
            String position,
            String duration,
            String description
    ) {}
}
