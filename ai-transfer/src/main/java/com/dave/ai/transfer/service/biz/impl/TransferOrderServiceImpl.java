package com.dave.ai.transfer.service.biz.impl;

import cn.hutool.core.util.IdUtil;
import com.dave.ai.transfer.domain.convert.TransferOrderConvert;
import com.dave.ai.transfer.domain.param.TransferOrderItemParam;
import com.dave.ai.transfer.domain.param.TransferOrderParam;
import com.dave.ai.transfer.model.BbTransferOrder;
import com.dave.ai.transfer.model.BbTransferOrderItem;
import com.dave.ai.transfer.service.base.BbTransferOrderItemService;
import com.dave.ai.transfer.service.base.BbTransferOrderService;
import com.dave.ai.transfer.service.biz.TransferOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransferOrderServiceImpl implements TransferOrderService {

    @Value("${ai.transfer.snowflake.worker-id:1}")
    private long workerId;

    @Value("${ai.transfer.snowflake.datacenter-id:1}")
    private long datacenterId;

    @Resource
    private BbTransferOrderService bbTransferOrderService;

    @Resource
    private BbTransferOrderItemService bbTransferOrderItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int create(TransferOrderParam transferOrderParam) {
        transferOrderParam.setOrderNo(IdUtil.getSnowflake(workerId, datacenterId).nextIdStr());
        BbTransferOrder bbTransferOrder = TransferOrderConvert.INSTANCE.paramToModel(transferOrderParam);
        bbTransferOrderService.save(bbTransferOrder);

        List<BbTransferOrderItem> bbTransferOrderItems = TransferOrderConvert.INSTANCE.itemParamToModels(transferOrderParam.getItems());
        bbTransferOrderItems.forEach(item -> item.setTransferOrderId(bbTransferOrder.getId()));
        bbTransferOrderItemService.saveBatch(bbTransferOrderItems);
        return 0;
    }
}
