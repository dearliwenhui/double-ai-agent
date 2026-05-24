package com.dave.ai.transfer.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.dave.ai.transfer.edges.ApprovalEdge;
import com.dave.ai.transfer.nodes.*;
import com.dave.ai.transfer.service.base.BbInventoryService;
import com.dave.ai.transfer.service.biz.EmailService;
import com.dave.ai.transfer.service.biz.InventoryOrderService;
import com.dave.ai.transfer.service.biz.SaleRecordService;
import com.dave.ai.transfer.service.biz.TransferOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
public class GraphConfig {

    @Resource
    private SaleRecordService saleRecordService;

    @Resource
    private BbInventoryService bbInventoryService;

    @Resource
    private InventoryOrderService inventoryOrderService;

    @Resource
    private EmailService emailService;

    @Resource
    private TransferOrderService transferOrderService;

    @Resource
    private RedissonClient redissonClient;

    @Value("${ai.transfer.approval.base-url:http://127.0.0.1:8080/saleProduct/approval}")
    private String approvalBaseUrl;

    @Bean
    public CompiledGraph graph(ChatClient.Builder builder) throws GraphStateException {
        log.info("Initializing inventoryTransferGraph, saleRecordService: {}, bbInventoryService: {}",
                saleRecordService.getClass().getSimpleName(),
                bbInventoryService.getClass().getSimpleName());

        StateGraph stateGraph = getStateGraph();
        log.info("StateGraph created: inventoryTransferGraph");

        stateGraph.addNode("saleRecordNode", AsyncNodeAction.node_async(new CollectSaleRecordNode(saleRecordService, bbInventoryService)));
        log.info("Node registered: saleRecordNode");

        stateGraph.addNode("inventoryOrderDataNode", AsyncNodeAction.node_async(new CollectInventoryOrderNode(inventoryOrderService)));
        stateGraph.addNode("predictNode", AsyncNodeAction.node_async(new PredictNode(builder.build())));
        stateGraph.addNode("extractNode", AsyncNodeAction.node_async(new ExtractNode(builder.build())));
        stateGraph.addNode("sendEmailNode", AsyncNodeAction.node_async(new SendEmailNode(emailService, approvalBaseUrl)));
        stateGraph.addNode("humanApprovalNode", AsyncNodeAction.node_async(new HumanApprovalNode()));
        stateGraph.addNode("createInventoryTransferNode", AsyncNodeAction.node_async(new CreateInventoryTransferNode(transferOrderService)));

        stateGraph.addEdge(StateGraph.START, "saleRecordNode");
        stateGraph.addEdge(StateGraph.START, "inventoryOrderDataNode");
        stateGraph.addEdge("saleRecordNode", "predictNode");
        stateGraph.addEdge("inventoryOrderDataNode", "predictNode");
        stateGraph.addEdge("predictNode", "extractNode");
        stateGraph.addEdge("extractNode", "sendEmailNode");
        stateGraph.addEdge("sendEmailNode", "humanApprovalNode");
        stateGraph.addConditionalEdges("humanApprovalNode", AsyncEdgeAction.edge_async(new ApprovalEdge()),
                Map.of("createInventoryTransferNode", "createInventoryTransferNode"
                        , StateGraph.END, StateGraph.END));
        stateGraph.addEdge("createInventoryTransferNode", StateGraph.END);

        log.info("Edges registered: START -> saleRecordNode / inventoryOrderDataNode -> predictNode -> extractNode -> sendEmailNode -> END");

        SaverConfig saverConfig = SaverConfig.builder()
                .register(SaverEnum.REDIS.getValue(), new RedisSaver(redissonClient)).build();

        CompileConfig config = CompileConfig.builder()
                .interruptBefore("humanApprovalNode")
                .saverConfig(saverConfig)
                .build();

        CompiledGraph compiledGraph = stateGraph.compile(config);
        log.info("inventoryTransferGraph compiled successfully");
        GraphRepresentation graph = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID, "inventoryTransferGraph", true);
        log.info("inventoryTransferGraph representation: {}", graph.content());

        return compiledGraph;
    }

    @NotNull
    private static StateGraph getStateGraph() {
        KeyStrategyFactory keyStrategyFactory = () -> {
            log.info("Creating key strategies for inventoryTransferGraph");
            return Map.of("productId", new ReplaceStrategy(),
                    "saleRecordData", new ReplaceStrategy(),
                    "nowProductInventoryData", new ReplaceStrategy(),
                    "inventoryOrderData", new ReplaceStrategy(),
                    "inventoryTransferStr", new ReplaceStrategy(),
                    "inventoryTransferJsonStr", new ReplaceStrategy(),
                    "humanApprovalNextStep", new ReplaceStrategy()
            );
        };

        return new StateGraph("inventoryTransferGraph", keyStrategyFactory);
    }

}
