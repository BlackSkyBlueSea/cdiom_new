package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.*;
import com.cdiom.backend.model.*;
import com.cdiom.backend.service.PriceWarningService;
import com.cdiom.backend.service.SupplierApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商准入审批服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierApprovalServiceImpl implements SupplierApprovalService {

    private final SupplierApprovalApplicationMapper applicationMapper;
    private final SupplierApprovalItemMapper itemMapper;
    private final SupplierApprovalLogMapper logMapper;
    private final SupplierBlacklistMapper blacklistMapper;
    private final PriceWarningService priceWarningService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierApprovalApplication createApplication(SupplierApprovalApplication application, 
                                                          List<SupplierApprovalItem> items, 
                                                          Long applicantId, String applicantName) {
        // 检查黑名单
        if (isSupplierInBlacklist(application.getSupplierName(), application.getCreditCode())) {
            throw new RuntimeException("该供应商已被列入黑名单，无法创建申请");
        }

        // 设置申请人信息
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setStatus("PENDING");
        application.setApplyTime(LocalDateTime.now());

        // 保存申请
        applicationMapper.insert(application);

        // 保存明细并检查价格预警
        StringBuilder priceWarnings = new StringBuilder();
        for (SupplierApprovalItem item : items) {
            item.setApplicationId(application.getId());
            
            // 价格预警检查
            PriceWarningResult warningResult = priceWarningService.checkPrice(
                    item.getDrugId(), item.getProposedPrice());
            item.setReferencePrice(warningResult.getReferencePrice());
            item.setPriceDifferenceRate(warningResult.getPriceDifferenceRate());
            item.setPriceWarningLevel(warningResult.getWarningLevel());
            
            if (!"NORMAL".equals(warningResult.getWarningLevel())) {
                priceWarnings.append(warningResult.getWarningMessage()).append("; ");
            }
            
            itemMapper.insert(item);
        }

        // 如果有价格预警，记录到申请中
        if (priceWarnings.length() > 0) {
            application.setPriceWarning(priceWarnings.toString());
            applicationMapper.updateById(application);
        }

        // 记录操作日志
        recordLog(application.getId(), "APPLY", "SUBMIT", "PASS", 
                "提交供应商准入申请", applicantId, applicantName, null, null);

        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void qualityCheck(Long applicationId, String result, String opinion, 
                            Long checkerId, String checkerName, String ipAddress) {
        SupplierApprovalApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new RuntimeException("当前状态不允许进行资质核验");
        }

        application.setQualityCheckerId(checkerId);
        application.setQualityCheckerName(checkerName);
        application.setQualityCheckTime(LocalDateTime.now());
        application.setQualityCheckResult(result);
        application.setQualityCheckOpinion(opinion);
        application.setStatus("QUALITY_CHECKED");

        applicationMapper.updateById(application);

        // 记录操作日志
        recordLog(applicationId, "QUALITY_CHECK", "PASS".equals(result) ? "APPROVE" : "REJECT", 
                result, opinion, checkerId, checkerName, "仓库管理员", ipAddress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void priceReview(Long applicationId, String result, String opinion, 
                           Long reviewerId, String reviewerName, String ipAddress) {
        SupplierApprovalApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!"QUALITY_CHECKED".equals(application.getStatus()) || 
            !"PASS".equals(application.getQualityCheckResult())) {
            throw new RuntimeException("当前状态不允许进行价格审核");
        }

        application.setPriceReviewerId(reviewerId);
        application.setPriceReviewerName(reviewerName);
        application.setPriceReviewTime(LocalDateTime.now());
        application.setPriceReviewResult(result);
        application.setPriceReviewOpinion(opinion);
        application.setStatus("PRICE_REVIEWED");

        applicationMapper.updateById(application);

        // 记录操作日志
        recordLog(applicationId, "PRICE_REVIEW", "PASS".equals(result) ? "APPROVE" : "REJECT", 
                result, opinion, reviewerId, reviewerName, "采购/财务负责人", ipAddress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalApprove(Long applicationId, String result, String opinion, 
                            Long approverId, String approverName, String ipAddress) {
        SupplierApprovalApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!"PRICE_REVIEWED".equals(application.getStatus()) || 
            !"PASS".equals(application.getPriceReviewResult())) {
            throw new RuntimeException("当前状态不允许进行最终审批");
        }

        application.setFinalApproverId(approverId);
        application.setFinalApproverName(approverName);
        application.setFinalApproveTime(LocalDateTime.now());
        application.setFinalApproveResult(result);
        application.setFinalApproveOpinion(opinion);
        application.setStatus("APPROVED".equals(result) ? "APPROVED" : "REJECTED");
        if ("REJECTED".equals(result)) {
            application.setRejectReason(opinion);
        }

        applicationMapper.updateById(application);

        // 记录操作日志
        recordLog(applicationId, "FINAL_APPROVE", "APPROVED".equals(result) ? "APPROVE" : "REJECT", 
                result, opinion, approverId, approverName, "超级管理员/合规岗", ipAddress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectApplication(Long applicationId, String reason, Long operatorId, 
                                 String operatorName, String ipAddress) {
        SupplierApprovalApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        application.setStatus("REJECTED");
        application.setRejectReason(reason);
        applicationMapper.updateById(application);

        // 记录操作日志
        recordLog(applicationId, "REJECT", "REJECT", "FAIL", reason, 
                operatorId, operatorName, null, ipAddress);
    }

    @Override
    public SupplierApprovalApplication getApplicationById(Long id) {
        return applicationMapper.selectById(id);
    }

    @Override
    public List<SupplierApprovalLog> getApprovalLogs(Long applicationId) {
        LambdaQueryWrapper<SupplierApprovalLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierApprovalLog::getApplicationId, applicationId)
               .orderByAsc(SupplierApprovalLog::getOperationTime);
        return logMapper.selectList(wrapper);
    }

    @Override
    public boolean isSupplierInBlacklist(String supplierName, String creditCode) {
        LambdaQueryWrapper<SupplierBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierBlacklist::getDeleted, 0)
               .and(w -> w.eq(SupplierBlacklist::getSupplierName, supplierName)
                          .or()
                          .eq(SupplierBlacklist::getCreditCode, creditCode))
               .and(w -> w.isNull(SupplierBlacklist::getExpiryDate)
                          .or()
                          .ge(SupplierBlacklist::getExpiryDate, java.time.LocalDate.now()))
               .and(w -> w.isNull(SupplierBlacklist::getEffectiveDate)
                          .or()
                          .le(SupplierBlacklist::getEffectiveDate, java.time.LocalDate.now()));
        
        return blacklistMapper.selectCount(wrapper) > 0;
    }

    /**
     * 记录审批日志
     */
    private void recordLog(Long applicationId, String stepName, String operationType, 
                          String operationResult, String opinion, Long operatorId, 
                          String operatorName, String operatorRole, String ipAddress) {
        SupplierApprovalLog log = new SupplierApprovalLog();
        log.setApplicationId(applicationId);
        log.setStepName(stepName);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorRole(operatorRole);
        log.setOperationType(operationType);
        log.setOperationResult(operationResult);
        log.setOperationOpinion(opinion);
        log.setIpAddress(ipAddress);
        log.setOperationTime(LocalDateTime.now());
        logMapper.insert(log);
    }
}

