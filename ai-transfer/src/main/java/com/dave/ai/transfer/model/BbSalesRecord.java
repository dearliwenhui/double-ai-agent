package com.dave.ai.transfer.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 商品销售记录表
 * @TableName bb_sales_record
 */
@TableName(value ="bb_sales_record")
@Data
public class BbSalesRecord implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 销售日期
     */
    private Date saleDate;

    /**
     * 销售数量
     */
    private BigDecimal quantity;

    /**
     * 销售金额
     */
    private BigDecimal revenue;

    /**
     * 创建时间
     */
    private Date createdTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        BbSalesRecord other = (BbSalesRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getWarehouseId() == null ? other.getWarehouseId() == null : this.getWarehouseId().equals(other.getWarehouseId()))
            && (this.getProductId() == null ? other.getProductId() == null : this.getProductId().equals(other.getProductId()))
            && (this.getSaleDate() == null ? other.getSaleDate() == null : this.getSaleDate().equals(other.getSaleDate()))
            && (this.getQuantity() == null ? other.getQuantity() == null : this.getQuantity().equals(other.getQuantity()))
            && (this.getRevenue() == null ? other.getRevenue() == null : this.getRevenue().equals(other.getRevenue()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getWarehouseId() == null) ? 0 : getWarehouseId().hashCode());
        result = prime * result + ((getProductId() == null) ? 0 : getProductId().hashCode());
        result = prime * result + ((getSaleDate() == null) ? 0 : getSaleDate().hashCode());
        result = prime * result + ((getQuantity() == null) ? 0 : getQuantity().hashCode());
        result = prime * result + ((getRevenue() == null) ? 0 : getRevenue().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", warehouseId=").append(warehouseId);
        sb.append(", productId=").append(productId);
        sb.append(", saleDate=").append(saleDate);
        sb.append(", quantity=").append(quantity);
        sb.append(", revenue=").append(revenue);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}