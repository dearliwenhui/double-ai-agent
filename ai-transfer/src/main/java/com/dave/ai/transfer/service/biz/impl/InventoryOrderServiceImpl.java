package com.dave.ai.transfer.service.biz.impl;

import com.dave.ai.transfer.domain.vo.InventoryOrderVo;
import com.dave.ai.transfer.mapper.biz.InventoryOrderMapper;
import com.dave.ai.transfer.service.biz.InventoryOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class InventoryOrderServiceImpl implements InventoryOrderService {

    @Resource
    private InventoryOrderMapper inventoryOrderMapper;

    @Override
    public List<InventoryOrderVo> collectInventoryOrderDataByProductId(String productId) {
        return inventoryOrderMapper.collectInventoryOrderDataByProductId(productId);
    }
}
