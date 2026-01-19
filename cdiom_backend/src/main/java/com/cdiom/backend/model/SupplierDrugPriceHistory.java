package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商-药品价格历史记录实体类
 * 用于记录每次价格变更的完整历史
 * 
 * @author cdiom
 */
@Data
@TableName("supplier_drug_price_history")
public class SupplierDrugPriceHistory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 关联的协议ID
     */
    private Long agreementId;

    /**
     * 变更前价格
     */
    private BigDecimal priceBefore;

    /**
     * 变更后价格
     */
    private BigDecimal priceAfter;

    /**
     * 变更原因
     */
    private String changeReason;

    /**
     * 变更类型：MANUAL-手动修改/AGREEMENT-协议更新/AUTO-自动调整
     */
    private String changeType;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operationTime;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 备注
     */
    private String remark;
}

