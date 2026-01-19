package com.cdiom.backend.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格预警结果
 * 
 * @author cdiom
 */
@Data
public class PriceWarningResult {
    
    /**
     * 预警级别：NORMAL-正常/WARNING-警告/CRITICAL-严重
     */
    private String warningLevel;
    
    /**
     * 参考价格
     */
    private BigDecimal referencePrice;
    
    /**
     * 价格差异率（百分比）
     */
    private BigDecimal priceDifferenceRate;
    
    /**
     * 预警信息
     */
    private String warningMessage;
    
    /**
     * 是否需要填写差异说明
     */
    private Boolean needReason;
}

