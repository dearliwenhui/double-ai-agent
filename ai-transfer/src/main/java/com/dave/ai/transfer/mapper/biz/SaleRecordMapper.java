package com.dave.ai.transfer.mapper.biz;

import com.dave.ai.transfer.domain.vo.SaleRecordVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleRecordMapper {

    @Select("""
            select
                bp.id as productId,
                bp.product_code as productCode,
                bp.product_name as productName,
                YEAR(bsr.sale_date) as year,
                QUARTER(bsr.sale_date) as quarter,
                bw.id as warehouseId,
                bw.warehouse_code as warehouseCode,
                bw.warehouse_name as warehouseName,
                SUM(bsr.quantity) as totalSaleQty
            from
                bb_sales_record bsr
            JOIN bb_product bp on
                bsr.product_id = bp.id
            JOIN bb_warehouse bw on
                bsr.warehouse_id = bw.id
            where bp.id = #{productId}
            group by
                bp.id, bp.product_code, bp.product_name,
                YEAR(bsr.sale_date), QUARTER(bsr.sale_date),
                bw.id, bw.warehouse_code, bw.warehouse_name
            """)
    List<SaleRecordVo> collectSaleRecordDataByProductId(@Param("productId") Integer productId);

}
