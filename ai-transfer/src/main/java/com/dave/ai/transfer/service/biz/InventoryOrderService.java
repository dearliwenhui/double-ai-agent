package com.dave.ai.transfer.service.biz;

import com.dave.ai.transfer.domain.vo.InventoryOrderVo;

import java.util.List;

public interface InventoryOrderService {

    List<InventoryOrderVo> collectInventoryOrderDataByProductId(String productId);

}
