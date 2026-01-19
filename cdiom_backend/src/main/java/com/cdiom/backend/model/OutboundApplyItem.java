package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库申请明细实体类
 * 
 * @author cdiom
 */
@Data
@TableName("outbound_apply_item")
public class OutboundApplyItem {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请ID
     */
    private Long applyId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 批次号（可选，不指定则按FIFO）
     */
    private String batchNumber;

    /**
     * 申领数量
     */
    private Integer quantity;

    /**
     * 实际出库数量（出库时填写）
     */
    private Integer actualQuantity;

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




