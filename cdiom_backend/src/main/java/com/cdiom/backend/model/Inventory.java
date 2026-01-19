package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存实体类
 * 
 * @author cdiom
 */
@Data
@TableName("inventory")
public class Inventory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 库存数量
     */
    private Integer quantity;

    /**
     * 有效期至
     */
    private LocalDate expiryDate;

    /**
     * 存储位置
     */
    private String storageLocation;

    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 生产厂家
     */
    private String manufacturer;

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




