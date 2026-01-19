package com.cdiom.backend.service;

import com.cdiom.backend.model.SupplierDrugPriceHistory;

import java.util.List;

/**
 * 供应商-药品价格历史记录服务接口
 * 
 * @author cdiom
 */
public interface SupplierDrugPriceHistoryService {

    /**
     * 记录价格变更历史
     */
    void recordPriceChange(SupplierDrugPriceHistory history);

    /**
     * 根据供应商ID和药品ID查询价格历史
     */
    List<SupplierDrugPriceHistory> getPriceHistory(Long supplierId, Long drugId);

    /**
     * 根据协议ID查询价格历史
     */
    List<SupplierDrugPriceHistory> getPriceHistoryByAgreement(Long agreementId);
}

