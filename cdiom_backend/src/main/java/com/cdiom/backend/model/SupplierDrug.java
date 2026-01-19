package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商-药品关联实体类
 * 用于支持供应商和药品的多对多关系
 * 
 * @author cdiom
 */
@Data
@TableName("supplier_drug")
public class SupplierDrug {

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
     * 该供应商提供该药品的单价
     */
    private BigDecimal unitPrice;

    /**
     * 当前生效的协议ID
     */
    private Long currentAgreementId;

    /**
     * 是否启用：0-禁用/1-启用
     */
    private Integer isActive;

    /**
     * 备注
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




