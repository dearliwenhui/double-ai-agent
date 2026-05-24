package com.dave.ai.transfer.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 调拨单明细表
 * @TableName bb_transfer_order_item
 */
@TableName(value ="bb_transfer_order_item")
@Data
public class BbTransferOrderItem implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 实际到货数量
     */
    private BigDecimal actualQuantity;

    /**
     * 备注
     */
    private String remark;

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
        BbTransferOrderItem other = (BbTransferOrderItem) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getTransferOrderId() == null ? other.getTransferOrderId() == null : this.getTransferOrderId().equals(other.getTransferOrderId()))
            && (this.getProductId() == null ? other.getProductId() == null : this.getProductId().equals(other.getProductId()))
            && (this.getTransferQuantity() == null ? other.getTransferQuantity() == null : this.getTransferQuantity().equals(other.getTransferQuantity()))
            && (this.getActualQuantity() == null ? other.getActualQuantity() == null : this.getActualQuantity().equals(other.getActualQuantity()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTransferOrderId() == null) ? 0 : getTransferOrderId().hashCode());
        result = prime * result + ((getProductId() == null) ? 0 : getProductId().hashCode());
        result = prime * result + ((getTransferQuantity() == null) ? 0 : getTransferQuantity().hashCode());
        result = prime * result + ((getActualQuantity() == null) ? 0 : getActualQuantity().hashCode());
        result = prime * result + ((getRemark() == null) ? 0 : getRemark().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", transferOrderId=").append(transferOrderId);
        sb.append(", productId=").append(productId);
        sb.append(", transferQuantity=").append(transferQuantity);
        sb.append(", actualQuantity=").append(actualQuantity);
        sb.append(", remark=").append(remark);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}