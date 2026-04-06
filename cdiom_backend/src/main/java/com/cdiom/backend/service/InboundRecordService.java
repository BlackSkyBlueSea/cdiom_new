package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.InboundReceiptBatch;
import com.cdiom.backend.model.InboundRecord;
import com.cdiom.backend.model.vo.InboundSplitResult;
import com.cdiom.backend.model.vo.OrderInboundRemainingRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 入库记录服务接口
 * 
 * @author cdiom
 */
public interface InboundRecordService {

    /**
     * 分页查询入库记录列表
     */
    Page<InboundRecord> getInboundRecordList(Integer page, Integer size, String keyword, Long orderId, Long drugId, String batchNumber, Long operatorId, LocalDate startDate, LocalDate endDate, String status, String expiryCheckStatus, String secondConfirmStatus, Long secondOperatorId);

    /**
     * 根据ID查询入库记录
     */
    InboundRecord getInboundRecordById(Long id);

    /**
     * 根据入库单号查询入库记录
     */
    InboundRecord getInboundRecordByRecordNumber(String recordNumber);

    /**
     * 创建入库记录（采购订单入库）
     *
     * @param receiptBatchId 已登记的到货批次头；为空则按随货单等信息新建一批次头
     * @param batchArrivalAt   新建批次时的到货时间；可为空则取入库行到货日或当前时间
     */
    InboundRecord createInboundRecordFromOrder(InboundRecord inboundRecord, Long orderId, Long drugId,
                                               Long receiptBatchId, LocalDateTime batchArrivalAt);

    /**
     * 同一订单、同一到货批次、同一批号：一次提交「合格 + 不合格」两条入库明细（同一追溯码，事务）
     */
    InboundSplitResult createInboundSplitFromOrder(InboundRecord base, Long orderId, Long drugId,
            int qualifiedQty, int unqualifiedQty, String unqualifiedReason,
            String unqualifiedDispositionCode, String unqualifiedDispositionRemark,
            Long receiptBatchId, LocalDateTime batchArrivalAt);

    /**
     * 不合格处置意向下拉选项（供前端与后续扩展）
     */
    Map<String, String> listDispositionOptions();

    /**
     * 仅登记到货批次头（可选：先建批次再逐行入库）
     */
    InboundReceiptBatch createReceiptBatchHeader(Long orderId, String deliveryNoteNumber,
                                                 LocalDateTime arrivalAt, Long operatorId,
                                                 String remark, String deliveryNoteImage);

    /**
     * 创建入库记录（临时入库）
     */
    InboundRecord createInboundRecordTemporary(InboundRecord inboundRecord, Long drugId);

    /**
     * 效期校验
     * 返回：PASS-通过/WARNING-不足180天需确认/FORCE-强制入库
     */
    String checkExpiryDate(LocalDate expiryDate);

    /**
     * 查询订单某个药品的已确认入账数量（计入库存、用于判断订单是否全部到货）
     */
    Integer getInboundQuantityByOrderAndDrug(Long orderId, Long drugId);

    /**
     * 查询订单某个药品已从订单占用的到货数量：合格（已入账或待第二人确认）+ 不合格登记（用于不超量校验、剩余可入）
     */
    Integer getInboundCommittedQuantityByOrderAndDrug(Long orderId, Long drugId);

    /**
     * 订单下各药品剩余可入库汇总（动态展示用）
     */
    List<OrderInboundRemainingRow> listOrderInboundRemaining(Long orderId);

    /**
     * 查询今日入库数量
     */
    Long getTodayInboundCount();

    /**
     * 第二操作人确认（特殊药品待确认入库入账）
     */
    InboundRecord secondConfirmInbound(Long id, Long currentUserId);

    /**
     * 第二操作人驳回
     */
    InboundRecord secondRejectInbound(Long id, Long currentUserId, String reason);

    /**
     * 第一操作人撤回待第二人确认的入库单
     */
    InboundRecord withdrawPendingSecondInbound(Long id, Long currentUserId);

    /**
     * 超时关闭待第二人确认的入库单（定时任务调用）
     */
    int timeoutPendingSecondInbound();
}

