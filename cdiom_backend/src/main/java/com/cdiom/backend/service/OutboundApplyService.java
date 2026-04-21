package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.OutboundApply;
import com.cdiom.backend.model.OutboundApplyItem;
import com.cdiom.backend.model.SysUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 出库申请服务接口
 * 
 * @author cdiom
 */
public interface OutboundApplyService {

    /**
     * 分页查询出库申请列表
     */
    Page<OutboundApply> getOutboundApplyList(Integer page, Integer size, String keyword, Long applicantId, Long approverId, String department, String status, LocalDate startDate, LocalDate endDate);

    /**
     * 获取已有科室列表（出库申请中已使用过的科室，供新建申请时下拉选择）
     */
    List<String> listDepartments();

    /**
     * 申领出库时按药品查询可选批次（仅批次号、数量、效期；供无库存全量权限的医护人员使用）
     */
    List<Map<String, Object>> listDrugBatchesForApply(Long drugId);

    /**
     * 根据ID查询出库申请
     */
    OutboundApply getOutboundApplyById(Long id);

    /**
     * 根据申领单号查询出库申请
     */
    OutboundApply getOutboundApplyByApplyNumber(String applyNumber);

    /**
     * 创建出库申请
     */
    OutboundApply createOutboundApply(OutboundApply outboundApply, List<Map<String, Object>> items);

    /**
     * 仓库管理员代医护人员创建出库申请（记录 proxyRegistrarId 便于追溯）
     */
    OutboundApply createOutboundApplyOnBehalf(Long proxyRegistrarId, Long applicantUserId, String department,
            String purpose, String remark, List<Map<String, Object>> items);

    /**
     * 代录出库时可选择的医护人员列表（启用、角色为医护人员）
     */
    List<Map<String, Object>> listMedicalApplicantsForProxy();

    /**
     * 可作为第二审批人的用户（具备 outbound:approve 或 outbound:approve:special，含超级管理员）
     */
    List<SysUser> listOutboundSecondApproverCandidates();

    /**
     * 审批出库申请（通过）
     */
    void approveOutboundApply(Long id, Long approverId, Long secondApproverId);

    /**
     * 特殊药品第二审批：第一审批已完成（PENDING_SECOND）时，由第二审批人本人确认通过，状态变为 APPROVED。
     */
    void secondApproveOutboundApply(Long id, Long secondApproverUserId);

    /**
     * 审批出库申请（驳回）
     */
    void rejectOutboundApply(Long id, Long approverId, String rejectReason);

    /**
     * 执行出库
     */
    void executeOutbound(Long id, List<Map<String, Object>> outboundItems);

    /**
     * 取消出库申请
     */
    void cancelOutboundApply(Long id);

    /**
     * 申请人撤回出库申请（仅待审批状态下本人可撤回）
     */
    void withdrawOutboundApply(Long id, Long applicantUserId);

    /**
     * 查询待审批的出库申请数量
     */
    Long getPendingOutboundCount();

    /**
     * 查询今日出库数量
     */
    Long getTodayOutboundCount();

    /**
     * 获取出库申请明细列表
     */
    List<OutboundApplyItem> getOutboundApplyItems(Long applyId);

    /**
     * 校验出库申请所需库存是否充足（用于审批前在界面友好提示）
     * @return Map: sufficient(Boolean), message(String), details(List<Map: drugId, drugName, required, available, sufficient>)
     */
    Map<String, Object> checkStockForApply(Long applyId);

    /**
     * 出库拣货汇总：待执行（已通过审批）的申领单按批次与存储位置展开，便于打印现场拣货单。
     *
     * @param date  当 scope 为 approve_day 时，按审批通过日期筛选；为 null 时使用当天
     * @param scope approve_day：仅含该日审批通过的待执行单；all_pending：全部待执行单
     */
    Map<String, Object> getOutboundPickSummary(LocalDate date, String scope);
}

