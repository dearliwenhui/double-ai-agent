package com.dave.ai.bi.helper.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.dave.ai.bi.helper.application.rag.service.SchemaRetrievalPipeline;
import com.dave.ai.bi.helper.edges.EvaluateEdge;
import com.dave.ai.bi.helper.nodes.CsvSqlAndCreateCsvNode;
import com.dave.ai.bi.helper.nodes.EvaluateNode;
import com.dave.ai.bi.helper.nodes.GenSQLNode;
import com.dave.ai.bi.helper.nodes.SendEmailNode;
import com.dave.ai.bi.helper.service.EmailService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

@Configuration
public class GraphConfig {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private EmailService emailService;

    @Resource
    private SchemaRetrievalPipeline schemaRetrievalPipeline;

    @Bean
    public CompiledGraph graph(ChatClient.Builder builder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of(
                        "userInput", new ReplaceStrategy()
                        , "genSQL", new ReplaceStrategy()
                        , "csvFilePath", new ReplaceStrategy()
                        , "evaluateResult", new ReplaceStrategy()
                        , "evaluateCount", new ReplaceStrategy()
                );
            }
        };

        StateGraph stateGraph = new StateGraph("biHelperGraph", keyStrategyFactory);

        stateGraph.addNode("GenSQLNode", AsyncNodeAction.node_async(new GenSQLNode(builder, schemaRetrievalPipeline)));
        stateGraph.addNode("EvaluateNode", AsyncNodeAction.node_async(new EvaluateNode(builder, schemaRetrievalPipeline)));
        stateGraph.addNode("ExecSqlAndCreateCsvNode", AsyncNodeAction.node_async(new CsvSqlAndCreateCsvNode(jdbcTemplate)));
        stateGraph.addNode("SendEmailNode", AsyncNodeAction.node_async(new SendEmailNode(emailService)));

        stateGraph.addEdge(StateGraph.START, "GenSQLNode");
        stateGraph.addEdge("GenSQLNode", "EvaluateNode");
        stateGraph.addConditionalEdges("EvaluateNode", AsyncEdgeAction.edge_async(new EvaluateEdge()),
                Map.of(
                        "GenSQLNode", "GenSQLNode",
                        "ExecSqlAndCreateCsvNode", "ExecSqlAndCreateCsvNode",
                        StateGraph.END, StateGraph.END
                ));
        stateGraph.addEdge("ExecSqlAndCreateCsvNode", "SendEmailNode");
        stateGraph.addEdge("SendEmailNode", StateGraph.END);
        return stateGraph.compile();
    }

}
