package com.dave.ai.transfer.nodes;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.dave.ai.transfer.domain.param.TransferOrderParam;
import com.dave.ai.transfer.service.biz.TransferOrderService;

import java.util.Map;

public class CreateInventoryTransferNode implements NodeAction {

    private final TransferOrderService transferOrderService;

    public CreateInventoryTransferNode(TransferOrderService transferOrderService) {
        this.transferOrderService = transferOrderService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String inventoryTransferJsonStr = state.value("inventoryTransferJsonStr", "");
        TransferOrderParam transferOrderParam = JSONUtil.toBean(inventoryTransferJsonStr, TransferOrderParam.class);
        transferOrderService.create(transferOrderParam);
        return Map.of();
    }
}
