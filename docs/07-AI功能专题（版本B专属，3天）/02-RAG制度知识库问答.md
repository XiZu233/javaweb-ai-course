# RAG 制度知识库问答

## 学习目标

- 理解 RAG（检索增强生成）的核心原理，以及为什么它比单纯调用大模型更可靠
- 掌握文档摄入的完整链路：PDF/Word 文本提取、分块策略、Redis 存储
- 掌握查询阶段的检索逻辑：关键词提取、Redis 模糊搜索、Top-K 筛选
- 掌握生成阶段的 Prompt 工程：系统角色设定、参考片段注入、约束条件设计
- 能够独立实现一个基于 Spring AI + Redis 的企业制度知识库问答系统
- 了解 RAG 系统的评估指标，知道如何判断系统回答得好不好

---

## 核心知识点

### 1. 为什么需要 RAG

#### 1.1 大模型的三大痛点

想象你请了一位"博学家"来回答公司制度问题，但他存在三个致命缺陷：

| 痛点 | 具体表现 | 对企业知识库的影响 |
|------|----------|-------------------|
| **知识截止日期** | 大模型的训练数据有截止时间（如 2024 年初），之后的新政策、新制度它不知道 | 2024 年新发布的差旅报销制度，模型完全不了解 |
| **幻觉问题** | 模型会"一本正经地胡说八道"，编造看似合理但实际不存在的内容 | 问"产假有多少天"，模型可能编造一个错误的数字 |
| **缺乏私有知识** | 模型训练用的是公开互联网数据，企业的内部制度、流程、规范从未见过 | 公司的考勤打卡规则、审批流程，模型一无所知 |

#### 1.2 RAG 的解决思路

RAG 的核心思想很简单：**给大模型配上一本"实时更新的参考书"**。

```
没有 RAG 的对话：
  用户：我们公司年假怎么请？
  大模型：（瞎猜）一般来说年假需要提前一周申请...
  （错误！公司实际规定是提前 3 个工作日）

有 RAG 的对话：
  用户：我们公司年假怎么请？
  系统：（先从企业制度文档中检索相关片段）
  大模型：（基于检索到的真实片段回答）
    根据《员工手册》第三章规定：
    1. 年假需提前 3 个工作日提交申请
    2. 审批流程：部门经理 → 人事部
    3. 未休年假可累计至次年 3 月 31 日
```

#### 1.3 RAG vs Fine-tuning（微调）对比

很多初学者会困惑：既然要让大模型了解企业知识，为什么不直接微调模型？

| 对比维度 | RAG（检索增强生成） | Fine-tuning（微调） |
|----------|---------------------|---------------------|
| **原理** | 不改变模型，只给模型提供相关上下文 | 修改模型参数，让模型"记住"新知识 |
| **知识更新** | 随时更新文档库即可，实时生效 | 需要重新训练，耗时耗力 |
| **成本** | 低，只需存储文档和调用 API | 高，需要 GPU 训练资源 |
| ** hallucination** | 低，基于真实文档回答 | 仍有一定概率 hallucination |
| **适用场景** | 知识频繁变更、文档数量大 | 需要改变模型行为风格、固定知识 |
| **数据量要求** | 不需要训练数据 | 需要大量高质量标注数据 |

**结论**：对于企业知识库问答这种"知识频繁更新、文档数量大"的场景，**RAG 是首选方案**。

---

### 2. RAG 三阶段流程详解

```
+-------------------------------------------------------------+
|                     RAG 完整流程图                           |
+-------------------------------------------------------------+
|                                                             |
|  【第一阶段：文档摄入】                                        |
|                                                             |
|   PDF/Word 文档                                               |
|        |                                                     |
|        v                                                     |
|   +----------------+     +----------------+                 |
|   |  文本提取       | --> |  文本分块       |                 |
|   | (PDFBox/POI)   |     | (固定长度/段落) |                 |
|   +----------------+     +----------------+                 |
|        |                                                     |
|        v                                                     |
|   +----------------+                                         |
|   |  存入 Redis     |                                         |
|   | (Hash 结构)    |                                         |
|   +----------------+                                         |
|                                                             |
|  【第二阶段：查询检索】                                        |
|                                                             |
|   用户提问："年假怎么请？"                                     |
|        |                                                     |
|        v                                                     |
|   +----------------+     +----------------+                 |
|   | LLM 提取关键词  | --> | Redis 模糊搜索  |                 |
|   | (年假,请假,申请) |     | (匹配相关片段)  |                 |
|   +----------------+     +----------------+                 |
|        |                                                     |
|        v                                                     |
|   +----------------+                                         |
|   |  Top-K 筛选     |                                         |
|   | (取最相关的 K 个)|                                         |
|   +----------------+                                         |
|                                                             |
|  【第三阶段：生成回答】                                        |
|                                                             |
|   检索片段 + 用户问题                                          |
|        |                                                     |
|        v                                                     |
|   +----------------+     +----------------+                 |
|   |  Prompt 增强    | --> |  LLM 生成回答   |                 |
|   | (系统角色+约束) |     | (基于真实文档)  |                 |
|   +----------------+     +----------------+                 |
|        |                                                     |
|        v                                                     |
|   带引用来源的完整回答                                         |
|                                                             |
+-------------------------------------------------------------+
```

---

### 3. 第一阶段：文档摄入详解

#### 3.1 PDF 文本提取（Apache PDFBox）

PDF 是最常见的企业文档格式。我们使用 Apache PDFBox 库来提取文本。

```java
// 引入依赖（pom.xml）
// <dependency>
//     <groupId>org.apache.pdfbox</groupId>
//     <artifactId>pdfbox</artifactId>
//     <version>3.0.1</version>
// </dependency>

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;

/**
 * PDF 文本提取工具类
 * 作用：将 PDF 文件中的文字内容提取出来，供后续分块使用
 */
public class PdfExtractor {

    /**
     * 从 PDF 文件中提取全部文本
     * @param filePath PDF 文件的路径，如 "docs/员工手册.pdf"
     * @return 提取出的纯文本字符串
     */
    public static String extractText(String filePath) {
        // 使用 try-with-resources 自动关闭文档，防止内存泄漏
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            // PDFTextStripper 是 PDFBox 提供的文本提取器
            PDFTextStripper stripper = new PDFTextStripper();
            // 设置是否按页排序（true 表示按页码顺序提取）
            stripper.setSortByPosition(true);
            // 执行提取，返回整个文档的文本
            return stripper.getText(document);
        } catch (IOException e) {
            // 提取失败时抛出运行时异常，由上层处理
            throw new RuntimeException("PDF 文本提取失败: " + filePath, e);
        }
    }
}
```

#### 3.2 Word 文本提取（Apache POI）

Word 文档（.docx）是另一种常见格式，使用 Apache POI 处理。

```java
// 引入依赖（pom.xml）
// <dependency>
//     <groupId>org.apache.poi</groupId>
//     <artifactId>poi-ooxml</artifactId>
//     <version>5.2.5</version>
// </dependency>

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Word 文本提取工具类
 * 作用：将 .docx 文件中的文字内容提取出来
 */
public class WordExtractor {

    /**
     * 从 Word 文件中提取全部文本
     * @param filePath Word 文件的路径，如 "docs/考勤制度.docx"
     * @return 提取出的纯文本字符串，段落之间用换行分隔
     */
    public static String extractText(String filePath) {
        // 使用 try-with-resources 自动关闭文件流和文档
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            // 获取文档中的所有段落
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder sb = new StringBuilder();
            // 逐段提取文本，用换行符连接
            for (XWPFParagraph para : paragraphs) {
                String text = para.getText().trim();
                // 过滤空段落
                if (!text.isEmpty()) {
                    sb.append(text).append("\n");
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Word 文本提取失败: " + filePath, e);
        }
    }
}
```

#### 3.3 文本分块策略

提取出的文档可能有几万字，不能直接传给大模型（有长度限制）。需要切成小段。

**为什么需要分块？**
- 大模型有上下文长度限制（如 8K、32K tokens）
- 过长的文本会稀释注意力，降低检索精度
- 细粒度分块可以提高检索的准确性

**三种分块策略对比：**

| 策略 | 原理 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|----------|
| **固定长度分块** | 每 N 个字符切一块 | 简单、均匀 | 可能切断句子 | 对语义连续性要求不高的文档 |
| **按段落分块** | 以换行分隔的段落为一块 | 保留完整语义 | 段落长短不一 | 结构清晰的制度文档（推荐） |
| **重叠窗口分块** | 固定长度 + 相邻块重叠 M 个字符 | 避免上下文丢失 | 存储冗余增加 | 需要保留跨块上下文的场景 |

**本项目推荐：按段落分块（简化版实现）**

```java
import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块工具类
 * 作用：将长文本切分成适合检索和生成的小块
 */
public class TextChunker {

    // 最小块长度：少于这个长度的段落会被过滤（避免无意义的短片段）
    private static final int MIN_CHUNK_LENGTH = 20;
    // 最大块长度：超过这个长度的段落会被强制截断（防止单块过大）
    private static final int MAX_CHUNK_LENGTH = 1000;

    /**
     * 按段落分块
     * @param content 原始文本内容
     * @param source  文档来源标识，如 "员工手册.pdf"
     * @return 分块后的列表，每个元素是一个 Chunk 对象
     */
    public static List<Chunk> splitByParagraph(String content, String source) {
        List<Chunk> chunks = new ArrayList<>();
        // 按两个换行符分割（即空行分隔的段落）
        String[] paragraphs = content.split("\\n\\n");
        int index = 0;
        for (String para : paragraphs) {
            // 去除首尾空白字符
            String trimmed = para.trim();
            // 过滤过短的段落（无意义内容）
            if (trimmed.length() < MIN_CHUNK_LENGTH) {
                continue;
            }
            // 如果段落过长，截断到最大长度
            if (trimmed.length() > MAX_CHUNK_LENGTH) {
                trimmed = trimmed.substring(0, MAX_CHUNK_LENGTH);
            }
            // 创建 Chunk 对象
            Chunk chunk = new Chunk();
            chunk.setId(source + "_" + index);  // 唯一标识：来源_序号
            chunk.setContent(trimmed);           // 块内容
            chunk.setSource(source);             // 来源文档
            chunk.setIndex(index);               // 在原文中的序号
            chunks.add(chunk);
            index++;
        }
        return chunks;
    }
}

/**
 * 文本块实体类
 * 对应 Redis 中存储的一条记录
 */
public class Chunk {
    private String id;      // 唯一标识，如 "员工手册.pdf_0"
    private String content; // 文本内容
    private String source;  // 来源文档名
    private int index;      // 在原文中的序号
    // ... getter 和 setter 省略
}
```

**分块大小选择的权衡：**

```
分块太小（如 128 tokens）          分块太大（如 2048 tokens）
        |                                |
        v                                v
  +-------------+                  +-------------+
  | 检索精度高   |                  | 检索精度低   |
  | 每块语义单一 |                  | 包含过多无关信息|
  | 但上下文丢失 |                  | 但上下文完整 |
  +-------------+                  +-------------+

本项目推荐：512-1024 tokens/块（约 300-700 个汉字）
这是一个兼顾检索精度和上下文完整性的平衡点。
```

#### 3.4 存入 Redis（Hash 结构）

本项目简化版 RAG 使用 Redis 存储文本块，而非专门的向量数据库。

```
Redis 存储结构（Hash）：

  Key: rag:chunk:员工手册.pdf_0
  Value:
    ├─ content: "年假天数根据工龄计算：工作满1年不满10年，年假5天..."
    ├─ source: "员工手册.pdf"
    ├─ index: "0"
    └─ keywords: "年假,工龄,休假"  （可选，用于加速检索）

  Key: rag:chunk:员工手册.pdf_1
  Value:
    ├─ content: "请假需提前3个工作日提交申请，经部门经理审批..."
    ├─ source: "员工手册.pdf"
    ├─ index: "1"
    └─ keywords: "请假,申请,审批"
```

```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 文档存储服务
 * 作用：将分块后的文档存入 Redis，供后续检索使用
 */
@Component
public class RagStorageService {

    // Redis Key 前缀，用于区分 RAG 相关的数据
    private static final String CHUNK_KEY_PREFIX = "rag:chunk:";

    // 注入 Spring 提供的 Redis 操作模板
    private final RedisTemplate<String, String> redisTemplate;

    public RagStorageService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将文本块列表存入 Redis
     * @param chunks 分块后的文本块列表
     */
    public void storeChunks(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            // 构建 Redis Key：前缀 + 块ID
            String key = CHUNK_KEY_PREFIX + chunk.getId();
            // 使用 Hash 结构存储，方便后续按字段检索
            Map<String, String> hash = new HashMap<>();
            hash.put("content", chunk.getContent());   // 文本内容
            hash.put("source", chunk.getSource());     // 来源文档
            hash.put("index", String.valueOf(chunk.getIndex())); // 序号
            // 将 Hash 写入 Redis
            redisTemplate.opsForHash().putAll(key, hash);
        }
    }

    /**
     * 清空所有 RAG 文档数据
     * 用于重新摄入文档前的清理
     */
    public void clearAllChunks() {
        // 扫描所有以 rag:chunk: 开头的 Key
        var keys = redisTemplate.keys(CHUNK_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            // 批量删除
            redisTemplate.delete(keys);
        }
    }
}
```

**注意**：生产环境推荐使用专门的向量数据库（如 Milvus、Pinecone、Qdrant），它们支持基于语义相似度的向量检索，效果远好于关键词匹配。本项目的 Redis + 关键词匹配方案仅用于教学简化。

---

### 4. 第二阶段：查询检索详解

#### 4.1 关键词提取（让 LLM 帮忙）

用户的问题往往是自然语言，如"年假怎么请？"，我们需要从中提取关键词用于检索。

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 关键词提取服务
 * 作用：将用户的自然语言问题转化为适合检索的关键词
 */
@Component
public class KeywordExtractor {

    // Spring AI 提供的聊天客户端，用于调用大模型
    private final ChatClient chatClient;

    public KeywordExtractor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 从用户问题中提取检索关键词
     * @param question 用户的原始问题，如 "年假怎么请？"
     * @return 关键词列表，如 "年假,请假,申请"
     */
    public String extractKeywords(String question) {
        // 构建 Prompt，让 LLM 帮我们提取关键词
        String prompt = """
            请从以下问题中提取 3-5 个最核心的检索关键词，用于在文档库中搜索相关内容。
            要求：
            1. 只输出关键词，用逗号分隔
            2. 不要输出任何解释性文字
            3. 优先提取名词和动词

            问题：%s
            """.formatted(question);

        // 调用大模型，获取关键词
        String response = chatClient.prompt()
            .user(prompt)   // 设置用户输入
            .call()         // 发起调用
            .content();     // 获取返回的文本内容

        // 清理返回结果，去除多余空白
        return response.trim();
    }
}
```

**关键词提取示例：**

| 用户问题 | 提取的关键词 |
|----------|-------------|
| "年假怎么请？" | 年假,请假,申请,审批 |
| "报销差旅费需要什么材料？" | 报销,差旅费,材料,发票 |
| "试用期多久，工资怎么算？" | 试用期,工资,计算,期限 |

#### 4.2 Redis 模糊搜索

使用 Redis 的 `HSCAN` 或遍历所有 Chunk 进行关键词匹配。

```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * RAG 检索服务
 * 作用：根据关键词从 Redis 中检索相关文本块
 */
@Component
public class RagRetrievalService {

    private static final String CHUNK_KEY_PREFIX = "rag:chunk:";
    private final RedisTemplate<String, String> redisTemplate;

    public RagRetrievalService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据关键词检索相关文本块
     * @param keywords 关键词字符串，逗号分隔，如 "年假,请假,申请"
     * @param topK     返回最相关的 K 个片段
     * @return 按相关度排序的文本块列表
     */
    public List<Chunk> search(String keywords, int topK) {
        // 将关键词字符串拆分为数组
        String[] keywordArray = keywords.split(",");
        // 用于存储每个块的匹配得分
        Map<String, Integer> scoreMap = new HashMap<>();
        // 用于存储块的完整内容
        Map<String, Chunk> chunkMap = new HashMap<>();

        // 获取所有 RAG chunk 的 Key
        Set<String> keys = redisTemplate.keys(CHUNK_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList(); // 没有数据，返回空列表
        }

        // 遍历所有 chunk，计算匹配得分
        for (String key : keys) {
            // 从 Redis Hash 中获取 content 字段
            String content = (String) redisTemplate.opsForHash().get(key, "content");
            String source = (String) redisTemplate.opsForHash().get(key, "source");
            String indexStr = (String) redisTemplate.opsForHash().get(key, "index");

            if (content == null) continue;

            // 计算得分：每个匹配的关键词 +1 分
            int score = 0;
            for (String kw : keywordArray) {
                String trimmedKw = kw.trim().toLowerCase();
                if (!trimmedKw.isEmpty() && content.toLowerCase().contains(trimmedKw)) {
                    score++;
                }
            }

            // 如果有匹配，记录得分和块信息
            if (score > 0) {
                scoreMap.put(key, score);
                Chunk chunk = new Chunk();
                chunk.setId(key.replace(CHUNK_KEY_PREFIX, ""));
                chunk.setContent(content);
                chunk.setSource(source);
                chunk.setIndex(Integer.parseInt(indexStr != null ? indexStr : "0"));
                chunkMap.put(key, chunk);
            }
        }

        // 按得分降序排序，取 Top-K
        return scoreMap.entrySet().stream()
            // 按得分从高到低排序
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            // 限制返回数量
            .limit(topK)
            // 从 chunkMap 中获取对应的 Chunk 对象
            .map(entry -> chunkMap.get(entry.getKey()))
            // 收集为列表
            .toList();
    }
}
```

#### 4.3 Top-K 筛选与相关度评分

```
检索结果排序示例：

关键词："年假,请假,申请"

+--------+------------------------------------------+-------+
| 排名   | 内容片段                                  | 得分  |
+--------+------------------------------------------+-------+
| 1      | 年假需提前3个工作日申请，填写请假单...      | 3     |  <-- 匹配全部3个关键词
| 2      | 员工请假流程：提交申请 → 部门审批...         | 2     |  <-- 匹配"请假""申请"
| 3      | 年假天数根据工龄计算，满1年5天...            | 1     |  <-- 只匹配"年假"
+--------+------------------------------------------+-------+

取 Top-K（如 K=3），将前 3 个片段传入 Prompt。
```

---

### 5. 第三阶段：生成回答详解

#### 5.1 Prompt 模板设计

Prompt 的设计直接决定回答质量。一个好的 RAG Prompt 包含四个部分：

```
+-------------------------------------------------------------+
|                    RAG Prompt 结构                          |
+-------------------------------------------------------------+
|                                                             |
|  1. 【系统角色】定义 AI 的身份和行为准则                       |
|     "你是企业制度知识库助手，专门回答公司制度相关问题..."       |
|                                                             |
|  2. 【参考片段】注入检索到的相关文档内容                        |
|     "参考文档片段：\n[片段1] ...\n[片段2] ..."                 |
|                                                             |
|  3. 【用户问题】原始提问                                     |
|     "用户问题：年假怎么请？"                                   |
|                                                             |
|  4. 【约束条件】限制回答的行为                                |
|     "如果片段中没有相关信息，请明确说明..."                     |
|                                                             |
+-------------------------------------------------------------+
```

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * RAG 回答生成服务
 * 作用：将检索到的片段和用户问题组合，调用大模型生成回答
 */
@Service
public class RagGenerationService {

    private final ChatClient chatClient;

    public RagGenerationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 生成 RAG 回答
     * @param chunks   检索到的相关文本块
     * @param question 用户的原始问题
     * @return 生成的回答文本
     */
    public String generateAnswer(List<Chunk> chunks, String question) {
        // 构建参考片段部分
        StringBuilder references = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            references.append("[片段").append(i + 1).append("] ")
                     .append("来源：").append(chunk.getSource()).append("\n")
                     .append(chunk.getContent()).append("\n\n");
        }

        // 构建完整的 Prompt
        String prompt = """
            你是企业制度知识库助手。请基于以下提供的参考文档片段回答问题。

            【回答要求】
            1. 严格基于参考文档片段回答，不要编造信息
            2. 如果片段中没有相关信息，请明确说明"根据现有资料无法回答"
            3. 回答要简洁明了，分点列出关键信息
            4. 在回答末尾标注引用的来源文档

            【参考文档片段】
            %s

            【用户问题】
            %s
            """.formatted(references.toString(), question);

        // 调用大模型生成回答
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
}
```

#### 5.2 "不知道就说不知道"的约束设计

这是 RAG 系统最重要的安全机制，防止模型 hallucination。

```java
/**
 * 增强版 Prompt，加入更严格的约束
 */
public String generateAnswerWithStrictConstraints(List<Chunk> chunks, String question) {
    // ... 构建 references 同上 ...

    String prompt = """
        你是企业制度知识库助手。你的唯一信息来源是下面提供的参考文档片段。

        【核心约束 - 必须遵守】
        1. 如果参考片段中没有足够信息回答问题，你必须回答：
           "根据现有资料无法回答该问题，建议咨询人事部门。"
        2. 禁止编造任何数字、日期、流程步骤
        3. 禁止引用片段中不存在的内容
        4. 禁止回答与参考片段无关的问题（如天气、新闻等）

        【回答格式】
        - 先给出直接答案
        - 再分点列出详细说明
        - 最后标注："参考来源：《文档名》"

        【参考文档片段】
        %s

        【用户问题】
        %s
        """.formatted(references.toString(), question);

    return chatClient.prompt().user(prompt).call().content();
}
```

#### 5.3 引用溯源设计

让用户知道回答来自哪份文档，增加可信度。

```java
/**
 * 带引用溯源的回答生成
 * 在回答中标注每个信息点的来源
 */
public String generateAnswerWithCitations(List<Chunk> chunks, String question) {
    // ... 构建 references ...

    String prompt = """
        你是企业制度知识库助手。请基于参考文档片段回答问题。

        【引用要求】
        回答中的每个关键信息点后，用方括号标注来源片段编号，如：
        "年假需提前3个工作日申请[片段1]，审批通过后由人事部备案[片段2]。"

        【参考文档片段】
        %s

        【用户问题】
        %s
        """.formatted(references.toString(), question);

    return chatClient.prompt().user(prompt).call().content();
}
```

---

### 6. Spring AI 完整代码实现

#### 6.1 RagService：核心业务逻辑

```java
package com.itheima.tlias.ai.service;

import com.itheima.tlias.ai.model.Chunk;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * RAG 知识库服务
 * 整合文档摄入、检索、生成三个阶段的核心业务逻辑
 */
@Service
public class RagService {

    // Redis Key 前缀
    private static final String CHUNK_KEY_PREFIX = "rag:chunk:";
    // 默认返回的最相关片段数量
    private static final int DEFAULT_TOP_K = 3;

    // 依赖注入
    private final ChatClient chatClient;
    private final RedisTemplate<String, String> redisTemplate;

    public RagService(ChatClient chatClient, RedisTemplate<String, String> redisTemplate) {
        this.chatClient = chatClient;
        this.redisTemplate = redisTemplate;
    }

    // ==================== 第一阶段：文档摄入 ====================

    /**
     * 摄入文档：上传文件 → 提取文本 → 分块 → 存入 Redis
     * @param file 上传的文件（PDF 或 Word）
     * @return 成功摄入的块数量
     */
    public int ingestDocument(MultipartFile file) {
        try {
            // 1. 将上传的文件保存到临时目录
            Path tempDir = Files.createTempDirectory("rag_upload_");
            File tempFile = new File(tempDir.toFile(), file.getOriginalFilename());
            file.transferTo(tempFile);

            // 2. 根据文件类型选择提取器
            String content;
            String filename = file.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                content = PdfExtractor.extractText(tempFile.getAbsolutePath());
            } else if (filename != null && filename.toLowerCase().endsWith(".docx")) {
                content = WordExtractor.extractText(tempFile.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("不支持的文件格式，仅支持 PDF 和 DOCX");
            }

            // 3. 文本分块
            List<Chunk> chunks = TextChunker.splitByParagraph(content, filename);

            // 4. 存入 Redis
            storeChunks(chunks);

            // 5. 清理临时文件
            Files.deleteIfExists(tempFile.toPath());
            Files.deleteIfExists(tempDir);

            return chunks.size();
        } catch (Exception e) {
            throw new RuntimeException("文档摄入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将文本块存入 Redis Hash
     */
    private void storeChunks(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            String key = CHUNK_KEY_PREFIX + chunk.getId();
            Map<String, String> hash = new HashMap<>();
            hash.put("content", chunk.getContent());
            hash.put("source", chunk.getSource());
            hash.put("index", String.valueOf(chunk.getIndex()));
            redisTemplate.opsForHash().putAll(key, hash);
        }
    }

    // ==================== 第二阶段：查询检索 ====================

    /**
     * 检索相关文本块
     * @param question 用户问题
     * @param topK     返回片段数量
     * @return 相关文本块列表
     */
    public List<Chunk> retrieve(String question, int topK) {
        // 1. 提取关键词
        String keywords = extractKeywords(question);

        // 2. 基于关键词搜索 Redis
        return searchByKeywords(keywords, topK);
    }

    /**
     * 使用 LLM 提取关键词
     */
    private String extractKeywords(String question) {
        String prompt = """
            请从以下问题中提取 3-5 个最核心的检索关键词，用于在文档库中搜索。
            只输出关键词，用逗号分隔，不要解释。
            问题：%s
            """.formatted(question);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content()
            .trim();
    }

    /**
     * 基于关键词在 Redis 中搜索
     */
    private List<Chunk> searchByKeywords(String keywords, int topK) {
        String[] keywordArray = keywords.split(",");
        Map<String, Integer> scoreMap = new HashMap<>();
        Map<String, Chunk> chunkMap = new HashMap<>();

        Set<String> keys = redisTemplate.keys(CHUNK_KEY_PREFIX + "*");
        if (keys == null) return Collections.emptyList();

        for (String key : keys) {
            String content = (String) redisTemplate.opsForHash().get(key, "content");
            String source = (String) redisTemplate.opsForHash().get(key, "source");
            String indexStr = (String) redisTemplate.opsForHash().get(key, "index");

            if (content == null) continue;

            int score = 0;
            for (String kw : keywordArray) {
                if (content.toLowerCase().contains(kw.trim().toLowerCase())) {
                    score++;
                }
            }

            if (score > 0) {
                scoreMap.put(key, score);
                Chunk chunk = new Chunk();
                chunk.setId(key.replace(CHUNK_KEY_PREFIX, ""));
                chunk.setContent(content);
                chunk.setSource(source);
                chunk.setIndex(Integer.parseInt(indexStr != null ? indexStr : "0"));
                chunkMap.put(key, chunk);
            }
        }

        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(topK)
            .map(entry -> chunkMap.get(entry.getKey()))
            .toList();
    }

    // ==================== 第三阶段：生成回答 ====================

    /**
     * 完整的 RAG 问答流程
     * @param question 用户问题
     * @return 生成的回答
     */
    public String chat(String question) {
        // 1. 检索相关片段
        List<Chunk> chunks = retrieve(question, DEFAULT_TOP_K);

        // 2. 如果没有检索到任何片段，直接返回提示
        if (chunks.isEmpty()) {
            return "知识库中暂无相关资料，请尝试上传相关制度文档后再提问。";
        }

        // 3. 构建 Prompt
        String prompt = buildPrompt(chunks, question);

        // 4. 调用大模型生成回答
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    /**
     * 构建 RAG Prompt
     */
    private String buildPrompt(List<Chunk> chunks, String question) {
        StringBuilder refs = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            refs.append("[片段").append(i + 1).append("] ")
                .append("来源：").append(c.getSource()).append("\n")
                .append(c.getContent()).append("\n\n");
        }

        return """
            你是企业制度知识库助手。请基于以下参考文档片段回答问题。

            【回答要求】
            1. 严格基于参考文档片段回答，不要编造信息
            2. 如果片段中没有相关信息，请明确说明"根据现有资料无法回答"
            3. 回答简洁明了，分点列出关键信息
            4. 末尾标注参考来源

            【参考文档片段】
            %s

            【用户问题】
            %s
            """.formatted(refs.toString(), question);
    }
}
```

#### 6.2 RagController：API 接口

```java
package com.itheima.tlias.ai.controller;

import com.itheima.tlias.ai.service.RagService;
import com.itheima.tlias.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG 知识库控制器
 * 提供文档上传和问答的 REST API
 */
@RestController
@RequestMapping("/ai/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 文档摄入接口
     * 用于上传企业制度文档（PDF/DOCX）到知识库
     * @param file 上传的文件
     * @return 摄入结果，包含成功处理的块数量
     */
    @PostMapping("/ingest")
    public Result<String> ingest(@RequestParam("file") MultipartFile file) {
        // 调用服务层处理文档摄入
        int chunkCount = ragService.ingestDocument(file);
        return Result.success("文档摄入成功，共处理 " + chunkCount + " 个文本块");
    }

    /**
     * 知识库问答接口
     * 用户提问后，系统检索相关知识并生成回答
     * @param request 包含 question 字段的请求体
     * @return 生成的回答文本
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        // 调用服务层的完整 RAG 流程
        String answer = ragService.chat(request.getQuestion());
        return Result.success(answer);
    }

    // 内部请求 DTO
    public static class ChatRequest {
        private String question;
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }
}
```

#### 6.3 RedisTemplate 配置

```java
package com.itheima.tlias.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 配置 RedisTemplate，确保 Key 和 Value 都以字符串形式存储
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 RedisTemplate Bean
     * @param connectionFactory Redis 连接工厂，Spring Boot 自动配置
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(connectionFactory);

        // 使用 StringRedisSerializer 作为 Key 的序列化器
        // 这样 Redis 中存储的就是可读的字符串，而非二进制数据
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 也使用字符串序列化器
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        // 初始化设置
        template.afterPropertiesSet();
        return template;
    }
}
```

---

### 7. 前端实现

#### 7.1 悬浮 AI 助手球（Vue3 示例）

```vue
<template>
  <div class="ai-assistant">
    <!-- 悬浮球按钮 -->
    <div class="float-ball" @click="toggleChat">
      <span class="icon">🤖</span>
    </div>

    <!-- 聊天窗口 -->
    <div class="chat-window" v-show="isOpen">
      <div class="chat-header">
        <span>制度知识库助手</span>
        <button @click="toggleChat">×</button>
      </div>
      <div class="chat-body" ref="chatBody">
        <div v-for="(msg, index) in messages" :key="index"
             :class="['message', msg.role]">
          <div class="content" v-html="renderMarkdown(msg.content)"></div>
          <!-- 显示引用来源 -->
          <div class="sources" v-if="msg.sources">
            <span v-for="src in msg.sources" :key="src">来源：{{ src }}</span>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <input v-model="question" @keyup.enter="sendQuestion"
               placeholder="请输入问题，如：年假怎么请？" />
        <button @click="sendQuestion" :disabled="loading">
          {{ loading ? '思考中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { marked } from 'marked'; // Markdown 渲染库

const isOpen = ref(false);      // 聊天窗口是否打开
const question = ref('');       // 用户输入的问题
const messages = ref([]);       // 消息列表
const loading = ref(false);     // 是否正在加载
const chatBody = ref(null);     // 聊天内容区域引用

// 切换聊天窗口显示/隐藏
const toggleChat = () => {
  isOpen.value = !isOpen.value;
};

// 发送问题
const sendQuestion = async () => {
  if (!question.value.trim() || loading.value) return;

  // 添加用户消息到列表
  messages.value.push({ role: 'user', content: question.value });
  const currentQuestion = question.value;
  question.value = '';
  loading.value = true;

  try {
    // 调用后端 RAG 问答接口
    const res = await axios.post('/ai/rag/chat', {
      question: currentQuestion
    });
    // 添加助手回复到列表
    messages.value.push({
      role: 'assistant',
      content: res.data.data
    });
  } catch (err) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，服务暂时不可用，请稍后再试。'
    });
  } finally {
    loading.value = false;
  }
};

// Markdown 渲染
const renderMarkdown = (text) => {
  return marked(text);
};
</script>

<style scoped>
.float-ball {
  position: fixed;
  right: 30px;
  bottom: 30px;
  width: 60px;
  height: 60px;
  background: #409eff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.chat-window {
  position: fixed;
  right: 30px;
  bottom: 100px;
  width: 400px;
  height: 500px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
}
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.message.user { text-align: right; }
.message.assistant { text-align: left; }
</style>
```

#### 7.2 文档上传界面

```vue
<template>
  <div class="upload-section">
    <h3>上传制度文档</h3>
    <div class="upload-area" @drop="handleDrop" @dragover.prevent>
      <input type="file" ref="fileInput" accept=".pdf,.docx"
             @change="handleFileSelect" hidden />
      <p>点击或拖拽上传 PDF/DOCX 文件</p>
      <button @click="$refs.fileInput.click()">选择文件</button>
    </div>
    <div class="file-info" v-if="selectedFile">
      <span>{{ selectedFile.name }}</span>
      <button @click="uploadFile" :disabled="uploading">
        {{ uploading ? '上传中...' : '开始摄入' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';

const fileInput = ref(null);
const selectedFile = ref(null);
const uploading = ref(false);

const handleFileSelect = (e) => {
  selectedFile.value = e.target.files[0];
};

const handleDrop = (e) => {
  e.preventDefault();
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    selectedFile.value = files[0];
  }
};

const uploadFile = async () => {
  if (!selectedFile.value) return;
  uploading.value = true;

  const formData = new FormData();
  formData.append('file', selectedFile.value);

  try {
    const res = await axios.post('/ai/rag/ingest', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    alert(res.data.data); // 显示摄入成功消息
    selectedFile.value = null;
  } catch (err) {
    alert('上传失败：' + err.message);
  } finally {
    uploading.value = false;
  }
};
</script>
```

---

### 8. RAG 评估指标

如何知道你的 RAG 系统回答得好不好？以下是三个核心评估指标：

| 指标 | 定义 | 如何评估 | 目标值 |
|------|------|----------|--------|
| **检索准确率（Recall@K）** | 相关文档中被成功检索到的比例 | 准备 50 个标准问题，人工标注每个问题的相关片段，测试系统检索结果 | > 80% |
| **回答忠实度（Faithfulness）** | 回答中的信息是否都能在参考片段中找到依据 | 人工检查回答的每个陈述，判断是否有片段支撑 | > 90% |
| **回答相关性（Answer Relevance）** | 回答是否直接回应了用户的问题 | 让测试人员打分：1-5 分，看回答是否切题 | > 4.0 |

**简单评估方法（适合教学项目）：**

```java
/**
 * RAG 系统简易评估
 * 准备一组标准问题和预期答案，测试系统输出
 */
public class RagEvaluator {

    // 标准测试用例
    private static final List<TestCase> TEST_CASES = List.of(
        new TestCase("年假怎么请？", List.of("提前", "申请", "审批")),
        new TestCase("报销需要什么材料？", List.of("发票", "单据", "审批")),
        new TestCase("试用期多久？", List.of("试用", "期限", "个月"))
    );

    /**
     * 运行评估
     * @param ragService 被测试的 RAG 服务
     */
    public void evaluate(RagService ragService) {
        int passed = 0;
        for (TestCase tc : TEST_CASES) {
            String answer = ragService.chat(tc.question);
            // 检查回答中是否包含预期的关键词
            boolean containsKeywords = tc.expectedKeywords.stream()
                .allMatch(kw -> answer.contains(kw));
            if (containsKeywords) passed++;
            System.out.println("问题：" + tc.question);
            System.out.println("回答：" + answer.substring(0, Math.min(100, answer.length())));
            System.out.println("通过：" + containsKeywords);
            System.out.println("---");
        }
        System.out.println("通过率：" + passed + "/" + TEST_CASES.size());
    }

    record TestCase(String question, List<String> expectedKeywords) {}
}
```

---

## 动手练习

### 练习 1：完成文档摄入流程

1. 在 `tlias-pro-module-ai` 模块中创建 `PdfExtractor` 和 `WordExtractor` 工具类
2. 实现 `TextChunker.splitByParagraph()` 方法
3. 创建 `RagStorageService`，实现 `storeChunks()` 和 `clearAllChunks()`
4. 使用 Postman 或前端页面上传一份测试用的制度文档
5. 检查 Redis 中是否正确存储了文本块：`HGETALL rag:chunk:xxx`

### 练习 2：实现完整问答流程

1. 完成 `RagService` 的三个核心方法：`ingestDocument()`、`retrieve()`、`chat()`
2. 实现 `RagController` 的 `/ai/rag/ingest` 和 `/ai/rag/chat` 接口
3. 准备一份《员工手册》测试文档（至少包含年假、报销、考勤三个主题）
4. 测试以下问题，观察回答质量：
   - "年假怎么请？"
   - "报销需要什么材料？"
   - "公司几点上班？"（如果文档中没有，应回答"无法回答"）

### 练习 3：优化 Prompt 和约束

1. 修改 `buildPrompt()` 方法，尝试不同的系统角色设定
2. 加入"不知道就说不知道"的约束，测试模型是否会 hallucination
3. 尝试在 Prompt 中加入引用溯源要求，观察回答是否标注来源
4. 对比不同 Top-K 值（K=1, 3, 5）对回答质量的影响

---

## 常见错误排查

| 阶段 | 现象 | 可能原因 | 解决方案 |
|------|------|----------|----------|
| **文档摄入** | 上传 PDF 后报错 "PDF 文本提取失败" | PDF 是扫描件（图片），非文本型 PDF | 使用 OCR 工具预处理，或换用文本型 PDF 测试 |
| **文档摄入** | Redis 中没有存储任何 chunk | 分块时所有段落都短于 MIN_CHUNK_LENGTH | 调小 `MIN_CHUNK_LENGTH` 阈值，或检查原始文本 |
| **查询检索** | 无论问什么，都返回空结果 | Redis 中没有数据，或关键词提取失败 | 检查 Redis 中是否有 `rag:chunk:*` 的 Key；检查 LLM 关键词提取是否正常 |
| **查询检索** | 返回的片段与问题无关 | 关键词匹配太粗糙，匹配到了无关内容 | 增加关键词提取的准确性；或尝试加入同义词扩展 |
| **生成回答** | 模型编造了文档中没有的信息 | Prompt 约束不够严格 | 加强 Prompt 中的约束描述；加入"禁止编造"的明确指令 |
| **生成回答** | 回答太长、太啰嗦 | Prompt 中没有限制回答长度 | 在 Prompt 中加入"回答控制在 200 字以内"的约束 |
| **接口调用** | 调用 `/ai/rag/chat` 返回 500 | ChatClient 配置错误，或 Redis 连接失败 | 检查 `application.yml` 中的 API Key；检查 Redis 服务是否启动 |

---

## 本节小结

```
+-------------------------------------------------------------+
|                  RAG 制度知识库问答 知识脑图                  |
+-------------------------------------------------------------+
|                                                             |
|                        RAG 核心思想                          |
|                     "给大模型配参考书"                        |
|                             |                               |
|           +-----------------+----------------+              |
|           |                 |                |              |
|           v                 v                v              |
|      文档摄入           查询检索           生成回答          |
|           |                 |                |              |
|    +------+------+    +-----+-----+    +-----+-----+        |
|    |             |    |           |    |           |        |
|    v             v    v           v    v           v        |
|  文本提取      文本分块  关键词提取   Redis搜索   Prompt增强  LLM生成 |
|  (PDFBox/POI) (按段落)  (LLM辅助)  (Hash遍历)  (角色+约束) (Kimi) |
|    |             |                |                |        |
|    +------+------+                +--------+-------+        |
|           |                              |                  |
|           v                              v                  |
|      存入 Redis                      返回带引用回答          |
|                                                             |
|  【关键记忆点】                                               |
|  - RAG 解决大模型的幻觉、知识截止、私有知识三大痛点            |
|  - 分块策略推荐按段落分，兼顾语义完整性和检索精度              |
|  - Prompt 必须包含"不知道就说不知道"的约束                    |
|  - 生产环境应使用向量数据库替代 Redis 关键词匹配               |
|                                                             |
+-------------------------------------------------------------+
```

---

## 参考文档

1. [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/) - Spring AI 框架使用指南
2. [Apache PDFBox 文档](https://pdfbox.apache.org/) - PDF 文本提取库
3. [Apache POI 文档](https://poi.apache.org/) - Word 文本提取库
4. [Redis 官方文档](https://redis.io/documentation) - Redis 数据类型和命令参考
5. [RAG Survey Paper](https://arxiv.org/abs/2312.10997) - RAG 技术综述论文（英文）
6. [LangChain RAG 教程](https://python.langchain.com/docs/use_cases/question_answering/) - RAG 实现参考（概念通用）
7. [向量数据库对比](https://www.pinecone.io/learn/vector-database/) - 了解生产环境应使用的向量检索方案
