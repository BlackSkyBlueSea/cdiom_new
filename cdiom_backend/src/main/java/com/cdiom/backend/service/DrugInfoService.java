package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.DrugInfo;

/**
 * 药品信息服务接口
 * 
 * @author cdiom
 */
public interface DrugInfoService {

    /**
     * 分页查询药品信息列表
     */
    Page<DrugInfo> getDrugInfoList(Integer page, Integer size, String keyword, Integer isSpecial);

    /**
     * 根据ID查询药品信息
     */
    DrugInfo getDrugInfoById(Long id);

    /**
     * 创建药品信息
     */
    DrugInfo createDrugInfo(DrugInfo drugInfo);

    /**
     * 更新药品信息
     */
    DrugInfo updateDrugInfo(DrugInfo drugInfo);

    /**
     * 删除药品信息
     */
    void deleteDrugInfo(Long id);

    /**
     * 根据商品码或本位码查询药品信息
     * 先查询本地数据库，如果未找到则查询极速数据API
     * 
     * @param code 商品码或本位码
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchDrugByCode(String code);

    /**
     * 根据药品名称查询药品信息（调用万维易源API）
     * 
     * @param drugName 药品名称
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchDrugByName(String drugName);

    /**
     * 根据批准文号查询药品信息（调用万维易源API）
     * 
     * @param approvalNumber 批准文号
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchDrugByApprovalNumber(String approvalNumber);

    /**
     * 根据供应商ID查询该供应商提供的药品列表
     * 
     * @param supplierId 供应商ID
     * @param page 页码
     * @param size 每页大小
     * @param keyword 关键词（可选）
     * @return 药品信息分页列表
     */
    Page<DrugInfo> getDrugInfoListBySupplierId(Long supplierId, Integer page, Integer size, String keyword);
}


