package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.SupplierDrugPriceHistoryMapper;
import com.cdiom.backend.model.SupplierDrugPriceHistory;
import com.cdiom.backend.service.SupplierDrugPriceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商-药品价格历史记录服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierDrugPriceHistoryServiceImpl implements SupplierDrugPriceHistoryService {

    private final SupplierDrugPriceHistoryMapper historyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPriceChange(SupplierDrugPriceHistory history) {
        historyMapper.insert(history);
        log.info("记录价格变更历史: supplierId={}, drugId={}, priceBefore={}, priceAfter={}", 
                history.getSupplierId(), history.getDrugId(), 
                history.getPriceBefore(), history.getPriceAfter());
    }

    @Override
    public List<SupplierDrugPriceHistory> getPriceHistory(Long supplierId, Long drugId) {
        LambdaQueryWrapper<SupplierDrugPriceHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrugPriceHistory::getSupplierId, supplierId)
               .eq(SupplierDrugPriceHistory::getDrugId, drugId)
               .orderByDesc(SupplierDrugPriceHistory::getOperationTime);
        return historyMapper.selectList(wrapper);
    }

    @Override
    public List<SupplierDrugPriceHistory> getPriceHistoryByAgreement(Long agreementId) {
        LambdaQueryWrapper<SupplierDrugPriceHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrugPriceHistory::getAgreementId, agreementId)
               .orderByDesc(SupplierDrugPriceHistory::getOperationTime);
        return historyMapper.selectList(wrapper);
    }
}

