package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.Supplier;

/**
 * 供应商服务接口
 * 
 * @author cdiom
 */
public interface SupplierService {

    /**
     * 分页查询供应商列表
     */
    Page<Supplier> getSupplierList(Integer page, Integer size, String keyword, Integer status, Integer auditStatus);

    /**
     * 根据ID查询供应商
     */
    Supplier getSupplierById(Long id);

    /**
     * 创建供应商
     */
    Supplier createSupplier(Supplier supplier);

    /**
     * 更新供应商
     */
    Supplier updateSupplier(Supplier supplier);

    /**
     * 删除供应商（逻辑删除）
     */
    void deleteSupplier(Long id);

    /**
     * 更新供应商状态
     */
    void updateSupplierStatus(Long id, Integer status);

    /**
     * 审核供应商
     */
    void auditSupplier(Long id, Integer auditStatus, String auditReason, Long auditBy);
}




