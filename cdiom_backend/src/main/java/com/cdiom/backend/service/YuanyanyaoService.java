package com.cdiom.backend.service;

import com.cdiom.backend.model.DrugInfo;

/**
 * 万维易源药品信息服务接口
 * 用于查询第三方药品信息数据库
 * 
 * @author cdiom
 */
public interface YuanyanyaoService {

    /**
     * 根据批准文号查询药品信息
     * 
     * @param approvalNumber 批准文号
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchByApprovalNumber(String approvalNumber);

    /**
     * 根据药品名称查询药品信息
     * 
     * @param drugName 药品名称
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchByDrugName(String drugName);

    /**
     * 根据商品码或本位码查询药品信息
     * 优先通过批准文号匹配
     * 
     * @param code 商品码或本位码
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchByCode(String code);
}

