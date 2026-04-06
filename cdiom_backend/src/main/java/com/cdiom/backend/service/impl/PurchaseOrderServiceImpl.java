package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.exception.ServiceException;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.PurchaseOrderItemMapper;
import com.cdiom.backend.mapper.PurchaseOrderMapper;
import com.cdiom.backend.mapper.SupplierMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;
import com.cdiom.backend.model.Supplier;
import com.cdiom.backend.model.SysNotice;
import com.cdiom.backend.service.InboundRecordService;
import com.cdiom.backend.service.PurchaseOrderService;
import com.cdiom.backend.service.SysNoticeService;
import com.cdiom.backend.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final DrugInfoMapper drugInfoMapper;

    /**
     * 构造函数注入
     * 使用 @Lazy 延迟加载 InboundRecordService 以解决循环依赖问题
     */
    public PurchaseOrderServiceImpl(
            PurchaseOrderMapper purchaseOrderMapper,
            PurchaseOrderItemMapper purchaseOrderItemMapper,
            @Lazy InboundRecordService inboundRecordService,
            SysNoticeService sysNoticeService,
            SupplierMapper supplierMapper,
            DrugInfoMapper drugInfoMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.inboundRecordService = inboundRecordService;
        this.sysNoticeService = sysNoticeService;
        this.supplierMapper = supplierMapper;
        this.drugInfoMapper = drugInfoMapper;
    }

    @Override
    public Page<PurchaseOrder> getPurchaseOrderList(Integer page, Integer size, String keyword, Long supplierId, Long purchaserId, String status) {
        Page<PurchaseOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();

        // 支持按订单编号、物流单号和供应商名称模糊查询
        Set<Long> supplierIdsByName = null;
        if (StringUtils.hasText(keyword)) {
            List<Supplier> supplierList = supplierMapper.selectList(
                    new LambdaQueryWrapper<Supplier>()
                            .like(Supplier::getName, keyword)
            );
            if (supplierList != null && !supplierList.isEmpty()) {
                supplierIdsByName = supplierList.stream()
                        .map(Supplier::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        }

        if (StringUtils.hasText(keyword)) {
            Set<Long> finalSupplierIdsByName = supplierIdsByName;
            wrapper.and(w -> {
                w.like(PurchaseOrder::getOrderNumber, keyword)
                        .or().like(PurchaseOrder::getLogisticsNumber, keyword);
                if (finalSupplierIdsByName != null && !finalSupplierIdsByName.isEmpty()) {
                    w.or().in(PurchaseOrder::getSupplierId, finalSupplierIdsByName);
                }
            });
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

        Page<PurchaseOrder> orderPage = purchaseOrderMapper.selectPage(pageParam, wrapper);

        // 为列表中的订单补充供应商名称，方便前端展示
        List<PurchaseOrder> records = orderPage.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> supplierIds = records.stream()
                    .map(PurchaseOrder::getSupplierId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!supplierIds.isEmpty()) {
                List<Supplier> suppliers = supplierMapper.selectByIds(supplierIds);
                Map<Long, String> supplierNameMap = suppliers.stream()
                        .collect(Collectors.toMap(Supplier::getId, Supplier::getName));
                for (PurchaseOrder order : records) {
                    if (order.getSupplierId() != null) {
                        order.setSupplierName(supplierNameMap.get(order.getSupplierId()));
                    }
                }
            }
        }

        return orderPage;
    }

    @Override
    public PurchaseOrder getPurchaseOrderById(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order != null && order.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            if (supplier != null) {
                order.setSupplierName(supplier.getName());
            }
        }
        return order;
    }

    @Override
    public PurchaseOrder getPurchaseOrderByOrderNumber(String orderNumber) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getOrderNumber, orderNumber);
        PurchaseOrder order = purchaseOrderMapper.selectOne(wrapper);
        if (order != null && order.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            if (supplier != null) {
                order.setSupplierName(supplier.getName());
            }
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items) {
        try {
            if (items == null || items.isEmpty()) {
                throw new ServiceException("订单明细不能为空");
            }
            
            // 若未指定单号，使用重试机制生成并创建订单
            if (!StringUtils.hasText(purchaseOrder.getOrderNumber())) {
                try {
                    return RetryUtil.executeWithRetry(() -> createOrderWithGeneratedNumber(purchaseOrder, items));
                } catch (Exception e) {
                    if (e.getCause() instanceof DuplicateKeyException) {
                        throw new ServiceException("当前订单创建过于繁忙，请稍后重试");
                    }
                    throw new ServiceException("创建订单失败：" + e.getMessage());
                }
            } else {
                // 手动指定单号，先校验唯一性
                PurchaseOrder existing = getPurchaseOrderByOrderNumber(purchaseOrder.getOrderNumber());
                if (existing != null) {
                    throw new ServiceException("订单编号已存在");
                }
                return createOrderDirectly(purchaseOrder, items);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建采购订单异常：订单编号={}", purchaseOrder.getOrderNumber(), e);
            throw new ServiceException("创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 核心任务：生成单号 + 插入订单（供重试工具调用）
     */
    private PurchaseOrder createOrderWithGeneratedNumber(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items) {
        // 1. 生成唯一单号（原有逻辑，此处保留格式兼容性）
        String orderNumber = generateOrderNumber();
        purchaseOrder.setOrderNumber(orderNumber);
        
        // 2. 插入订单（若单号重复，会抛出DuplicateKeyException）
        return createOrderDirectly(purchaseOrder, items);
    }

    /**
     * 直接创建订单（不生成单号，用于手动指定单号或重试场景）
     */
    private PurchaseOrder createOrderDirectly(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items) {
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
        
        // 保存订单（若单号重复，数据库唯一索引会抛出DuplicateKeyException）
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
    @Transactional(rollbackFor = Throwable.class)
    public PurchaseOrder updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        try {
            PurchaseOrder existing = purchaseOrderMapper.selectById(purchaseOrder.getId());
            if (existing == null) {
                throw new ServiceException("采购订单不存在");
            }
            
            // 如果修改了订单编号，检查是否重复
            if (StringUtils.hasText(purchaseOrder.getOrderNumber()) 
                    && !purchaseOrder.getOrderNumber().equals(existing.getOrderNumber())) {
                PurchaseOrder duplicate = getPurchaseOrderByOrderNumber(purchaseOrder.getOrderNumber());
                if (duplicate != null) {
                    throw new ServiceException("订单编号已存在");
                }
            }
            
            purchaseOrderMapper.updateById(purchaseOrder);
            return purchaseOrder;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新采购订单异常：订单ID={}", purchaseOrder.getId(), e);
            throw new ServiceException("更新订单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deletePurchaseOrder(Long id) {
        // 删除订单明细（级联删除）
        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, id);
        purchaseOrderItemMapper.delete(itemWrapper);
        
        // 删除订单
        purchaseOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateOrderStatus(Long id, String status, String reason) {
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(id);
            if (order == null) {
                throw new ServiceException("采购订单不存在");
            }
            
            order.setStatus(status);
            if ("REJECTED".equals(status) && StringUtils.hasText(reason)) {
                order.setRejectReason(reason);
            }
            if ("SHIPPED".equals(status)) {
                order.setShipDate(java.time.LocalDateTime.now());
            }
            
            purchaseOrderMapper.updateById(order);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新订单状态异常：订单ID={}, 状态={}", id, status, e);
            throw new ServiceException("更新订单状态失败：" + e.getMessage());
        }
    }

    @Override
    public List<PurchaseOrderItem> getOrderItems(Long orderId) {
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectByOrderId(orderId);
        if (items == null) {
            return Collections.emptyList();
        }
        if (items.isEmpty()) {
            return items;
        }

        // 为订单明细补充药品名称和规格，方便前端详情展示
        Set<Long> drugIds = items.stream()
                .map(PurchaseOrderItem::getDrugId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!drugIds.isEmpty()) {
            List<DrugInfo> drugInfos = drugInfoMapper.selectByIds(drugIds);
            Map<Long, DrugInfo> drugInfoMap = drugInfos.stream()
                    .collect(Collectors.toMap(DrugInfo::getId, d -> d));
            for (PurchaseOrderItem item : items) {
                DrugInfo drug = drugInfoMap.get(item.getDrugId());
                if (drug != null) {
                    item.setDrugName(drug.getDrugName());
                    item.setSpecification(drug.getSpecification());
                }
            }
        }

        return items;
    }

    @Override
    public Map<Long, Integer> getInboundQuantitiesByOrder(Long orderId) {
        List<PurchaseOrderItem> items = getOrderItems(orderId);
        Map<Long, Integer> result = new HashMap<>();
        for (PurchaseOrderItem item : items) {
            Integer allocated = inboundRecordService.getInboundCommittedQuantityByOrderAndDrug(orderId, item.getDrugId());
            result.put(item.getDrugId(), allocated != null ? allocated : 0);
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
        boolean allReceived = true;
        for (PurchaseOrderItem item : getOrderItems(orderId)) {
            Integer allocated = inboundRecordService.getInboundCommittedQuantityByOrderAndDrug(orderId, item.getDrugId());
            int a = allocated != null ? allocated : 0;
            int ordered = item.getQuantity() != null ? item.getQuantity() : 0;
            if (a < ordered) {
                allReceived = false;
                break;
            }
        }
        return allReceived;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateOrderInboundStatus(Long orderId) {
        if (isOrderFullyInbound(orderId)) {
            updateOrderStatus(orderId, "RECEIVED", null);
            log.info("订单全部入库完成：订单ID={}", orderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void confirmOrder(Long id) {
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(id);
            if (order == null) {
                throw new ServiceException("采购订单不存在");
            }
            if (!"PENDING".equals(order.getStatus())) {
                throw new ServiceException("只有待确认状态的订单才能确认");
            }
            order.setStatus("CONFIRMED");
            purchaseOrderMapper.updateById(order);
            log.info("确认采购订单：订单ID={}, 订单编号={}", id, order.getOrderNumber());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("确认采购订单异常：订单ID={}", id, e);
            throw new ServiceException("确认订单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void rejectOrder(Long id, String reason) {
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(id);
            if (order == null) {
                throw new ServiceException("采购订单不存在");
            }
            if (!"PENDING".equals(order.getStatus())) {
                throw new ServiceException("只有待确认状态的订单才能拒绝");
            }
            if (!StringUtils.hasText(reason)) {
                throw new ServiceException("拒绝理由不能为空");
            }
            order.setStatus("REJECTED");
            order.setRejectReason(reason);
            purchaseOrderMapper.updateById(order);
            log.info("拒绝采购订单：订单ID={}, 订单编号={}, 拒绝理由={}", id, order.getOrderNumber(), reason);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("拒绝采购订单异常：订单ID={}", id, e);
            throw new ServiceException("拒绝订单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void shipOrder(Long id, String logisticsNumber) {
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(id);
            if (order == null) {
                throw new ServiceException("采购订单不存在");
            }
            if (!"CONFIRMED".equals(order.getStatus())) {
                throw new ServiceException("只有待发货状态的订单才能发货");
            }
            if (!StringUtils.hasText(logisticsNumber)) {
                throw new ServiceException("物流单号不能为空");
            }
            order.setStatus("SHIPPED");
            order.setLogisticsNumber(logisticsNumber);
            order.setShipDate(java.time.LocalDateTime.now());
            purchaseOrderMapper.updateById(order);

            createInboundShipNotice(order, id, logisticsNumber);

            log.info("发货采购订单：订单ID={}, 订单编号={}, 物流单号={}", id, order.getOrderNumber(), logisticsNumber);
        } catch (RuntimeException e) {
            if (e instanceof ServiceException) {
                throw e;
            }
            log.error("发货采购订单异常：订单ID={}", id, e);
            throw new ServiceException("发货失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void cancelOrder(Long id, String reason) {
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(id);
            if (order == null) {
                throw new ServiceException("采购订单不存在");
            }
            // 已入库或已取消的订单不能取消
            if ("RECEIVED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
                throw new ServiceException("已入库或已取消的订单不能取消");
            }
            // 已拒绝的订单不能取消
            if ("REJECTED".equals(order.getStatus())) {
                throw new ServiceException("已拒绝的订单不能取消");
            }
            order.setStatus("CANCELLED");
            if (StringUtils.hasText(reason)) {
                order.setRemark((StringUtils.hasText(order.getRemark()) ? order.getRemark() + "\n" : "") + "取消原因：" + reason);
            }
            purchaseOrderMapper.updateById(order);
            log.info("取消采购订单：订单ID={}, 订单编号={}, 取消原因={}", id, order.getOrderNumber(), reason);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消采购订单异常：订单ID={}", id, e);
            throw new ServiceException("取消订单失败：" + e.getMessage());
        }
    }

    @Override
    public void updateLogisticsNumber(Long id, String logisticsNumber) {
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(id);
            if (order == null) {
                throw new ServiceException("采购订单不存在");
            }
            // 只有已发货或已确认的订单才能更新物流单号
            if (!"SHIPPED".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
                throw new ServiceException("只有待发货或已发货状态的订单才能更新物流单号");
            }
            if (!StringUtils.hasText(logisticsNumber)) {
                throw new ServiceException("物流单号不能为空");
            }
            order.setLogisticsNumber(logisticsNumber);
            // 如果订单是待发货状态，更新物流单号后自动变为已发货
            if ("CONFIRMED".equals(order.getStatus())) {
                order.setStatus("SHIPPED");
                order.setShipDate(java.time.LocalDateTime.now());
            }
            purchaseOrderMapper.updateById(order);
            log.info("更新物流单号：订单ID={}, 订单编号={}, 物流单号={}", id, order.getOrderNumber(), logisticsNumber);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新物流单号异常：订单ID={}", id, e);
            throw new ServiceException("更新物流单号失败：" + e.getMessage());
        }
    }

    // 发货后写入库提醒通知（失败不影响发货事务）
    private void createInboundShipNotice(PurchaseOrder order, Long id, String logisticsNumber) {
        try {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            String supplierName = supplier != null ? supplier.getName() : "未知供应商";
            String shipDateStr = order.getShipDate() != null
                    ? order.getShipDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "";
            String content = String.format(
                    "订单编号：%s\n供应商：%s\n物流单号：%s\n发货日期：%s\n\n请及时进行入库验收。",
                    order.getOrderNumber(),
                    supplierName,
                    logisticsNumber,
                    shipDateStr);

            SysNotice notice = new SysNotice();
            notice.setNoticeTitle("待入库提醒");
            notice.setNoticeContent(content);
            notice.setNoticeType(1);
            notice.setStatus(1);
            notice.setCreateBy(null);
            sysNoticeService.createNotice(notice);

            log.info("已创建待入库提醒通知：订单编号={}", order.getOrderNumber());
        } catch (Exception e) {
            log.warn("创建待入库提醒通知失败：订单ID={}, 错误={}", id, e.getMessage());
        }
    }

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

