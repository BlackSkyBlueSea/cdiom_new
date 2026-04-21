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
     * 代录人ID（仓库管理员现场代录时；自助申领为 null）
     */
    private Long proxyRegistrarId;

    /**
     * 代录人姓名（非表字段）
     */
    @TableField(exist = false)
    private String proxyRegistrarName;

    /**
     * 代录人角色名称（非表字段）
     */
    @TableField(exist = false)
    private String proxyRegistrarRoleName;

    /**
     * 申请人姓名（非表字段，列表/详情展示用，来自 sys_user.username）
     */
    @TableField(exist = false)
    private String applicantName;

    /**
     * 申请人角色名称（非表字段，列表/详情展示用，来自 sys_role.role_name，便于区分同名或易混用户）
     */
    @TableField(exist = false)
    private String applicantRoleName;

    /**
     * 所属科室
     */
    private String department;

    /**
     * 用途
     */
    private String purpose;

    /**
     * 申请状态：PENDING-待审批/PENDING_SECOND-待第二审批（特殊药品）/APPROVED-已通过/REJECTED-已驳回/OUTBOUND-已出库/CANCELLED-已取消
     */
    private String status;

    /**
     * 审批人ID（仓库管理员）
     */
    private Long approverId;

    /**
     * 审批人姓名（非表字段，列表/详情展示用，来自 sys_user.username）
     */
    @TableField(exist = false)
    private String approverName;

    /**
     * 审批人角色名称（非表字段，列表/详情展示用，来自 sys_role.role_name）
     */
    @TableField(exist = false)
    private String approverRoleName;

    /**
     * 第二审批人ID（特殊药品）
     */
    private Long secondApproverId;

    /**
     * 第二审批人姓名（非表字段）
     */
    @TableField(exist = false)
    private String secondApproverName;

    /**
     * 第二审批人角色名称（非表字段）
     */
    @TableField(exist = false)
    private String secondApproverRoleName;

    /**
     * 第一审批通过时间（特殊药品：进入待第二审批时写入；终批见 approveTime）
     */
    private LocalDateTime firstApproveTime;

    /**
     * 终批通过时间（非特殊药品一次审批即写入；特殊药品在第二人确认后写入）
     */
    private LocalDateTime approveTime;

    /**
     * 驳回理由
     */
    private String rejectReason;

    /**
     * 驳回操作人（待第二审批状态下由第二人驳回时记录；普通待审批驳回时可为空，以 approverId 为准）
     */
    private Long rejectOperatorId;

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








