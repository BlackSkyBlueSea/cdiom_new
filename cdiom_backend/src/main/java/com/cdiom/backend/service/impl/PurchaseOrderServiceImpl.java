package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.PurchaseOrderItemMapper;
import com.cdiom.backend.mapper.PurchaseOrderMapper;
import com.cdiom.backend.mapper.SupplierMapper;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;
import com.cdiom.backend.model.Supplier;
import com.cdiom.backend.model.SysNotice;
import com.cdiom.backend.service.InboundRecordService;
import com.cdiom.backend.service.PurchaseOrderService;
import com.cdiom.backend.service.SysNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InboundRecordService inboundRecordService;
    private final SysNoticeService sysNoticeService;
    private final SupplierMapper supplierMapper;

    /**
     * 构造函数注入
     * 使用 @Lazy 延迟加载 InboundRecordService 以解决循环依赖问题
     */
    public PurchaseOrderServiceImpl(
            PurchaseOrderMapper purchaseOrderMapper,
            PurchaseOrderItemMapper purchaseOrderItemMapper,
            @Lazy InboundRecordService inboundRecordService,
            SysNoticeService sysNoticeService,
            SupplierMapper supplierMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.inboundRecordService = inboundRecordService;
        this.sysNoticeService = sysNoticeService;
        this.supplierMapper = supplierMapper;
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只有待确认状态的订单才能确认");
        }
        order.setStatus("CONFIRMED");
        purchaseOrderMapper.updateById(order);
        log.info("确认采购订单：订单ID={}, 订单编号={}", id, order.getOrderNumber());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long id, String reason) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只有待确认状态的订单才能拒绝");
        }
        if (!StringUtils.hasText(reason)) {
            throw new RuntimeException("拒绝理由不能为空");
        }
        order.setStatus("REJECTED");
        order.setRejectReason(reason);
        purchaseOrderMapper.updateById(order);
        log.info("拒绝采购订单：订单ID={}, 订单编号={}, 拒绝理由={}", id, order.getOrderNumber(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long id, String logisticsNumber) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("只有待发货状态的订单才能发货");
        }
        if (!StringUtils.hasText(logisticsNumber)) {
            throw new RuntimeException("物流单号不能为空");
        }
        order.setStatus("SHIPPED");
        order.setLogisticsNumber(logisticsNumber);
        order.setShipDate(java.time.LocalDateTime.now());
        purchaseOrderMapper.updateById(order);
        
        // 创建待入库提醒通知，推送给仓库管理员
        try {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            String supplierName = supplier != null ? supplier.getName() : "未知供应商";
            
            SysNotice notice = new SysNotice();
            notice.setNoticeTitle("待入库提醒");
            notice.setNoticeContent(String.format("订单编号：%s\n供应商：%s\n物流单号：%s\n发货日期：%s\n\n请及时进行入库验收。",
                    order.getOrderNumber(),
                    supplierName,
                    logisticsNumber,
                    order.getShipDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            notice.setNoticeType(1); // 1-通知
            notice.setStatus(1); // 1-正常
            notice.setCreateBy(null); // 系统自动创建
            sysNoticeService.createNotice(notice);
            
            log.info("已创建待入库提醒通知：订单编号={}", order.getOrderNumber());
        } catch (Exception e) {
            // 通知创建失败不影响发货流程
            log.warn("创建待入库提醒通知失败：订单ID={}, 错误={}", id, e.getMessage());
        }
        
        log.info("发货采购订单：订单ID={}, 订单编号={}, 物流单号={}", id, order.getOrderNumber(), logisticsNumber);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String reason) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        // 已入库或已取消的订单不能取消
        if ("RECEIVED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("已入库或已取消的订单不能取消");
        }
        // 已拒绝的订单不能取消
        if ("REJECTED".equals(order.getStatus())) {
            throw new RuntimeException("已拒绝的订单不能取消");
        }
        order.setStatus("CANCELLED");
        if (StringUtils.hasText(reason)) {
            order.setRemark((StringUtils.hasText(order.getRemark()) ? order.getRemark() + "\n" : "") + "取消原因：" + reason);
        }
        purchaseOrderMapper.updateById(order);
        log.info("取消采购订单：订单ID={}, 订单编号={}, 取消原因={}", id, order.getOrderNumber(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLogisticsNumber(Long id, String logisticsNumber) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        // 只有已发货或已确认的订单才能更新物流单号
        if (!"SHIPPED".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("只有待发货或已发货状态的订单才能更新物流单号");
        }
        if (!StringUtils.hasText(logisticsNumber)) {
            throw new RuntimeException("物流单号不能为空");
        }
        order.setLogisticsNumber(logisticsNumber);
        // 如果订单是待发货状态，更新物流单号后自动变为已发货
        if ("CONFIRMED".equals(order.getStatus())) {
            order.setStatus("SHIPPED");
            order.setShipDate(java.time.LocalDateTime.now());
        }
        purchaseOrderMapper.updateById(order);
        log.info("更新物流单号：订单ID={}, 订单编号={}, 物流单号={}", id, order.getOrderNumber(), logisticsNumber);
    }

    /**
     * 生成订单编号
     * 格式：ORD + 日期（YYYYMMDD）+ 3位序号
     * 符合GSP规范，用于生成Code128条形码
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
        return "ORD" + dateStr + sequence;
    }
}

