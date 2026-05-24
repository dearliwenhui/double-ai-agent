package com.dave.ai.transfer.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import lombok.Data;

/**
 * 调拨单主表
 * @TableName bb_transfer_order
 */
@TableName(value ="bb_transfer_order")
@Data
public class BbTransferOrder implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
    private LocalDate transferDate;

    /**
     * 说明
     */
    private String comment;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 审核人
     */
    private String approvedBy;

    /**
     * 创建时间
     */
    private LocalDate createdTime;

    /**
     * 更新时间
     */
    private LocalDate updatedTime;

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
        BbTransferOrder other = (BbTransferOrder) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getOrderNo() == null ? other.getOrderNo() == null : this.getOrderNo().equals(other.getOrderNo()))
            && (this.getSourceWarehouseId() == null ? other.getSourceWarehouseId() == null : this.getSourceWarehouseId().equals(other.getSourceWarehouseId()))
            && (this.getTargetWarehouseId() == null ? other.getTargetWarehouseId() == null : this.getTargetWarehouseId().equals(other.getTargetWarehouseId()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getTransferType() == null ? other.getTransferType() == null : this.getTransferType().equals(other.getTransferType()))
            && (this.getTransferDate() == null ? other.getTransferDate() == null : this.getTransferDate().equals(other.getTransferDate()))
            && (this.getComment() == null ? other.getComment() == null : this.getComment().equals(other.getComment()))
            && (this.getCreatedBy() == null ? other.getCreatedBy() == null : this.getCreatedBy().equals(other.getCreatedBy()))
            && (this.getApprovedBy() == null ? other.getApprovedBy() == null : this.getApprovedBy().equals(other.getApprovedBy()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getUpdatedTime() == null ? other.getUpdatedTime() == null : this.getUpdatedTime().equals(other.getUpdatedTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getOrderNo() == null) ? 0 : getOrderNo().hashCode());
        result = prime * result + ((getSourceWarehouseId() == null) ? 0 : getSourceWarehouseId().hashCode());
        result = prime * result + ((getTargetWarehouseId() == null) ? 0 : getTargetWarehouseId().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getTransferType() == null) ? 0 : getTransferType().hashCode());
        result = prime * result + ((getTransferDate() == null) ? 0 : getTransferDate().hashCode());
        result = prime * result + ((getComment() == null) ? 0 : getComment().hashCode());
        result = prime * result + ((getCreatedBy() == null) ? 0 : getCreatedBy().hashCode());
        result = prime * result + ((getApprovedBy() == null) ? 0 : getApprovedBy().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getUpdatedTime() == null) ? 0 : getUpdatedTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", orderNo=").append(orderNo);
        sb.append(", sourceWarehouseId=").append(sourceWarehouseId);
        sb.append(", targetWarehouseId=").append(targetWarehouseId);
        sb.append(", status=").append(status);
        sb.append(", transferType=").append(transferType);
        sb.append(", transferDate=").append(transferDate);
        sb.append(", comment=").append(comment);
        sb.append(", createdBy=").append(createdBy);
        sb.append(", approvedBy=").append(approvedBy);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}