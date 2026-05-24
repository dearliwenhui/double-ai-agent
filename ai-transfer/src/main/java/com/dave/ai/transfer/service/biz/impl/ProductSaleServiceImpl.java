package com.dave.ai.transfer.service.biz.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dave.ai.transfer.constants.TopicConstant;
import com.dave.ai.transfer.domain.convert.SaleRecordConvert;
import com.dave.ai.transfer.domain.param.ProductSaleParam;
import com.dave.ai.transfer.model.BbInventory;
import com.dave.ai.transfer.model.BbSalesRecord;
import com.dave.ai.transfer.service.base.BbInventoryService;
import com.dave.ai.transfer.service.base.BbSalesRecordService;
import com.dave.ai.transfer.service.biz.ProductSaleService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RefreshScope
public class ProductSaleServiceImpl implements ProductSaleService {


    @Resource
    private BbSalesRecordService bbSalesRecordService;

    @Resource
    private BbInventoryService bbInventoryService;

    @Resource
    private KafkaTemplate kafkaTemplate;

    @Resource
    private CompiledGraph graph;

    @Value("${threshold.productCount:1}")
    private Integer thresholdProductCount;

    @Override
    public void sale(ProductSaleParam productSaleParam) {
        // 保存一个销售记录
        BbSalesRecord bbSalesRecord = SaleRecordConvert.INSTANCE.paramToModel(productSaleParam);
        bbSalesRecordService.save(bbSalesRecord);
        // 更新库存信息
        bbInventoryService.update(Wrappers.lambdaUpdate(BbInventory.class)
                .eq(BbInventory::getProductId, productSaleParam.getProductId())
                .eq(BbInventory::getWarehouseId, productSaleParam.getWarehouseId())
                .setSql("quantity=quantity-" + productSaleParam.getQuantity())
        );
        // 判断是否达到阈值，若没有达到阈值则直接介绍，反之发送msg到kafka
        BbInventory inventory = bbInventoryService.getOne(Wrappers.<BbInventory>lambdaQuery()
                .eq(BbInventory::getProductId, productSaleParam.getProductId())
                .eq(BbInventory::getWarehouseId, productSaleParam.getWarehouseId())
        );
        if (inventory == null || inventory.getQuantity().intValue() > thresholdProductCount) {
            return;
        }
        //发送消息到kafka
        kafkaTemplate.send(TopicConstant.PRODUCT_SALE_TOPIC, JSONUtil.toJsonStr(productSaleParam));
    }

    @Override
    public void approval(Boolean approval, String executionId) {
        RunnableConfig build = RunnableConfig.builder().threadId(executionId).build();
        // 读取该 executionId 对应的执行快照；sale 接口首次运行后会在 humanApprovalNode 前中断并保存现场。
        // 使用同一个 executionId 取回之前暂停的那条图执行链路，避免和其他请求串线。
        StateSnapshot stateSnapshot = graph.getState(build);
        OverAllState state = stateSnapshot.state();
        // 标记这次调用为从中断点恢复执行，而不是从 START 重新跑整张图。
        state.withResume();

        HashMap<String, Object> map = new HashMap<>();
        map.put("approval", approval);
        // 将人工审批结果写入状态，供 HumanApprovalNode 从 state.humanFeedback() 中读取。
        state.withHumanFeedback(new OverAllState.HumanFeedback(map, ""));

        // 基于恢复后的状态继续执行图，后续会从 humanApprovalNode 开始往下走。
        OverAllState overAllState = graph.call(state, build).get();
        Map<String, Object> data = overAllState.data();
    }
}
