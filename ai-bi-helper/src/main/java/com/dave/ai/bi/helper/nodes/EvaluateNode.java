package com.dave.ai.bi.helper.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
public class EvaluateNode implements NodeAction {

    private final ChatClient.Builder chatClientBuilder;

    private final VectorStore vectorStore;

    public EvaluateNode(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClientBuilder = chatClientBuilder;
        this.vectorStore = vectorStore;
    }


    /**
     *
     * @param state
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String genSQL = state.value("genSQL", "");
        String userInput = state.value("userInput", "");
        Integer evaluateCount = state.value("evaluateCount", 0);

        if (genSQL.isEmpty()) {
            log.info("genSQL is empty");
            return Map.of("evaluateResult", "");
        }


        // 在检索前先改写用户问题，让向量库可以用更清晰的查询表达召回库表结构和业务文档。
        RewriteQueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder().chatClientBuilder(chatClientBuilder).build();

        // 组装 RAG 增强链路：
        // 1. 先对用户问题做改写；
        // 2. 再从向量库检索相关上下文；
        // 3. 即使没有检索到上下文，也允许继续生成 SQL。
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(rewriteQueryTransformer)
                .documentRetriever(VectorStoreDocumentRetriever
                        .builder().vectorStore(vectorStore).build())
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
                .build();
        Flux<String> content = chatClientBuilder.build().prompt()
                .advisors(retrievalAugmentationAdvisor)
                .system("""
                        # 角色
                        你是一个专业的SQL审核专家，专门负责评估生成的SQL查询的质量、正确性和安全性。
                        # 数据库类型
                        
                        当前数据库为 MySQL。
                        所有 SQL 必须严格符合 MySQL 语法规范与执行行为。
                        禁止生成或允许以下非 MySQL 语法：
                        - PostgreSQL 专属语法
                        - Oracle 专属语法
                        - SQL Server 专属语法
                        - Hive / SparkSQL 语法
                        禁止出现：
                        - TOP N
                        - NVL
                        - ROWNUM
                        - DISTINCT ON
                        - RETURNING
                        - MERGE INTO
                        - QUALIFY
                        - :: 类型转换
                        - ILIKE
                        
                        # 核心目标
                        
                        你需要判断：
                        
                        1. SQL 能否在 MySQL 中正确执行
                        2. SQL 是否安全
                        3. SQL 性能是否合理
                        4. SQL 是否真正回答了用户问题
                        5. SQL 是否存在 AI 幻觉
                        6. SQL 是否符合 MySQL 最佳实践
                        
                        # 审核维度
                        
                        ## 1. 用户意图一致性（最重要）
                        
                        检查：
                        
                        - SQL 是否真正回答了用户问题
                        - 查询目标是否正确
                        - 指标计算是否符合用户需求
                        - 时间范围是否符合问题描述
                        - 聚合粒度是否正确
                        - 排序逻辑是否合理
                        - 是否遗漏关键过滤条件
                        - 是否返回了用户真正需要的数据
                        
                        如果 SQL 与用户问题不一致，即使语法正确，也不能通过。
                        
                        ## 2. MySQL 语法正确性
                        需要评估的SQL: {genSQL}
                        
                        检查：
                        
                        - SQL 是否符合 MySQL 语法
                        - LIMIT 语法是否正确
                        - GROUP BY 是否符合 MySQL 行为
                        - ORDER BY 是否合法
                        - JOIN 语法是否正确
                        - 子查询是否符合 MySQL 规范
                        - UNION 使用是否合理
                        - HAVING 是否正确使用
                        - 日期函数是否为 MySQL 函数
                        - JSON 函数是否被 MySQL 支持
                        - 字符串函数是否兼容 MySQL
                        
                        重点检查以下 MySQL 函数：
                        
                        日期函数：
                        - DATE_FORMAT
                        - DATE_SUB
                        - DATE_ADD
                        - CURDATE
                        - NOW
                        
                        聚合函数：
                        - COUNT
                        - SUM
                        - AVG
                        - MAX
                        - MIN
                        - GROUP_CONCAT
                        
                        JSON函数：
                        - JSON_EXTRACT
                        - JSON_UNQUOTE
                        
                        字符串函数：
                        - CONCAT
                        - SUBSTRING
                        - IFNULL
                        
                        ## 3. AI 幻觉检查
                        
                        检查：
                        
                        - 是否使用不存在的字段
                        - 是否使用不存在的表
                        - 是否伪造业务字段
                        - 是否错误理解表结构
                        - 是否生成无意义 SQL
                        
                        ## 4. MySQL 安全审核
                        
                        检查：
                        
                        - 是否存在 DELETE / UPDATE / DROP 等危险操作
                        - 是否存在 TRUNCATE
                        - 是否存在 ALTER TABLE
                        - 是否存在 SQL 注入风险
                        - 是否缺少 WHERE 条件
                        - 是否可能造成全表查询
                        - 是否缺少 LIMIT
                        - 是否可能返回超大结果集
                        
                        以下 SQL 必须直接拒绝：
                        
                        - DROP
                        - TRUNCATE
                        - DELETE
                        - UPDATE
                        - ALTER
                        - CREATE
                        - INSERT
                        
                        仅允许 SELECT 查询。
                        
                        ## 5. MySQL 性能审核
                        
                        检查：
                        
                        - 是否可能全表扫描
                        - 是否存在 SELECT *
                        - JOIN 数量是否过多
                        - 是否存在笛卡尔积
                        - WHERE 条件是否有效
                        - ORDER BY 是否可能导致文件排序
                        - GROUP BY 是否可能导致临时表
                        - 是否存在深层嵌套子查询
                        - LIMIT 是否合理
                        
                        ## 6. MySQL 最佳实践审核
                        
                        检查：
                        
                        - 是否避免 SELECT *
                        - 是否合理使用别名
                        - 是否明确指定字段
                        - 是否存在不必要排序
                        - 是否存在无意义 DISTINCT
                        - 是否存在重复聚合
                        - 是否使用合理分页
                        
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
                        
                        # 字段说明
                        
                        - pass:
                        是否允许执行
                        
                        - score:
                        综合评分（0-100）
                        
                        - intentMatched:
                        是否满足用户真实意图
                        
                        - mysqlCompatible:
                        是否兼容 MySQL
                        
                        - riskLevel:
                        LOW / MEDIUM / HIGH
                        
                        - finalDecision:
                        ALLOW / REVIEW / REJECT
                        
                        # 判定规则
                        
                        以下情况必须 REJECT：
                        
                        - 存在危险 SQL
                        - SQL 无法在 MySQL 执行
                        - 存在幻觉字段
                        - 不满足用户意图
                        - 存在明显逻辑错误
                        - 存在非 MySQL 语法
                        
                        以下情况必须 REVIEW：
                        
                        - 存在性能风险
                        - 缺少 LIMIT
                        - 查询范围过大
                        - 存在潜在歧义
                        
                        # 输出限制
                        
                        - 仅返回 JSON
                        - 不要输出 Markdown
                        - 不要输出解释
                        - 不要输出代码块
                        
                        """.replace("{genSQL}", genSQL))
                .user(promptUserSpec -> promptUserSpec.text("""
                        用户的问题: {user_query}
                        """).params(Map.of("user_query", userInput)))
                .stream().content();
        StringBuilder builder = new StringBuilder();
        content.doOnNext(builder::append).blockLast();
        state.input(Map.of("evaluateCount", ++evaluateCount));
        log.info("evaluateResult json: {}", builder);
        return Map.of("evaluateResult", builder.toString());
    }
}
