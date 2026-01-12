package com.cdiom.backend.service;

import com.cdiom.backend.model.DrugInfo;

/**
 * 极速数据药品信息服务接口
 * 用于根据商品码、批准文号等查询药品详细信息
 * 
 * @author cdiom
 */
public interface JisuApiService {

    /**
     * 根据商品码查询药品信息
     * 
     * @param productCode 商品码
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchByProductCode(String productCode);

    /**
     * 根据批准文号查询药品信息
     * 
     * @param approvalNumber 批准文号
     * @return 药品信息，如果未找到返回null
     */
    DrugInfo searchByApprovalNumber(String approvalNumber);
}

