package com.dave.ai.bi.helper.nodes;

import cn.hutool.core.text.StrBuilder;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.dave.ai.bi.helper.application.rag.dto.SchemaRetrievalContext;
import com.dave.ai.bi.helper.application.rag.service.SchemaRetrievalPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class GenSQLNode implements NodeAction {


    private final ChatClient.Builder chatClientBuilder;
    private final SchemaRetrievalPipeline schemaRetrievalPipeline;

    public GenSQLNode(ChatClient.Builder chatClientBuilder, SchemaRetrievalPipeline schemaRetrievalPipeline) {
        this.chatClientBuilder = chatClientBuilder;
        this.schemaRetrievalPipeline = schemaRetrievalPipeline;
    }

    /**
     * 根据当前图状态中的业务问题、表结构信息和约束条件，调用大模型生成 SQL。
     *
     * <p>执行流程：</p>
     * <p>1. 从 state 中读取用户问题、库表结构、字段说明等上下文信息。</p>
     * <p>2. 组装生成 SQL 所需的提示词，明确查询目标、表名、字段名和输出要求。</p>
     * <p>3. 调用 ChatClient 生成 SQL 语句。</p>
     * <p>4. 对模型返回结果做基础清洗，例如去掉 Markdown 代码块、解释性文本等无关内容。</p>
     * <p>5. 将生成后的 SQL 以 genSQL 字段写回状态，供后续节点继续处理。</p>
     *
     * @param state 图执行过程中的全局状态，包含问题描述及中间结果
     * @return 返回需要写回图状态的字段，例如 genSQL
     * @throws Exception 当模型调用失败或结果解析失败时抛出异常
     */

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 从图状态中读取原始用户问题，作为后续检索增强和 SQL 生成的输入。
        String userInput = state.value("userInput", "");
        SchemaRetrievalContext retrievalContext = schemaRetrievalPipeline.retrieve(userInput);
        // 创建提示词请求，挂载检索增强 advisor，并以流式方式接收模型输出的 SQL 片段。
        Flux<String> content = chatClientBuilder.build().prompt()
                // system 提示词用于约束模型角色、可用库表范围以及输出格式。
                .system("""
                        你是一名专业的 SQL 生成助手。
                        
                        你的任务是：
                        根据“用户问题”和“检索到的数据库表结构上下文”，生成一条正确、可执行、符合 MySQL 语法的 SQL 查询语句。
                        
                        【生成规则】
                        1. 只输出 SQL，本次回复中禁止输出任何解释、说明、注释、分析过程、Markdown 代码块或多余文本。
                        2. 只能使用上下文中明确提供的表名、字段名、主外键关系和业务口径。
                        3. 严禁虚构字段、表、关联关系、时间字段、状态字段、枚举值。
                        4. 若上下文不足以支持准确生成 SQL，则返回空字符串。
                        5. 默认只生成 SELECT 查询，不生成 INSERT、UPDATE、DELETE、DROP、ALTER 等语句。
                        6. 多表查询时，必须使用上下文中已知的关系进行 JOIN。
                        7. 若用户问题涉及维度属性名称，如商品名称、门店名称、客户名称、时间属性，优先关联维度表获取。
                        8. 若问题涉及销售分析，优先从 fact_sales 出发。
                        9. 若问题涉及库存分析，优先从 fact_inventory 出发。
                        10. 如果存在多种 SQL 写法，优先选择语义最准确、结构最清晰、性能更优的写法。
                        11. 每个查询字段必须指定一个中文别名，需要简短清晰描述字段含义。例如：`user_name` AS 用户名 。
                        
                        【输出格式要求】
                        - 只输出 SQL 纯文本
                        - 不要输出 ```sql
                        - 不要输出任何额外字符
                        
                        以下是已经为你筛选和整理好的 schema 上下文，请严格只基于这些内容生成 SQL：
                        """ + "\n" + retrievalContext.assembledContext() + "\n")
                // 原始用户问题作为 user message 传入，检索得到的上下文由 advisor 注入。
                .user(userInput).stream().content();
        // 将流式返回的 SQL 片段拼接成完整 SQL。
        StrBuilder strBuilder = new StrBuilder();
        content.doOnNext(strBuilder::append).blockLast();
        // 将生成结果写回图状态，供后续节点继续处理。
        String genSQL = strBuilder.toString();
        log.info("Generated SQL: {}", genSQL);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("genSQL", genSQL);
        result.put("retrievalContext", retrievalContext.assembledContext());
        result.put("rewrittenQuery", retrievalContext.queryIntent().rewrittenQuery());
        return result;
    }
}
