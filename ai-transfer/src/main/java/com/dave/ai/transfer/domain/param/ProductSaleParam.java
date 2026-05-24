package com.dave.ai.transfer.domain.param;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ProductSaleParam {
    private Integer productId;

    private Integer warehouseId;

    private Integer quantity;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate saleDate;
}
