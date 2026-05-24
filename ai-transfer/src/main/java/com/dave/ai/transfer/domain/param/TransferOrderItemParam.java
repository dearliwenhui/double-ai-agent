package com.dave.ai.transfer.domain.param;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TransferOrderItemParam {
    /**
     * 调拨单ID
     */
    private Long transferOrderId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 调拨数量
     */
    private BigDecimal transferQuantity;

    /**
     * 备注
     */
    private String remark;

}
