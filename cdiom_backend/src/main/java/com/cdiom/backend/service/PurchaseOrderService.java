package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;

import java.util.List;
import java.util.Map;

/**
 * 采购订单服务接口
 * 
 * @author cdiom
 */
public interface PurchaseOrderService {

    /**
     * 分页查询采购订单列表
     */
    Page<PurchaseOrder> getPurchaseOrderList(Integer page, Integer size, String keyword, Long supplierId, Long purchaserId, String status);

    /**
     * 根据ID查询采购订单
     */
    PurchaseOrder getPurchaseOrderById(Long id);

    /**
     * 根据订单编号查询采购订单（用于条形码扫描）
     */
    PurchaseOrder getPurchaseOrderByOrderNumber(String orderNumber);

    /**
     * 创建采购订单
     */
    PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items);

    /**
     * 更新采购订单
     */
    PurchaseOrder updatePurchaseOrder(PurchaseOrder purchaseOrder);

    /**
     * 删除采购订单
     */
    void deletePurchaseOrder(Long id);

    /**
     * 更新订单状态
     */
    void updateOrderStatus(Long id, String status, String reason);

    /**
     * 获取订单明细列表
     */
    List<PurchaseOrderItem> getOrderItems(Long orderId);

    /**
     * 获取订单明细的已入库数量
     */
    Map<Long, Integer> getInboundQuantitiesByOrder(Long orderId);

    /**
     * 检查订单是否可以入库
     */
    boolean canInbound(Long orderId);

    /**
     * 检查订单是否全部入库
     */
    boolean isOrderFullyInbound(Long orderId);

    /**
     * 更新订单入库状态（自动判断是否全部入库）
     */
    void updateOrderInboundStatus(Long orderId);
}

