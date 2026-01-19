package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商审批流程日志实体类
 * 详细记录每个审批环节的操作
 * 
 * @author cdiom
 */
@Data
@TableName("supplier_approval_log")
public class SupplierApprovalLog {

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
     * 审批环节名称：APPLY-申请/QUALITY_CHECK-资质核验/PRICE_REVIEW-价格审核/FINAL_APPROVE-最终审批
     */
    private String stepName;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作人角色
     */
    private String operatorRole;

    /**
     * 操作类型：SUBMIT-提交/APPROVE-通过/REJECT-驳回/REVOKE-撤回
     */
    private String operationType;

    /**
     * 操作结果：PASS-通过/FAIL-不通过
     */
    private String operationResult;

    /**
     * 操作意见
     */
    private String operationOpinion;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 操作时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operationTime;
}

