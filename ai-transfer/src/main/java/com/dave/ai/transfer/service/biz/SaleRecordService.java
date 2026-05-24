package com.dave.ai.transfer.service.biz;

import com.dave.ai.transfer.domain.vo.SaleRecordVo;

import java.util.List;

public interface SaleRecordService {

    List<SaleRecordVo> collectSaleRecordDataByProductId(Integer productId);


}
