package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.PurchaseOrderItemMapper;
import com.cdiom.backend.mapper.PurchaseOrderMapper;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;
import com.cdiom.backend.service.InboundRecordService;
import com.cdiom.backend.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购订单服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InboundRecordService inboundRecordService;

    @Override
    public Page<PurchaseOrder> getPurchaseOrderList(Integer page, Integer size, String keyword, Long supplierId, Long purchaserId, String status) {
        Page<PurchaseOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PurchaseOrder::getOrderNumber, keyword)
                    .or().like(PurchaseOrder::getLogisticsNumber, keyword));
        }
        
        if (supplierId != null) {
            wrapper.eq(PurchaseOrder::getSupplierId, supplierId);
        }
        
        if (purchaserId != null) {
            wrapper.eq(PurchaseOrder::getPurchaserId, purchaserId);
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(PurchaseOrder::getStatus, status);
        }
        
        wrapper.orderByDesc(PurchaseOrder::getCreateTime);
        
        return purchaseOrderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public PurchaseOrder getPurchaseOrderById(Long id) {
        return purchaseOrderMapper.selectById(id);
    }

    @Override
    public PurchaseOrder getPurchaseOrderByOrderNumber(String orderNumber) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getOrderNumber, orderNumber);
        return purchaseOrderMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("订单明细不能为空");
        }
        
        // 生成订单编号
        if (!StringUtils.hasText(purchaseOrder.getOrderNumber())) {
            purchaseOrder.setOrderNumber(generateOrderNumber());
        } else {
            // 检查订单编号是否已存在
            PurchaseOrder existing = getPurchaseOrderByOrderNumber(purchaseOrder.getOrderNumber());
            if (existing != null) {
                throw new RuntimeException("订单编号已存在");
            }
        }
        
        // 设置默认状态
        if (!StringUtils.hasText(purchaseOrder.getStatus())) {
            purchaseOrder.setStatus("PENDING");
        }
        
        // 计算订单总金额
        BigDecimal totalAmount = items.stream()
                .map(item -> {
                    if (item.getUnitPrice() != null && item.getQuantity() != null) {
                        item.setTotalPrice(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
                        return item.getTotalPrice();
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        purchaseOrder.setTotalAmount(totalAmount);
        
        // 保存订单
        purchaseOrderMapper.insert(purchaseOrder);
        
        // 保存订单明细
        for (PurchaseOrderItem item : items) {
            item.setOrderId(purchaseOrder.getId());
            purchaseOrderItemMapper.insert(item);
        }
        
        log.info("创建采购订单：订单编号={}, 供应商ID={}, 总金额={}", purchaseOrder.getOrderNumber(), purchaseOrder.getSupplierId(), totalAmount);
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        PurchaseOrder existing = purchaseOrderMapper.selectById(purchaseOrder.getId());
        if (existing == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 如果修改了订单编号，检查是否重复
        if (StringUtils.hasText(purchaseOrder.getOrderNumber()) 
                && !purchaseOrder.getOrderNumber().equals(existing.getOrderNumber())) {
            PurchaseOrder duplicate = getPurchaseOrderByOrderNumber(purchaseOrder.getOrderNumber());
            if (duplicate != null) {
                throw new RuntimeException("订单编号已存在");
            }
        }
        
        purchaseOrderMapper.updateById(purchaseOrder);
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseOrder(Long id) {
        // 删除订单明细（级联删除）
        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, id);
        purchaseOrderItemMapper.delete(itemWrapper);
        
        // 删除订单
        purchaseOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long id, String status, String reason) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        order.setStatus(status);
        if ("REJECTED".equals(status) && StringUtils.hasText(reason)) {
            order.setRejectReason(reason);
        }
        if ("SHIPPED".equals(status)) {
            order.setShipDate(java.time.LocalDateTime.now());
        }
        
        purchaseOrderMapper.updateById(order);
    }

    @Override
    public List<PurchaseOrderItem> getOrderItems(Long orderId) {
        return purchaseOrderItemMapper.selectByOrderId(orderId);
    }

    @Override
    public Map<Long, Integer> getInboundQuantitiesByOrder(Long orderId) {
        List<PurchaseOrderItem> items = getOrderItems(orderId);
        Map<Long, Integer> result = new HashMap<>();
        
        for (PurchaseOrderItem item : items) {
            Integer inboundQuantity = inboundRecordService.getInboundQuantityByOrderAndDrug(orderId, item.getDrugId());
            result.put(item.getDrugId(), inboundQuantity);
        }
        
        return result;
    }

    @Override
    public boolean canInbound(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        return order != null && "SHIPPED".equals(order.getStatus());
    }

    @Override
    public boolean isOrderFullyInbound(Long orderId) {
        List<PurchaseOrderItem> items = getOrderItems(orderId);
        Map<Long, Integer> inboundQuantities = getInboundQuantitiesByOrder(orderId);
        
        for (PurchaseOrderItem item : items) {
            Integer inboundQuantity = inboundQuantities.getOrDefault(item.getDrugId(), 0);
            if (inboundQuantity < item.getQuantity()) {
                return false; // 还有未入库的
            }
        }
        
        return true; // 全部入库
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderInboundStatus(Long orderId) {
        if (isOrderFullyInbound(orderId)) {
            updateOrderStatus(orderId, "RECEIVED", null);
            log.info("订单全部入库完成：订单ID={}", orderId);
        }
    }

    /**
     * 生成订单编号
     * 格式：PO + 日期（YYYYMMDD）+ 3位序号
     */
    private String generateOrderNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询今天已生成的订单数量
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(PurchaseOrder::getCreateTime, today.atStartOfDay());
        wrapper.lt(PurchaseOrder::getCreateTime, today.plusDays(1).atStartOfDay());
        long count = purchaseOrderMapper.selectCount(wrapper);
        String sequence = String.format("%03d", count + 1);
        return "PO" + dateStr + sequence;
    }
}

