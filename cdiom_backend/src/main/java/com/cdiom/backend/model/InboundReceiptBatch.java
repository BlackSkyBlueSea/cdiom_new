package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购入库到货批次头（随货单、到货时间；多行入库明细可共用同一批次）
 */
@Data
@TableName("inbound_receipt_batch")
public class InboundReceiptBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchCode;

    private Long orderId;

    private String deliveryNoteNumber;

    private LocalDateTime arrivalTime;

    private String deliveryNoteImage;

    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
