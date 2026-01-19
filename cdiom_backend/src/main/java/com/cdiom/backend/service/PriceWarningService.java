package com.cdiom.backend.service;

import com.cdiom.backend.model.PriceWarningResult;

import java.math.BigDecimal;

/**
 * 价格预警服务接口
 * 
 * @author cdiom
 */
public interface PriceWarningService {

    /**
     * 检查价格并返回预警结果
     * 
     * @param drugId 药品ID
     * @param proposedPrice 申请价格
     * @return 价格预警结果
     */
    PriceWarningResult checkPrice(Long drugId, BigDecimal proposedPrice);

    /**
     * 获取参考价格
     * 
     * @param drugId 药品ID
     * @return 参考价格
     */
    BigDecimal getReferencePrice(Long drugId);
}

