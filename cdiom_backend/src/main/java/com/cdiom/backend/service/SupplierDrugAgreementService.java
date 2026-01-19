package com.cdiom.backend.service;

import com.cdiom.backend.model.SupplierDrugAgreement;

import java.util.List;

/**
 * 供应商-药品价格协议服务接口
 * 
 * @author cdiom
 */
public interface SupplierDrugAgreementService {

    /**
     * 创建价格协议
     */
    SupplierDrugAgreement createAgreement(SupplierDrugAgreement agreement);

    /**
     * 根据ID查询协议
     */
    SupplierDrugAgreement getAgreementById(Long id);

    /**
     * 根据供应商ID和药品ID查询当前生效的协议
     */
    SupplierDrugAgreement getCurrentAgreement(Long supplierId, Long drugId);

    /**
     * 根据供应商ID和药品ID查询所有协议（包括历史）
     */
    List<SupplierDrugAgreement> getAgreementsBySupplierAndDrug(Long supplierId, Long drugId);

    /**
     * 更新协议
     */
    SupplierDrugAgreement updateAgreement(SupplierDrugAgreement agreement);

    /**
     * 删除协议（逻辑删除）
     */
    void deleteAgreement(Long id);
}

