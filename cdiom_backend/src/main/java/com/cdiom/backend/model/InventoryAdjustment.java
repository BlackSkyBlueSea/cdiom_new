package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存调整记录实体类
 * 
 * @author cdiom
 */
@Data
@TableName("inventory_adjustment")
public class InventoryAdjustment {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 调整单号（唯一）
     */
    private String adjustmentNumber;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 调整类型：PROFIT-盘盈/LOSS-盘亏
     */
    private String adjustmentType;

    /**
     * 调整前数量
     */
    private Integer quantityBefore;

    /**
     * 调整后数量
     */
    private Integer quantityAfter;

    /**
     * 调整数量（调整后数量 - 调整前数量）
     */
    private Integer adjustmentQuantity;

    /**
     * 调整原因
     */
    private String adjustmentReason;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 第二操作人ID（特殊药品）
     */
    private Long secondOperatorId;

    /**
     * 盘点记录照片路径
     */
    private String adjustmentImage;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}



