package com.dave.ai.transfer.service.biz;

import com.dave.ai.transfer.domain.param.ProductSaleParam;

public interface ProductSaleService {

    void sale(ProductSaleParam productSaleParam);

    void approval(Boolean approval, String executionId);
}
