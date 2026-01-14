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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}



