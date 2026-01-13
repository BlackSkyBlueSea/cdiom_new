package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.InboundRecord;

import java.time.LocalDate;

/**
 * 入库记录服务接口
 * 
 * @author cdiom
 */
public interface InboundRecordService {

    /**
     * 分页查询入库记录列表
     */
    Page<InboundRecord> getInboundRecordList(Integer page, Integer size, String keyword, Long orderId, Long drugId, String batchNumber, Long operatorId, LocalDate startDate, LocalDate endDate, String status, String expiryCheckStatus);

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
     */
    InboundRecord createInboundRecordFromOrder(InboundRecord inboundRecord, Long orderId, Long drugId);

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
     * 查询订单某个药品的已入库数量
     */
    Integer getInboundQuantityByOrderAndDrug(Long orderId, Long drugId);

    /**
     * 查询今日入库数量
     */
    Long getTodayInboundCount();
}

