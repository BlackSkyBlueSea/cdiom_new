package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.PriceWarningConfigMapper;
import com.cdiom.backend.mapper.SupplierDrugPriceHistoryMapper;
import com.cdiom.backend.model.PriceWarningConfig;
import com.cdiom.backend.model.PriceWarningResult;
import com.cdiom.backend.model.SupplierDrugPriceHistory;
import com.cdiom.backend.service.PriceWarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 价格预警服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceWarningServiceImpl implements PriceWarningService {

    private final PriceWarningConfigMapper configMapper;
    private final SupplierDrugPriceHistoryMapper historyMapper;

    @Override
    public PriceWarningResult checkPrice(Long drugId, BigDecimal proposedPrice) {
        PriceWarningResult result = new PriceWarningResult();
        result.setWarningLevel("NORMAL");
        result.setNeedReason(false);
        
        // 获取参考价格
        BigDecimal referencePrice = getReferencePrice(drugId);
        result.setReferencePrice(referencePrice);
        
        if (referencePrice == null || referencePrice.compareTo(BigDecimal.ZERO) <= 0) {
            // 没有参考价格，不预警
            result.setWarningMessage("无参考价格，无法进行价格预警");
            return result;
        }
        
        // 计算价格差异率
        BigDecimal difference = proposedPrice.subtract(referencePrice);
        BigDecimal differenceRate = difference.divide(referencePrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        result.setPriceDifferenceRate(differenceRate);
        
        // 获取预警配置（优先药品配置，其次全局配置）
        PriceWarningConfig config = getWarningConfig(drugId);
        if (config == null) {
            result.setWarningMessage("未配置价格预警规则");
            return result;
        }
        
        BigDecimal warningThreshold = config.getWarningThreshold();
        BigDecimal criticalThreshold = config.getCriticalThreshold();
        
        // 判断预警级别
        BigDecimal absDifferenceRate = differenceRate.abs();
        if (absDifferenceRate.compareTo(criticalThreshold) >= 0) {
            result.setWarningLevel("CRITICAL");
            result.setWarningMessage(String.format("价格差异严重：申请价格 %.2f 元，参考价格 %.2f 元，差异率 %.2f%%，超过严重预警阈值 %.2f%%",
                    proposedPrice, referencePrice, absDifferenceRate, criticalThreshold));
            result.setNeedReason(true);
        } else if (absDifferenceRate.compareTo(warningThreshold) >= 0) {
            result.setWarningLevel("WARNING");
            result.setWarningMessage(String.format("价格差异警告：申请价格 %.2f 元，参考价格 %.2f 元，差异率 %.2f%%，超过预警阈值 %.2f%%",
                    proposedPrice, referencePrice, absDifferenceRate, warningThreshold));
            result.setNeedReason(true);
        } else {
            result.setWarningLevel("NORMAL");
            result.setWarningMessage("价格正常");
        }
        
        return result;
    }

    @Override
    public BigDecimal getReferencePrice(Long drugId) {
        // 优先获取药品特定的配置
        PriceWarningConfig drugConfig = getWarningConfig(drugId);
        if (drugConfig != null && drugConfig.getReferencePrice() != null) {
            return drugConfig.getReferencePrice();
        }
        
        // 获取全局配置
        PriceWarningConfig globalConfig = getWarningConfig(null);
        if (globalConfig != null && globalConfig.getReferencePrice() != null) {
            return globalConfig.getReferencePrice();
        }
        
        // 如果没有配置参考价格，尝试从历史价格中获取最低价
        LambdaQueryWrapper<SupplierDrugPriceHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrugPriceHistory::getDrugId, drugId)
               .isNotNull(SupplierDrugPriceHistory::getPriceAfter)
               .orderByAsc(SupplierDrugPriceHistory::getPriceAfter)
               .last("LIMIT 1");
        SupplierDrugPriceHistory history = historyMapper.selectOne(wrapper);
        if (history != null) {
            return history.getPriceAfter();
        }
        
        return null;
    }

    /**
     * 获取预警配置（优先药品配置，其次全局配置）
     */
    private PriceWarningConfig getWarningConfig(Long drugId) {
        // 先查药品特定配置
        if (drugId != null) {
            LambdaQueryWrapper<PriceWarningConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PriceWarningConfig::getDrugId, drugId)
                   .eq(PriceWarningConfig::getIsActive, 1)
                   .eq(PriceWarningConfig::getDeleted, 0)
                   .orderByDesc(PriceWarningConfig::getCreateTime)
                   .last("LIMIT 1");
            PriceWarningConfig config = configMapper.selectOne(wrapper);
            if (config != null) {
                return config;
            }
        }
        
        // 查全局配置
        LambdaQueryWrapper<PriceWarningConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(PriceWarningConfig::getDrugId)
               .eq(PriceWarningConfig::getIsActive, 1)
               .eq(PriceWarningConfig::getDeleted, 0)
               .orderByDesc(PriceWarningConfig::getCreateTime)
               .last("LIMIT 1");
        return configMapper.selectOne(wrapper);
    }
}

