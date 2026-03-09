package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.exception.ServiceException;
import com.cdiom.backend.mapper.InventoryMapper;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.service.InventoryService;
import com.cdiom.backend.util.SystemConfigUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final SystemConfigUtil systemConfigUtil;

    @Override
    public Page<Inventory> getInventoryList(Integer page, Integer size, String keyword, Long drugId, String batchNumber, String storageLocation, LocalDate expiryDateStart, LocalDate expiryDateEnd, Integer isSpecial) {
        // 优化：如果有关键字或特殊药品筛选，使用JOIN查询避免N+1问题
        // 否则使用普通查询（性能更好）
        boolean needJoin = StringUtils.hasText(keyword) || isSpecial != null;
        
        if (needJoin) {
            // 使用JOIN查询优化N+1问题
            List<Inventory> allResults = inventoryMapper.selectInventoryListWithJoin(
                    keyword, drugId, batchNumber, storageLocation, 
                    expiryDateStart, expiryDateEnd, isSpecial);
            
            // 手动分页
            int start = (page - 1) * size;
            int end = Math.min(start + size, allResults.size());
            List<Inventory> pageResults = start < allResults.size() 
                    ? allResults.subList(start, end) 
                    : new java.util.ArrayList<>();
            
            Page<Inventory> pageParam = new Page<>(page, size, allResults.size());
            pageParam.setRecords(pageResults);
            return pageParam;
        } else {
            // 普通查询（无关键字和特殊药品筛选时，性能更好）
            Page<Inventory> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
            
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
            
            // 只查询数量大于0的库存
            wrapper.gt(Inventory::getQuantity, 0);
            
            // 按有效期排序（最早到期的在前，FIFO）
            wrapper.orderByAsc(Inventory::getExpiryDate);
            wrapper.orderByDesc(Inventory::getCreateTime);
            
            return inventoryMapper.selectPage(pageParam, wrapper);
        }
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
        try {
            if (quantity == null || quantity <= 0) {
                throw new ServiceException("入库数量必须大于0");
            }
            
            // 使用 INSERT ... ON DUPLICATE KEY UPDATE 原子操作，确保并发安全
            // 如果记录不存在则插入，存在则原子增加数量
            int affectedRows = inventoryMapper.insertOrUpdateInventory(drugId, batchNumber, quantity, 
                    expiryDate, storageLocation, productionDate, manufacturer);
            
            if (affectedRows == 0) {
                throw new ServiceException("入库操作失败，请重试");
            }
            
            // 查询更新后的库存数量用于日志
            Inventory inventory = inventoryMapper.selectForUpdate(drugId, batchNumber);
            if (inventory != null) {
                log.info("入库操作：药品ID={}, 批次号={}, 增加数量={}, 当前数量={}", drugId, batchNumber, quantity, inventory.getQuantity());
            } else {
                log.info("入库操作：药品ID={}, 批次号={}, 增加数量={}", drugId, batchNumber, quantity);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("入库操作异常：药品ID={}, 批次号={}, 数量={}", drugId, batchNumber, quantity, e);
            throw new ServiceException("入库操作失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseInventory(Long drugId, String batchNumber, Integer quantity) {
        try {
            if (quantity == null || quantity <= 0) {
                throw new ServiceException("出库数量必须大于0");
            }
            
            // 使用悲观锁查询库存，防止并发问题
            Inventory inventory = inventoryMapper.selectForUpdate(drugId, batchNumber);
            if (inventory == null) {
                throw new ServiceException("库存不存在");
            }
            
            // 检查库存是否充足
            if (inventory.getQuantity() < quantity) {
                throw new ServiceException("库存不足，当前库存：" + inventory.getQuantity() + "，需要出库：" + quantity);
            }
            
            // 使用原子更新操作减少库存，确保并发安全
            // 原子更新操作会在 WHERE 条件中检查 quantity >= quantity，确保库存充足
            int affectedRows = inventoryMapper.decreaseQuantityAtomically(drugId, batchNumber, quantity);
            if (affectedRows == 0) {
                // 如果原子更新失败（可能是在检查后到更新前库存被其他事务修改），再次查询确认
                Inventory currentInventory = inventoryMapper.selectForUpdate(drugId, batchNumber);
                if (currentInventory == null) {
                    throw new ServiceException("库存不存在");
                }
                throw new ServiceException("库存不足，当前库存：" + currentInventory.getQuantity() + "，需要出库：" + quantity);
            }
            
            // 计算剩余数量用于日志（避免再次查询数据库）
            int remainingQuantity = inventory.getQuantity() - quantity;
            log.info("减少库存：药品ID={}, 批次号={}, 减少数量={}, 剩余数量={}", drugId, batchNumber, quantity, remainingQuantity);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("出库操作异常：药品ID={}, 批次号={}, 数量={}", drugId, batchNumber, quantity, e);
            throw new ServiceException("出库操作失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInventoryQuantity(Long drugId, String batchNumber, Integer newQuantity) {
        try {
            if (newQuantity == null || newQuantity < 0) {
                throw new ServiceException("库存数量不能为负");
            }
            
            // 使用悲观锁查询库存，防止并发问题
            Inventory inventory = inventoryMapper.selectForUpdate(drugId, batchNumber);
            if (inventory == null) {
                throw new ServiceException("库存不存在");
            }
            
            inventory.setQuantity(newQuantity);
            inventoryMapper.updateById(inventory);
            log.info("更新库存数量：药品ID={}, 批次号={}, 新数量={}", drugId, batchNumber, newQuantity);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新库存数量异常：药品ID={}, 批次号={}, 新数量={}", drugId, batchNumber, newQuantity, e);
            throw new ServiceException("更新库存数量失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Long> getNearExpiryWarning() {
        // 从配置工具类获取预警天数（优先从数据库读取，否则使用配置文件默认值）
        Integer expiryWarningDays = systemConfigUtil.getExpiryWarningDays();
        Integer expiryCriticalDays = systemConfigUtil.getExpiryCriticalDays();
        
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
        
        // 计算累计数量，只返回足够数量的批次
        int totalQuantity = 0;
        java.util.List<Inventory> resultBatches = new java.util.ArrayList<>();
        for (Inventory batch : batches) {
            resultBatches.add(batch);
            totalQuantity += batch.getQuantity();
            if (totalQuantity >= requiredQuantity) {
                // 已找到足够数量的批次，停止添加
                break;
            }
        }
        
        // 验证总数量是否足够
        if (totalQuantity < requiredQuantity) {
            throw new ServiceException("库存不足，药品ID=" + drugId + "，需要数量：" + requiredQuantity + "，可用数量：" + totalQuantity);
        }
        
        return resultBatches;
    }

    @Override
    public int getTotalAvailableQuantity(Long drugId) {
        if (drugId == null) return 0;
        Integer total = inventoryMapper.getTotalAvailableQuantityByDrugId(drugId, LocalDate.now());
        return total != null ? total : 0;
    }
}

