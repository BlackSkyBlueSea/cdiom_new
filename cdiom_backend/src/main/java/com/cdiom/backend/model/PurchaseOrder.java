package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单实体类
 * 
 * @author cdiom
 */
@Data
@TableName("purchase_order")
public class PurchaseOrder {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号（唯一）
     */
    private String orderNumber;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 采购员ID
     */
    private Long purchaserId;

    /**
     * 订单状态：PENDING-待确认/REJECTED-已拒绝/CONFIRMED-待发货/SHIPPED-已发货/RECEIVED-已入库/CANCELLED-已取消
     */
    private String status;

    /**
     * 预计交货日期
     */
    private LocalDate expectedDeliveryDate;

    /**
     * 物流单号
     */
    private String logisticsNumber;

    /**
     * 发货日期
     */
    private LocalDateTime shipDate;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 拒绝理由
     */
    private String rejectReason;

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

