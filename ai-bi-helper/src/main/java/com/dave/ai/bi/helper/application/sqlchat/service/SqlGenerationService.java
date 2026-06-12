package com.dave.ai.bi.helper.application.sqlchat.service;

import cn.hutool.core.text.StrBuilder;
import com.dave.ai.bi.helper.application.rag.dto.SchemaRetrievalContext;
import com.dave.ai.bi.helper.application.rag.service.SchemaRetrievalPipeline;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SqlGenerationService {

    private final ChatClient.Builder chatClientBuilder;
    private final SchemaRetrievalPipeline schemaRetrievalPipeline;

    public SqlGenerationService(ChatClient.Builder chatClientBuilder,
                                SchemaRetrievalPipeline schemaRetrievalPipeline) {
        this.chatClientBuilder = chatClientBuilder;
        this.schemaRetrievalPipeline = schemaRetrievalPipeline;
    }

    public SqlGenerationResult generate(String userInput) {
        SchemaRetrievalContext retrievalContext = schemaRetrievalPipeline.retrieve(userInput);
        Flux<String> content = chatClientBuilder.build().prompt()
                .system(buildGenerationPrompt(retrievalContext))
                .user(userInput)
                .stream()
                .content();
        StrBuilder sqlBuilder = new StrBuilder();
        content.doOnNext(sqlBuilder::append).blockLast();
        String sql = sanitizeSql(sqlBuilder.toString());
        return new SqlGenerationResult(
                sql,
                retrievalContext.assembledContext(),
                retrievalContext.queryIntent().rewrittenQuery()
        );
    }

    private String buildGenerationPrompt(SchemaRetrievalContext retrievalContext) {
        return """
                你是一名专业的 SQL 生成助手。

                你的任务是：
                根据“用户问题”和“筛选后的数据库 schema 上下文”，生成一条正确、可执行、符合 MySQL 语法的 SQL 查询语句。

                规则：
                1. 只输出 SQL，不要输出解释、分析、Markdown 代码块或额外文字。
                2. 只能使用上下文中明确给出的表名、字段名、关联关系和业务口径。
                3. 严禁虚构字段、表、关联关系、时间字段、状态字段和枚举值。
                4. 上下文不足时返回空字符串。
                5. 默认只生成 SELECT 查询，不生成 INSERT、UPDATE、DELETE、DROP、ALTER。
                6. 多表查询时，必须使用上下文中已知关系进行 JOIN。
                7. 涉及名称类维度时，优先关联维度表获取。
                8. 销售分析优先从 fact_sales 出发，库存分析优先从 fact_inventory 出发。
                9. 如存在多种写法，优先选择语义最准确、结构最清晰、性能更优的写法。
                10. 每个查询字段必须指定中文别名。

                输出格式要求：
                - 只输出 SQL 纯文本
                - 不要输出 ```sql
                - 不要输出任何额外字符

                以下是已经为你整理好的 schema 上下文，请严格只基于这些内容生成 SQL：
                """
                + "\n" + retrievalContext.assembledContext() + "\n";
    }

    private String sanitizeSql(String rawSql) {
        if (rawSql == null) {
            return "";
        }
        String sanitized = rawSql.trim();
        sanitized = sanitized.replace("```sql", "").replace("```", "").trim();
        return sanitized;
    }

    public record SqlGenerationResult(String generatedSql,
                                      String retrievalContext,
                                      String rewrittenQuery) {
    }
}
