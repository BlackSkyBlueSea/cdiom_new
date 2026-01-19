package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商准入审批明细实体类（协议价格明细）
 * 
 * @author cdiom
 */
@Data
@TableName("supplier_approval_item")
public class SupplierApprovalItem {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 审批申请ID
     */
    private Long applicationId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 药品名称（冗余字段，便于查询）
     */
    private String drugName;

    /**
     * 申请价格
     */
    private BigDecimal proposedPrice;

    /**
     * 参考价格（集采价/市场价/历史最低价）
     */
    private BigDecimal referencePrice;

    /**
     * 价格差异率（(申请价-参考价)/参考价*100）
     */
    private BigDecimal priceDifferenceRate;

    /**
     * 价格预警级别：NORMAL-正常/WARNING-警告/CRITICAL-严重
     */
    private String priceWarningLevel;

    /**
     * 价格差异说明
     */
    private String priceDifferenceReason;

    /**
     * 协议文件URL
     */
    private String agreementFileUrl;

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

