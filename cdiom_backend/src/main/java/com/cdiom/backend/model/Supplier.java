package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商实体类
 * 
 * @author cdiom
 */
@Data
@TableName("supplier")
public class Supplier {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 供应商名称
     */
    private String name;

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
     * 状态：0-禁用/1-启用/2-待审核
     */
    private Integer status;

    /**
     * 审核状态：0-待审核/1-已通过/2-已驳回
     */
    private Integer auditStatus;

    /**
     * 审核理由
     */
    private String auditReason;

    /**
     * 审核人ID
     */
    private Long auditBy;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

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

