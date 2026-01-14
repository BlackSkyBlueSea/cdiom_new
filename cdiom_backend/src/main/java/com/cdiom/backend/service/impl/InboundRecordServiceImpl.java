package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.InboundRecordMapper;
import com.cdiom.backend.mapper.PurchaseOrderItemMapper;
import com.cdiom.backend.mapper.PurchaseOrderMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.InboundRecord;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;
import com.cdiom.backend.service.InboundRecordService;
import com.cdiom.backend.service.InventoryService;
import com.cdiom.backend.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 入库记录服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundRecordServiceImpl implements InboundRecordService {

    private final InboundRecordMapper inboundRecordMapper;
    private final DrugInfoMapper drugInfoMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InventoryService inventoryService;
    private final PurchaseOrderService purchaseOrderService;

    @Value("${system.config.expiry-warning-days:180}")
    private Integer expiryWarningDays;

    @Value("${system.config.expiry-critical-days:90}")
    private Integer expiryCriticalDays;

    @Override
    public Page<InboundRecord> getInboundRecordList(Integer page, Integer size, String keyword, Long orderId, Long drugId, String batchNumber, Long operatorId, LocalDate startDate, LocalDate endDate, String status, String expiryCheckStatus) {
        Page<InboundRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InboundRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InboundRecord::getRecordNumber, keyword)
                    .or().like(InboundRecord::getBatchNumber, keyword)
                    .or().like(InboundRecord::getManufacturer, keyword));
        }
        
        if (orderId != null) {
            wrapper.eq(InboundRecord::getOrderId, orderId);
        }
        
        if (drugId != null) {
            wrapper.eq(InboundRecord::getDrugId, drugId);
        }
        
        if (StringUtils.hasText(batchNumber)) {
            wrapper.like(InboundRecord::getBatchNumber, batchNumber);
        }
        
        if (operatorId != null) {
            wrapper.eq(InboundRecord::getOperatorId, operatorId);
        }
        
        if (startDate != null) {
            wrapper.ge(InboundRecord::getCreateTime, startDate.atStartOfDay());
        }
        
        if (endDate != null) {
            wrapper.le(InboundRecord::getCreateTime, endDate.plusDays(1).atStartOfDay());
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(InboundRecord::getStatus, status);
        }
        
        if (StringUtils.hasText(expiryCheckStatus)) {
            wrapper.eq(InboundRecord::getExpiryCheckStatus, expiryCheckStatus);
        }
        
        wrapper.orderByDesc(InboundRecord::getCreateTime);
        
        return inboundRecordMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public InboundRecord getInboundRecordById(Long id) {
        return inboundRecordMapper.selectById(id);
    }

    @Override
    public InboundRecord getInboundRecordByRecordNumber(String recordNumber) {
        LambdaQueryWrapper<InboundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundRecord::getRecordNumber, recordNumber);
        return inboundRecordMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundRecord createInboundRecordFromOrder(InboundRecord inboundRecord, Long orderId, Long drugId) {
        // 验证订单是否存在且状态为SHIPPED
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不是已发货，无法入库");
        }
        
        // 验证药品是否存在
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) {
            throw new RuntimeException("药品信息不存在");
        }
        
        // 检查是否特殊药品，需要第二操作人
        if (drug.getIsSpecial() != null && drug.getIsSpecial() == 1) {
            if (inboundRecord.getSecondOperatorId() == null) {
                throw new RuntimeException("特殊药品入库需要第二操作人确认");
            }
        }
        
        // 检查入库数量是否超过订单数量
        // 查询订单明细的采购数量
        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId)
                   .eq(PurchaseOrderItem::getDrugId, drugId);
        PurchaseOrderItem orderItem = purchaseOrderItemMapper.selectOne(itemWrapper);
        
        if (orderItem == null) {
            throw new RuntimeException("订单中不存在该药品的明细信息");
        }
        
        // 获取已入库数量（只统计验收合格的）
        Integer existingInboundQuantity = getInboundQuantityByOrderAndDrug(orderId, drugId);
        if (existingInboundQuantity == null) {
            existingInboundQuantity = 0;
        }
        
        // 检查本次入库数量加上已入库数量是否超过订单采购数量
        Integer totalInboundQuantity = existingInboundQuantity + inboundRecord.getQuantity();
        if (totalInboundQuantity > orderItem.getQuantity()) {
            throw new RuntimeException(String.format(
                "入库数量超过订单采购数量。订单采购数量：%d，已入库数量：%d，本次入库数量：%d，总计：%d",
                orderItem.getQuantity(), existingInboundQuantity, inboundRecord.getQuantity(), totalInboundQuantity
            ));
        }
        
        // 生成入库单号
        String recordNumber = generateRecordNumber();
        inboundRecord.setRecordNumber(recordNumber);
        inboundRecord.setOrderId(orderId);
        inboundRecord.setDrugId(drugId);
        
        // 设置到货日期（如果为空，默认为今天）
        if (inboundRecord.getArrivalDate() == null) {
            inboundRecord.setArrivalDate(LocalDate.now());
        }
        
        // 效期校验
        String expiryCheckStatus = checkExpiryDate(inboundRecord.getExpiryDate());
        inboundRecord.setExpiryCheckStatus(expiryCheckStatus);
        
        // 如果效期不足，需要填写原因
        if ("FORCE".equals(expiryCheckStatus) && !StringUtils.hasText(inboundRecord.getExpiryCheckReason())) {
            throw new RuntimeException("有效期不足90天，需要填写强制入库原因");
        }
        
        // 设置默认验收状态
        if (!StringUtils.hasText(inboundRecord.getStatus())) {
            inboundRecord.setStatus("QUALIFIED");
        }
        
        // 保存入库记录
        inboundRecordMapper.insert(inboundRecord);
        
        // 如果验收合格，更新库存
        if ("QUALIFIED".equals(inboundRecord.getStatus())) {
            inventoryService.increaseInventory(
                    drugId,
                    inboundRecord.getBatchNumber(),
                    inboundRecord.getQuantity(),
                    inboundRecord.getExpiryDate(),
                    drug.getStorageLocation(),
                    inboundRecord.getProductionDate(),
                    inboundRecord.getManufacturer() != null ? inboundRecord.getManufacturer() : drug.getManufacturer()
            );
            
            // 更新订单状态（检查是否全部入库）
            purchaseOrderService.updateOrderInboundStatus(orderId);
        }
        
        log.info("创建入库记录：入库单号={}, 订单ID={}, 药品ID={}, 数量={}", recordNumber, orderId, drugId, inboundRecord.getQuantity());
        
        return inboundRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundRecord createInboundRecordTemporary(InboundRecord inboundRecord, Long drugId) {
        // 验证药品是否存在
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) {
            throw new RuntimeException("药品信息不存在");
        }
        
        // 检查是否特殊药品，需要第二操作人
        if (drug.getIsSpecial() != null && drug.getIsSpecial() == 1) {
            if (inboundRecord.getSecondOperatorId() == null) {
                throw new RuntimeException("特殊药品入库需要第二操作人确认");
            }
        }
        
        // 生成入库单号
        String recordNumber = generateRecordNumber();
        inboundRecord.setRecordNumber(recordNumber);
        inboundRecord.setOrderId(null); // 临时入库不关联订单
        inboundRecord.setDrugId(drugId);
        
        // 设置到货日期（如果为空，默认为今天）
        if (inboundRecord.getArrivalDate() == null) {
            inboundRecord.setArrivalDate(LocalDate.now());
        }
        
        // 效期校验
        String expiryCheckStatus = checkExpiryDate(inboundRecord.getExpiryDate());
        inboundRecord.setExpiryCheckStatus(expiryCheckStatus);
        
        // 如果效期不足，需要填写原因
        if ("FORCE".equals(expiryCheckStatus) && !StringUtils.hasText(inboundRecord.getExpiryCheckReason())) {
            throw new RuntimeException("有效期不足90天，需要填写强制入库原因");
        }
        
        // 设置默认验收状态
        if (!StringUtils.hasText(inboundRecord.getStatus())) {
            inboundRecord.setStatus("QUALIFIED");
        }
        
        // 保存入库记录
        inboundRecordMapper.insert(inboundRecord);
        
        // 如果验收合格，更新库存
        if ("QUALIFIED".equals(inboundRecord.getStatus())) {
            inventoryService.increaseInventory(
                    drugId,
                    inboundRecord.getBatchNumber(),
                    inboundRecord.getQuantity(),
                    inboundRecord.getExpiryDate(),
                    drug.getStorageLocation(),
                    inboundRecord.getProductionDate(),
                    inboundRecord.getManufacturer() != null ? inboundRecord.getManufacturer() : drug.getManufacturer()
            );
        }
        
        log.info("创建临时入库记录：入库单号={}, 药品ID={}, 数量={}", recordNumber, drugId, inboundRecord.getQuantity());
        
        return inboundRecord;
    }

    @Override
    public String checkExpiryDate(LocalDate expiryDate) {
        if (expiryDate == null) {
            return "FORCE"; // 如果没有有效期，需要强制确认
        }
        
        LocalDate today = LocalDate.now();
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
        
        if (daysUntilExpiry >= expiryWarningDays) {
            return "PASS"; // 有效期≥180天，直接通过
        } else if (daysUntilExpiry >= expiryCriticalDays) {
            return "WARNING"; // 90-180天，需确认
        } else {
            return "FORCE"; // <90天，需填写强制入库原因
        }
    }

    @Override
    public Integer getInboundQuantityByOrderAndDrug(Long orderId, Long drugId) {
        return inboundRecordMapper.getInboundQuantityByOrderAndDrug(orderId, drugId);
    }

    @Override
    public Long getTodayInboundCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        return inboundRecordMapper.countTodayInbound(todayStart, todayEnd);
    }

    /**
     * 生成入库单号
     * 格式：IN + 日期（YYYYMMDD）+ 3位序号
     */
    private String generateRecordNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询今天已生成的入库单数量
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<InboundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InboundRecord::getCreateTime, today.atStartOfDay());
        wrapper.lt(InboundRecord::getCreateTime, today.plusDays(1).atStartOfDay());
        long count = inboundRecordMapper.selectCount(wrapper);
        String sequence = String.format("%03d", count + 1);
        return "IN" + dateStr + sequence;
    }
}

