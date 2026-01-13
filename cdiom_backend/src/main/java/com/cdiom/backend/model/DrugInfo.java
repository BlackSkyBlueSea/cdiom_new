package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 药品信息实体类
 * 
 * @author cdiom
 */
@Data
@TableName("drug_info")
public class DrugInfo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 国家本位码
     */
    private String nationalCode;

    /**
     * 药品追溯码
     */
    private String traceCode;

    /**
     * 商品码
     */
    private String productCode;

    /**
     * 通用名称
     */
    private String drugName;

    /**
     * 剂型
     */
    private String dosageForm;

    /**
     * 规格
     */
    private String specification;

    /**
     * 批准文号
     */
    private String approvalNumber;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 有效期
     */
    private LocalDate expiryDate;

    /**
     * 是否特殊药品：0-普通药品/1-特殊药品
     */
    private Integer isSpecial;

    /**
     * 存储要求
     */
    private String storageRequirement;

    /**
     * 存储位置
     */
    private String storageLocation;

    /**
     * 单位
     */
    private String unit;

    /**
     * 描述
     */
    private String description;

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





