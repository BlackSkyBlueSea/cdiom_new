package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.InventoryAdjustment;

import java.time.LocalDate;

/**
 * 库存调整服务接口
 * 
 * @author cdiom
 */
public interface InventoryAdjustmentService {

    /**
     * 分页查询库存调整记录列表
     */
    Page<InventoryAdjustment> getInventoryAdjustmentList(Integer page, Integer size, String keyword, Long drugId, String batchNumber, String adjustmentType, Long operatorId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据ID查询库存调整记录
     */
    InventoryAdjustment getInventoryAdjustmentById(Long id);

    /**
     * 创建库存调整记录（盘盈/盘亏）
     */
    InventoryAdjustment createInventoryAdjustment(InventoryAdjustment adjustment, Long secondOperatorId);
}



