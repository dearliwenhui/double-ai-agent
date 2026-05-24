package com.dave.ai.transfer.domain.param;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TransferOrderParam {
    /**
     * 调拨单号
     */
    private String orderNo;

    /**
     * 调出仓库ID
     */
    private Long sourceWarehouseId;

    /**
     * 调入仓库ID
     */
    private Long targetWarehouseId;

    /**
     * 状态（0：待审核，1：已审核，2：调拨中，3：已完成，4：已驳回）
     */
    private Integer status;

    /**
     * 调拨类型：1智能，0人工
     */
    private Integer transferType;

    /**
     * 调拨日期
     */
    private String transferDate;

    /**
     * 说明
     */
    private String comment;

    /**
     * 创建人
     */
    private String createdBy;

    private List<TransferOrderItemParam> items;

}
