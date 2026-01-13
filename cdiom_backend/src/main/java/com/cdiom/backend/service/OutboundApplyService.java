package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.OutboundApply;

import com.cdiom.backend.model.OutboundApplyItem;
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
     * 审批出库申请（通过）
     */
    void approveOutboundApply(Long id, Long approverId, Long secondApproverId);

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
}

