package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.Inventory;

import java.time.LocalDate;
import java.util.Map;

/**
 * 库存服务接口
 * 
 * @author cdiom
 */
public interface InventoryService {

    /**
     * 分页查询库存列表
     */
    Page<Inventory> getInventoryList(Integer page, Integer size, String keyword, Long drugId, String batchNumber, String storageLocation, LocalDate expiryDateStart, LocalDate expiryDateEnd, Integer isSpecial);

    /**
     * 根据ID查询库存信息
     */
    Inventory getInventoryById(Long id);

    /**
     * 根据药品ID和批次号查询库存
     */
    Inventory getInventoryByDrugAndBatch(Long drugId, String batchNumber);

    /**
     * 增加库存（入库时调用）
     */
    void increaseInventory(Long drugId, String batchNumber, Integer quantity, LocalDate expiryDate, String storageLocation, LocalDate productionDate, String manufacturer);

    /**
     * 减少库存（出库时调用）
     */
    void decreaseInventory(Long drugId, String batchNumber, Integer quantity);

    /**
     * 更新库存数量（库存调整时调用）
     */
    void updateInventoryQuantity(Long drugId, String batchNumber, Integer newQuantity);

    /**
     * 获取近效期预警统计
     */
    Map<String, Long> getNearExpiryWarning();

    /**
     * 获取库存总量
     */
    Long getTotalInventory();

    /**
     * 获取药品的可用批次列表（按FIFO排序，优先返回最早到期的批次）
     */
    java.util.List<Inventory> getAvailableBatches(Long drugId, Integer requiredQuantity);
}



