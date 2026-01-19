package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商黑名单实体类
 * 
 * @author cdiom
 */
@Data
@TableName("supplier_blacklist")
public class SupplierBlacklist {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 供应商ID（可为NULL，支持按名称黑名单）
     */
    private Long supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 列入黑名单原因
     */
    private String blacklistReason;

    /**
     * 黑名单类型：FULL-完全禁止/PARTIAL-部分药品禁止
     */
    private String blacklistType;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 到期日期（NULL表示永久有效）
     */
    private LocalDate expiryDate;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人姓名
     */
    private String creatorName;

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

