package com.dave.ai.transfer.domain.convert;

import com.dave.ai.transfer.domain.param.ProductSaleParam;
import com.dave.ai.transfer.model.BbSalesRecord;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SaleRecordConvert {

    SaleRecordConvert INSTANCE = Mappers.getMapper(SaleRecordConvert.class);

    BbSalesRecord paramToModel(ProductSaleParam productSaleParam);

}
