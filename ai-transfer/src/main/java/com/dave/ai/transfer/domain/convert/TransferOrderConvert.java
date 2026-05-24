package com.dave.ai.transfer.domain.convert;

import com.dave.ai.transfer.domain.param.TransferOrderItemParam;
import com.dave.ai.transfer.domain.param.TransferOrderParam;
import com.dave.ai.transfer.model.BbTransferOrder;
import com.dave.ai.transfer.model.BbTransferOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TransferOrderConvert {
    TransferOrderConvert INSTANCE = Mappers.getMapper(TransferOrderConvert.class);

    BbTransferOrder paramToModel(TransferOrderParam transferOrderParam);

    BbTransferOrderItem itemParamToModel(TransferOrderItemParam transferOrderItemParam);

    List<BbTransferOrderItem> itemParamToModels(List<TransferOrderItemParam> transferOrderItemParams);

}
