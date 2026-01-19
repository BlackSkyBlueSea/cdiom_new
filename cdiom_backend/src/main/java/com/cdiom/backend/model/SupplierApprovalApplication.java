package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商准入审批申请实体类
 * 实现分权制衡的审批流程
 * 
 * @author cdiom
 */
@Data
@TableName("supplier_approval_application")
public class SupplierApprovalApplication {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 供应商ID（新供应商为NULL，已有供应商修改协议时关联）
     */
    private Long supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 地址
     */
    private String address;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 许可证图片路径
     */
    private String licenseImage;

    /**
     * 许可证到期日期
     */
    private LocalDate licenseExpiryDate;

    /**
     * 申请类型：NEW-新供应商准入/MODIFY-协议修改
     */
    private String applicationType;

    /**
     * 审批状态：PENDING-待审核/QUALITY_CHECKED-资质已核验/PRICE_REVIEWED-价格已审核/APPROVED-已通过/REJECTED-已驳回
     */
    private String status;

    /**
     * 申请人ID（采购员）
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 申请时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime applyTime;

    /**
     * 资质核验人ID（仓库管理员）
     */
    private Long qualityCheckerId;

    /**
     * 资质核验人姓名
     */
    private String qualityCheckerName;

    /**
     * 资质核验时间
     */
    private LocalDateTime qualityCheckTime;

    /**
     * 资质核验结果：PASS-通过/FAIL-不通过
     */
    private String qualityCheckResult;

    /**
     * 资质核验意见
     */
    private String qualityCheckOpinion;

    /**
     * 价格审核人ID（采购/财务负责人）
     */
    private Long priceReviewerId;

    /**
     * 价格审核人姓名
     */
    private String priceReviewerName;

    /**
     * 价格审核时间
     */
    private LocalDateTime priceReviewTime;

    /**
     * 价格审核结果：PASS-通过/FAIL-不通过
     */
    private String priceReviewResult;

    /**
     * 价格审核意见
     */
    private String priceReviewOpinion;

    /**
     * 价格预警信息
     */
    private String priceWarning;

    /**
     * 最终审批人ID（超级管理员/合规岗）
     */
    private Long finalApproverId;

    /**
     * 最终审批人姓名
     */
    private String finalApproverName;

    /**
     * 最终审批时间
     */
    private LocalDateTime finalApproveTime;

    /**
     * 最终审批结果：APPROVED-通过/REJECTED-驳回
     */
    private String finalApproveResult;

    /**
     * 最终审批意见
     */
    private String finalApproveOpinion;

    /**
     * 驳回原因
     */
    private String rejectReason;

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

    /**
     * 逻辑删除：0-未删除/1-已删除
     */
    @TableLogic
    private Integer deleted;
}

