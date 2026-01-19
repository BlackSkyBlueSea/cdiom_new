package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库申请实体类
 * 
 * @author cdiom
 */
@Data
@TableName("outbound_apply")
public class OutboundApply {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申领单号（唯一）
     */
    private String applyNumber;

    /**
     * 申请人ID（医护人员）
     */
    private Long applicantId;

    /**
     * 所属科室
     */
    private String department;

    /**
     * 用途
     */
    private String purpose;

    /**
     * 申请状态：PENDING-待审批/APPROVED-已通过/REJECTED-已驳回/OUTBOUND-已出库/CANCELLED-已取消
     */
    private String status;

    /**
     * 审批人ID（仓库管理员）
     */
    private Long approverId;

    /**
     * 第二审批人ID（特殊药品）
     */
    private Long secondApproverId;

    /**
     * 审批时间
     */
    private LocalDateTime approveTime;

    /**
     * 驳回理由
     */
    private String rejectReason;

    /**
     * 出库时间
     */
    private LocalDateTime outboundTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}




