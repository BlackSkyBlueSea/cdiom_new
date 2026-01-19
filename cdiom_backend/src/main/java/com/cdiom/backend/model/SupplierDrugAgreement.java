package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商-药品价格协议实体类
 * 用于存储价格协议文件，确保价格与协议一致性
 * 
 * @author cdiom
 */
@Data
@TableName("supplier_drug_agreement")
public class SupplierDrugAgreement {

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
     * 协议编号
     */
    private String agreementNumber;

    /**
     * 协议名称
     */
    private String agreementName;

    /**
     * 协议文件URL（电子版或扫描件）
     */
    private String agreementFileUrl;

    /**
     * 协议类型：PRICE-价格协议/FRAMEWORK-框架协议
     */
    private String agreementType;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 到期日期
     */
    private LocalDate expiryDate;

    /**
     * 协议约定的单价
     */
    private BigDecimal unitPrice;

    /**
     * 货币单位：CNY-人民币
     */
    private String currency;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建人ID
     */
    private Long createBy;

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

