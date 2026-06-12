package com.dave.ai.bi.helper.application.sqlchat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SqlEvaluationService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public SqlEvaluationService(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = new ObjectMapper();
    }

    public SqlEvaluationResult evaluate(String userInput, String sql, String retrievalContext) {
        if (sql == null || sql.isBlank()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("pass", false);
            fallback.put("score", 0);
            fallback.put("intentMatched", false);
            fallback.put("mysqlCompatible", false);
            fallback.put("riskLevel", "HIGH");
            fallback.put("issues", java.util.List.of("未生成有效 SQL"));
            fallback.put("suggestions", java.util.List.of("请重新描述问题或补充筛选条件"));
            fallback.put("finalDecision", "REJECT");
            return new SqlEvaluationResult(fallback, toJson(fallback));
        }
        String systemPrompt = buildEvaluationPrompt(sql, retrievalContext);
        Flux<String> content = chatClientBuilder.build().prompt()
                .system(systemPrompt)
                .user(spec -> spec.text("用户的问题：{user_query}").params(Map.of("user_query", userInput)))
                .stream()
                .content();
        StringBuilder builder = new StringBuilder();
        content.doOnNext(builder::append).blockLast();
        String rawJson = builder.toString().trim();
        return new SqlEvaluationResult(parse(rawJson), rawJson);
    }

    private String buildEvaluationPrompt(String sql, String retrievalContext) {
        return ("""
                # 角色
                你是一名专业的 SQL 审核专家，负责评估生成 SQL 的质量、正确性与安全性。

                # 数据库类型
                当前数据库为 MySQL。
                所有 SQL 必须严格符合 MySQL 语法与执行行为。
                禁止出现 PostgreSQL、Oracle、SQL Server、Hive、SparkSQL 专属语法。

                # 核心目标
                你需要判断：
                1. SQL 能否在 MySQL 中正确执行
                2. SQL 是否安全
                3. SQL 性能是否合理
                4. SQL 是否真正回答了用户问题
                5. SQL 是否存在 AI 幻觉
                6. SQL 是否符合 MySQL 最佳实践

                需要评估的 SQL: {genSQL}

                # 输出要求
                你必须返回 JSON：
                {
                  "pass": true,
                  "score": 95,
                  "intentMatched": true,
                  "mysqlCompatible": true,
                  "riskLevel": "LOW",
                  "issues": [],
                  "suggestions": [],
                  "finalDecision": "ALLOW"
                }

                # 判定规则
                必须 REJECT 的情况：
                - 存在危险 SQL
                - SQL 无法在 MySQL 执行
                - 存在幻觉字段或表
                - 与用户意图不一致
                - 存在明显逻辑错误

                必须 REVIEW 的情况：
                - 存在性能风险
                - 缺少 LIMIT
                - 查询范围过大

                仅返回 JSON，不要返回 Markdown 或解释。

                以下是筛选后的 schema 上下文，请基于这些信息审核 SQL：
                """.replace("{genSQL}", sql))
                + "\n" + retrievalContext + "\n";
    }

    private Map<String, Object> parse(String rawJson) {
        try {
            return objectMapper.readValue(rawJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("pass", false);
            fallback.put("score", 0);
            fallback.put("intentMatched", false);
            fallback.put("mysqlCompatible", false);
            fallback.put("riskLevel", "HIGH");
            fallback.put("issues", java.util.List.of("评估结果无法解析为 JSON"));
            fallback.put("suggestions", java.util.List.of("请人工检查 SQL 后再继续"));
            fallback.put("finalDecision", "REVIEW");
            fallback.put("raw", rawJson);
            return fallback;
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public record SqlEvaluationResult(Map<String, Object> evaluateResult,
                                      String rawEvaluateJson) {
    }
}
