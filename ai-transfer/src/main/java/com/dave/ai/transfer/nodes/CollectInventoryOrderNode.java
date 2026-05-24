package com.dave.ai.transfer.nodes;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.dave.ai.transfer.domain.vo.InventoryOrderVo;
import com.dave.ai.transfer.service.biz.InventoryOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class CollectInventoryOrderNode implements NodeAction {

    private final InventoryOrderService inventoryOrderService;

    public CollectInventoryOrderNode(InventoryOrderService inventoryOrderService) {
        this.inventoryOrderService = inventoryOrderService;
    }

    /**
     * 1. 从状态机获取productId
     * 2. 写SQL查询这个商品对应的历史调拨情况
     * 维度： 年份、季度、仓库、商品
     * @param state
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String productId = state.value("productId", "");
        log.info("CollectInventoryOrderNode start, productId: {}", productId);
        if (StringUtils.isBlank(productId)) {
            log.warn("CollectInventoryOrderNode skip due to non-blank productId, productId: {}", productId);
            return Map.of();
        }
        List<InventoryOrderVo> inventoryOrderVos = inventoryOrderService.collectInventoryOrderDataByProductId(productId);
        String inventoryOrderData = JSONUtil.toJsonStr(inventoryOrderVos);
        log.info("CollectInventoryOrderNode success, order size: {}", inventoryOrderVos.size());
        return Map.of("inventoryOrderData", inventoryOrderData);
    }
}
