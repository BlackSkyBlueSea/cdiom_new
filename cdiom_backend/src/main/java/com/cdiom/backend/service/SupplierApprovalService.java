package com.cdiom.backend.service;

import com.cdiom.backend.model.SupplierApprovalApplication;
import com.cdiom.backend.model.SupplierApprovalItem;
import com.cdiom.backend.model.SupplierApprovalLog;

import java.util.List;

/**
 * 供应商准入审批服务接口
 * 
 * @author cdiom
 */
public interface SupplierApprovalService {

    /**
     * 创建审批申请（采购员发起）
     */
    SupplierApprovalApplication createApplication(SupplierApprovalApplication application, 
                                                   List<SupplierApprovalItem> items, 
                                                   Long applicantId, String applicantName);

    /**
     * 资质核验（仓库管理员）
     */
    void qualityCheck(Long applicationId, String result, String opinion, 
                     Long checkerId, String checkerName, String ipAddress);

    /**
     * 价格审核（采购/财务负责人）
     */
    void priceReview(Long applicationId, String result, String opinion, 
                    Long reviewerId, String reviewerName, String ipAddress);

    /**
     * 最终审批（超级管理员/合规岗）
     */
    void finalApprove(Long applicationId, String result, String opinion, 
                     Long approverId, String approverName, String ipAddress);

    /**
     * 驳回申请
     */
    void rejectApplication(Long applicationId, String reason, Long operatorId, 
                          String operatorName, String ipAddress);

    /**
     * 根据ID查询申请
     */
    SupplierApprovalApplication getApplicationById(Long id);

    /**
     * 查询审批日志
     */
    List<SupplierApprovalLog> getApprovalLogs(Long applicationId);

    /**
     * 检查供应商是否在黑名单
     */
    boolean isSupplierInBlacklist(String supplierName, String creditCode);
}

