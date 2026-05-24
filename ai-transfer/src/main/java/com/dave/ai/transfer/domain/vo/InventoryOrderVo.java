package com.dave.ai.transfer.domain.vo;

import lombok.Data;

@Data
public class InventoryOrderVo {

    private Integer productId;
    private String productCode;
    private String productName;
    private String year;
    private Integer quarter;
    private Long totalTransferQty;
    private Integer sourceWarehouseId;
    private String sourceWarehouseCode;
    private String sourceWarehouseName;
    private Integer targetWarehouseId;
    private String targetWarehouseCode;
    private String targetWarehouseName;
}
