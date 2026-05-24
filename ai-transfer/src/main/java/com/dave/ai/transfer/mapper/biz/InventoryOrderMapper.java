package com.dave.ai.transfer.mapper.biz;

import com.dave.ai.transfer.domain.vo.InventoryOrderVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InventoryOrderMapper{

    @Select("""
            select
                bp.id product_id,
                bp.product_code,
                bp.product_name,
                YEAR (bto.transfer_date) `year`,
                QUARTER(bto.transfer_date) `quarter`,
                sum(btoi.transfer_quantity) totalTransferQty,
                bto.source_warehouse_id,
                bw.warehouse_code source_warehouse_code,
                bw.warehouse_name source_warehouse_name,
                bto.target_warehouse_id,
                bw2.warehouse_code target_warehouse_code,
                bw2.warehouse_name target_warehouse_name
            from
            bb_transfer_order bto
            join bb_transfer_order_item btoi on bto.id = btoi.transfer_order_id
            join bb_product bp on bp.id =btoi.product_id
            join bb_warehouse bw on bto.source_warehouse_id =bw.id
            join bb_warehouse bw2 on bto.target_warehouse_id =bw2.id
            where bp.id = #{productId} and bto.status =3
            group by
            bp.id,YEAR (bto.transfer_date),QUARTER(bto.transfer_date),bw.id, bw2.id
            """)
    List<InventoryOrderVo> collectInventoryOrderDataByProductId(@Param("productId") String productchiwaId);

}
