package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入库记录实体类
 * 
 * @author cdiom
 */
@Data
@TableName("inbound_record")
public class InboundRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 入库单号（唯一）
     */
    private String recordNumber;

    /**
     * 关联采购订单ID（可为空，支持临时入库）
     */
    private Long orderId;

    /**
     * 到货批次头ID（采购订单多批次入库；临时入库可为空）
     */
    private Long receiptBatchId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 入库数量
     */
    private Integer quantity;

    /**
     * 有效期至
     */
    private LocalDate expiryDate;

    /**
     * 到货日期
     */
    private LocalDate arrivalDate;

    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 入库指定存储位置（合格入库必填，入账时写入库存批次）
     */
    private String storageLocation;

    /**
     * 随货同行单编号
     */
    private String deliveryNoteNumber;

    /**
     * 随货同行单图片路径
     */
    private String deliveryNoteImage;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 第二操作人ID（特殊药品）
     */
    private Long secondOperatorId;

    /**
     * 验收状态：QUALIFIED-合格/UNQUALIFIED-不合格
     */
    private String status;

    /**
     * 第二人确认流程：NONE-不适用 / CONFIRMED-已入账 / PENDING_SECOND-待第二人确认 /
     * REJECTED-第二人驳回 / WITHDRAWN-第一人撤回 / TIMEOUT-超时关闭
     */
    private String secondConfirmStatus;

    /**
     * 第二人确认时间
     */
    private LocalDateTime secondConfirmTime;

    /**
     * 第二人确认截止时间（超时后标记 TIMEOUT）
     */
    private LocalDateTime secondConfirmDeadline;

    /**
     * 第二人驳回原因
     */
    private String secondRejectReason;

    /**
     * 效期校验状态：PASS-通过/WARNING-不足180天需确认/FORCE-强制入库
     */
    private String expiryCheckStatus;

    /**
     * 效期校验说明
     */
    private String expiryCheckReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 不合格处置意向代码（仅 status=UNQUALIFIED 时有意义），见 {@link com.cdiom.backend.inbound.InboundDispositionCodes}
     */
    private String dispositionCode;

    /**
     * 处置补充说明（可选）
     */
    private String dispositionRemark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 药品名称（仅用于展示，来自关联的 drug_info 表）
     */
    @TableField(exist = false)
    private String drugName;

    /**
     * 到货批次号（仅列表展示，来自 inbound_receipt_batch）
     */
    @TableField(exist = false)
    private String receiptBatchCode;

    /**
     * 第一操作人展示名（来自 sys_user.username）
     */
    @TableField(exist = false)
    private String operatorName;

    /**
     * 第二操作人展示名（特殊药品双人流程，来自 sys_user.username）
     */
    @TableField(exist = false)
    private String secondOperatorName;
}
