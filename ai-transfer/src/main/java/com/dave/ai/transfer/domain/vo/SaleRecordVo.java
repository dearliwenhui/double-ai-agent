package com.dave.ai.transfer.domain.vo;

import lombok.Data;

@Data
public class SaleRecordVo {

    /**
     * 商品ID
     */
    private Integer productId;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 年份
     */
    private String year;

    /**
     * 季度
     */
    private Integer quarter;

    /**
     * 仓库ID
     */
    private Integer warehouseId;

    /**
     * 仓库编码
     */
    private String warehouseCode;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 总销售数量
     */
    private Long totalSaleQty;

}
