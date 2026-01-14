package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.InventoryAdjustmentMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.InventoryAdjustment;
import com.cdiom.backend.service.InventoryAdjustmentService;
import com.cdiom.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 库存调整服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAdjustmentServiceImpl implements InventoryAdjustmentService {

    private final InventoryAdjustmentMapper inventoryAdjustmentMapper;
    private final InventoryService inventoryService;
    private final DrugInfoMapper drugInfoMapper;

    @Override
    public Page<InventoryAdjustment> getInventoryAdjustmentList(Integer page, Integer size, String keyword, Long drugId, String batchNumber, String adjustmentType, Long operatorId, LocalDate startDate, LocalDate endDate) {
        Page<InventoryAdjustment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InventoryAdjustment> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InventoryAdjustment::getAdjustmentNumber, keyword)
                    .or().like(InventoryAdjustment::getBatchNumber, keyword)
                    .or().like(InventoryAdjustment::getAdjustmentReason, keyword));
        }
        
        if (drugId != null) {
            wrapper.eq(InventoryAdjustment::getDrugId, drugId);
        }
        
        if (StringUtils.hasText(batchNumber)) {
            wrapper.like(InventoryAdjustment::getBatchNumber, batchNumber);
        }
        
        if (StringUtils.hasText(adjustmentType)) {
            wrapper.eq(InventoryAdjustment::getAdjustmentType, adjustmentType);
        }
        
        if (operatorId != null) {
            wrapper.eq(InventoryAdjustment::getOperatorId, operatorId);
        }
        
        if (startDate != null) {
            wrapper.ge(InventoryAdjustment::getCreateTime, startDate.atStartOfDay());
        }
        
        if (endDate != null) {
            wrapper.le(InventoryAdjustment::getCreateTime, endDate.plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(InventoryAdjustment::getCreateTime);
        
        return inventoryAdjustmentMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public InventoryAdjustment getInventoryAdjustmentById(Long id) {
        return inventoryAdjustmentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustment createInventoryAdjustment(InventoryAdjustment adjustment, Long secondOperatorId) {
        // 验证药品是否存在
        DrugInfo drug = drugInfoMapper.selectById(adjustment.getDrugId());
        if (drug == null) {
            throw new RuntimeException("药品信息不存在");
        }
        
        // 检查是否特殊药品，需要第二操作人
        if (drug.getIsSpecial() != null && drug.getIsSpecial() == 1) {
            if (secondOperatorId == null) {
                throw new RuntimeException("特殊药品库存调整需要第二操作人确认");
            }
            adjustment.setSecondOperatorId(secondOperatorId);
        }
        
        // 获取调整前数量
        com.cdiom.backend.model.Inventory inventory = inventoryService.getInventoryByDrugAndBatch(
                adjustment.getDrugId(), adjustment.getBatchNumber());
        
        if (inventory == null) {
            throw new RuntimeException("库存不存在");
        }
        
        adjustment.setQuantityBefore(inventory.getQuantity());
        
        // 计算调整数量
        int adjustmentQuantity = adjustment.getQuantityAfter() - adjustment.getQuantityBefore();
        adjustment.setAdjustmentQuantity(adjustmentQuantity);
        
        // 验证调整后数量不能为负
        if (adjustment.getQuantityAfter() < 0) {
            throw new RuntimeException("调整后数量不能为负");
        }
        
        // 生成调整单号
        String adjustmentNumber = generateAdjustmentNumber();
        adjustment.setAdjustmentNumber(adjustmentNumber);
        
        // 保存调整记录
        inventoryAdjustmentMapper.insert(adjustment);
        
        // 更新库存数量
        inventoryService.updateInventoryQuantity(
                adjustment.getDrugId(),
                adjustment.getBatchNumber(),
                adjustment.getQuantityAfter()
        );
        
        log.info("创建库存调整：调整单号={}, 药品ID={}, 批次号={}, 调整前={}, 调整后={}, 调整数量={}",
                adjustmentNumber, adjustment.getDrugId(), adjustment.getBatchNumber(),
                adjustment.getQuantityBefore(), adjustment.getQuantityAfter(), adjustmentQuantity);
        
        return adjustment;
    }

    /**
     * 生成调整单号
     * 格式：ADJ + 日期（YYYYMMDD）+ 3位序号
     */
    private String generateAdjustmentNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询今天已生成的调整单数量
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<InventoryAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InventoryAdjustment::getCreateTime, today.atStartOfDay());
        wrapper.lt(InventoryAdjustment::getCreateTime, today.plusDays(1).atStartOfDay());
        long count = inventoryAdjustmentMapper.selectCount(wrapper);
        String sequence = String.format("%03d", count + 1);
        return "ADJ" + dateStr + sequence;
    }
}



