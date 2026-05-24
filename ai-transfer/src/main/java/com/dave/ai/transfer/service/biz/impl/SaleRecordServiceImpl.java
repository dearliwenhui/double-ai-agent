package com.dave.ai.transfer.service.biz.impl;

import com.dave.ai.transfer.domain.vo.SaleRecordVo;
import com.dave.ai.transfer.mapper.biz.SaleRecordMapper;
import com.dave.ai.transfer.service.biz.SaleRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SaleRecordServiceImpl implements SaleRecordService {

    @Resource
    private SaleRecordMapper saleRecordMapper;

    @Override
    public List<SaleRecordVo> collectSaleRecordDataByProductId(Integer productId) {
        return saleRecordMapper.collectSaleRecordDataByProductId(productId);
    }
}
