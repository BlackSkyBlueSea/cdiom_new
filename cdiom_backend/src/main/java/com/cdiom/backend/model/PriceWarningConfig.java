package com.cdiom.backend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格预警配置实体类
 * 
 * @author cdiom
 */
@Data
@TableName("price_warning_config")
public class PriceWarningConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 药品ID（NULL表示全局配置）
     */
    private Long drugId;

    /**
     * 药品名称（冗余字段）
     */
    private String drugName;

    /**
     * 参考价格类型：MARKET-市场价/COLLECTIVE-集采价/HISTORY_MIN-历史最低价
     */
    private String referencePriceType;

    /**
     * 参考价格
     */
    private BigDecimal referencePrice;

    /**
     * 预警阈值（百分比，超过此值触发预警）
     */
    private BigDecimal warningThreshold;

    /**
     * 严重预警阈值（百分比，超过此值触发严重预警）
     */
    private BigDecimal criticalThreshold;

    /**
     * 是否启用：0-禁用/1-启用
     */
    private Integer isActive;

    /**
     * 创建人ID
     */
    private Long creatorId;

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

