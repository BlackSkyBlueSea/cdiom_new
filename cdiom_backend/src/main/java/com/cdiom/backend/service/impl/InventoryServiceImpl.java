package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.InventoryMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final DrugInfoMapper drugInfoMapper;

    @Value("${system.config.expiry-warning-days:180}")
    private Integer expiryWarningDays;

    @Value("${system.config.expiry-critical-days:90}")
    private Integer expiryCriticalDays;

    @Override
    public Page<Inventory> getInventoryList(Integer page, Integer size, String keyword, Long drugId, String batchNumber, String storageLocation, LocalDate expiryDateStart, LocalDate expiryDateEnd, Integer isSpecial) {
        Page<Inventory> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        
        // 如果有关键字，需要关联drug_info表查询
        if (StringUtils.hasText(keyword)) {
            // 先查询药品ID
            LambdaQueryWrapper<DrugInfo> drugWrapper = new LambdaQueryWrapper<>();
            drugWrapper.and(w -> w.like(DrugInfo::getDrugName, keyword)
                    .or().like(DrugInfo::getNationalCode, keyword)
                    .or().like(DrugInfo::getApprovalNumber, keyword));
            List<DrugInfo> drugs = drugInfoMapper.selectList(drugWrapper);
            if (!drugs.isEmpty()) {
                List<Long> drugIds = drugs.stream().map(DrugInfo::getId).collect(Collectors.toList());
                wrapper.in(Inventory::getDrugId, drugIds);
            } else {
                // 如果没有找到药品，返回空结果
                wrapper.eq(Inventory::getId, -1);
            }
        }
        
        if (drugId != null) {
            wrapper.eq(Inventory::getDrugId, drugId);
        }
        
        if (StringUtils.hasText(batchNumber)) {
            wrapper.like(Inventory::getBatchNumber, batchNumber);
        }
        
        if (StringUtils.hasText(storageLocation)) {
            wrapper.like(Inventory::getStorageLocation, storageLocation);
        }
        
        if (expiryDateStart != null) {
            wrapper.ge(Inventory::getExpiryDate, expiryDateStart);
        }
        
        if (expiryDateEnd != null) {
            wrapper.le(Inventory::getExpiryDate, expiryDateEnd);
        }
        
        // 特殊药品筛选（需要关联drug_info表）
        if (isSpecial != null) {
            LambdaQueryWrapper<DrugInfo> drugWrapper = new LambdaQueryWrapper<>();
            drugWrapper.eq(DrugInfo::getIsSpecial, isSpecial);
            List<DrugInfo> drugs = drugInfoMapper.selectList(drugWrapper);
            if (!drugs.isEmpty()) {
                List<Long> drugIds = drugs.stream().map(DrugInfo::getId).collect(Collectors.toList());
                wrapper.in(Inventory::getDrugId, drugIds);
            } else {
                wrapper.eq(Inventory::getId, -1);
            }
        }
        
        // 只查询数量大于0的库存
        wrapper.gt(Inventory::getQuantity, 0);
        
        // 按有效期排序（最早到期的在前，FIFO）
        wrapper.orderByAsc(Inventory::getExpiryDate);
        wrapper.orderByDesc(Inventory::getCreateTime);
        
        return inventoryMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Inventory getInventoryById(Long id) {
        return inventoryMapper.selectById(id);
    }

    @Override
    public Inventory getInventoryByDrugAndBatch(Long drugId, String batchNumber) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getDrugId, drugId);
        wrapper.eq(Inventory::getBatchNumber, batchNumber);
        return inventoryMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseInventory(Long drugId, String batchNumber, Integer quantity, LocalDate expiryDate, String storageLocation, LocalDate productionDate, String manufacturer) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("入库数量必须大于0");
        }
        
        // 查询是否已存在该批次
        Inventory existing = getInventoryByDrugAndBatch(drugId, batchNumber);
        
        if (existing != null) {
            // 如果批次已存在，增加数量
            existing.setQuantity(existing.getQuantity() + quantity);
            inventoryMapper.updateById(existing);
            log.info("更新库存：药品ID={}, 批次号={}, 增加数量={}, 当前数量={}", drugId, batchNumber, quantity, existing.getQuantity());
        } else {
            // 如果批次不存在，新增库存记录
            Inventory inventory = new Inventory();
            inventory.setDrugId(drugId);
            inventory.setBatchNumber(batchNumber);
            inventory.setQuantity(quantity);
            inventory.setExpiryDate(expiryDate);
            inventory.setStorageLocation(storageLocation);
            inventory.setProductionDate(productionDate);
            inventory.setManufacturer(manufacturer);
            inventoryMapper.insert(inventory);
            log.info("新增库存：药品ID={}, 批次号={}, 数量={}", drugId, batchNumber, quantity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseInventory(Long drugId, String batchNumber, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("出库数量必须大于0");
        }
        
        Inventory inventory = getInventoryByDrugAndBatch(drugId, batchNumber);
        if (inventory == null) {
            throw new RuntimeException("库存不存在");
        }
        
        if (inventory.getQuantity() < quantity) {
            throw new RuntimeException("库存不足，当前库存：" + inventory.getQuantity() + "，需要出库：" + quantity);
        }
        
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryMapper.updateById(inventory);
        log.info("减少库存：药品ID={}, 批次号={}, 减少数量={}, 剩余数量={}", drugId, batchNumber, quantity, inventory.getQuantity());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInventoryQuantity(Long drugId, String batchNumber, Integer newQuantity) {
        if (newQuantity == null || newQuantity < 0) {
            throw new RuntimeException("库存数量不能为负");
        }
        
        Inventory inventory = getInventoryByDrugAndBatch(drugId, batchNumber);
        if (inventory == null) {
            throw new RuntimeException("库存不存在");
        }
        
        inventory.setQuantity(newQuantity);
        inventoryMapper.updateById(inventory);
        log.info("更新库存数量：药品ID={}, 批次号={}, 新数量={}", drugId, batchNumber, newQuantity);
    }

    @Override
    public Map<String, Long> getNearExpiryWarning() {
        LocalDate today = LocalDate.now();
        LocalDate yellowWarningDate = today.plusDays(expiryWarningDays);
        LocalDate redWarningDate = today.plusDays(expiryCriticalDays);
        
        Long yellowCount = inventoryMapper.countYellowWarning(today, yellowWarningDate);
        Long redCount = inventoryMapper.countRedWarning(today, redWarningDate);
        
        Map<String, Long> result = new HashMap<>();
        result.put("yellow", yellowCount);
        result.put("red", redCount);
        
        return result;
    }

    @Override
    public Long getTotalInventory() {
        return inventoryMapper.getTotalInventory();
    }

    @Override
    public List<Inventory> getAvailableBatches(Long drugId, Integer requiredQuantity) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getDrugId, drugId);
        wrapper.gt(Inventory::getQuantity, 0);
        wrapper.ge(Inventory::getExpiryDate, LocalDate.now()); // 只返回未过期的批次
        // 按有效期排序（最早到期的在前，FIFO）
        wrapper.orderByAsc(Inventory::getExpiryDate);
        
        List<Inventory> batches = inventoryMapper.selectList(wrapper);
        
        // 计算累计数量，返回足够数量的批次
        int totalQuantity = 0;
        for (Inventory batch : batches) {
            totalQuantity += batch.getQuantity();
            if (totalQuantity >= requiredQuantity) {
                break;
            }
        }
        
        return batches;
    }
}

