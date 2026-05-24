package com.dave.ai.transfer.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 商品信息表
 * @TableName bb_product
 */
@TableName(value ="bb_product")
@Data
public class BbProduct implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 分类
     */
    private String category;

    /**
     * 状态（1：启用，0：停用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;

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
        BbProduct other = (BbProduct) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getProductCode() == null ? other.getProductCode() == null : this.getProductCode().equals(other.getProductCode()))
            && (this.getProductName() == null ? other.getProductName() == null : this.getProductName().equals(other.getProductName()))
            && (this.getSpec() == null ? other.getSpec() == null : this.getSpec().equals(other.getSpec()))
            && (this.getUnit() == null ? other.getUnit() == null : this.getUnit().equals(other.getUnit()))
            && (this.getCategory() == null ? other.getCategory() == null : this.getCategory().equals(other.getCategory()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getUpdatedTime() == null ? other.getUpdatedTime() == null : this.getUpdatedTime().equals(other.getUpdatedTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getProductCode() == null) ? 0 : getProductCode().hashCode());
        result = prime * result + ((getProductName() == null) ? 0 : getProductName().hashCode());
        result = prime * result + ((getSpec() == null) ? 0 : getSpec().hashCode());
        result = prime * result + ((getUnit() == null) ? 0 : getUnit().hashCode());
        result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
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
        sb.append(", productCode=").append(productCode);
        sb.append(", productName=").append(productName);
        sb.append(", spec=").append(spec);
        sb.append(", unit=").append(unit);
        sb.append(", category=").append(category);
        sb.append(", status=").append(status);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}