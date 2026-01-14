package com.cdiom.backend.service;

import com.cdiom.backend.model.SupplierDrug;

import java.util.List;

/**
 * 供应商-药品关联服务接口
 * 
 * @author cdiom
 */
public interface SupplierDrugService {

    /**
     * 根据供应商ID查询该供应商提供的药品ID列表
     */
    List<Long> getDrugIdsBySupplierId(Long supplierId);

    /**
     * 根据药品ID查询提供该药品的供应商ID列表
     */
    List<Long> getSupplierIdsByDrugId(Long drugId);

    /**
     * 添加供应商-药品关联
     */
    SupplierDrug addSupplierDrug(Long supplierId, Long drugId, java.math.BigDecimal unitPrice, Long createBy);

    /**
     * 删除供应商-药品关联
     */
    void removeSupplierDrug(Long supplierId, Long drugId);

    /**
     * 更新供应商-药品关联的单价
     */
    SupplierDrug updateSupplierDrugPrice(Long supplierId, Long drugId, java.math.BigDecimal unitPrice);

    /**
     * 根据供应商ID和药品ID查询关联信息
     */
    SupplierDrug getSupplierDrug(Long supplierId, Long drugId);

    /**
     * 批量添加供应商-药品关联
     */
    void batchAddSupplierDrugs(Long supplierId, List<Long> drugIds, Long createBy);
}



