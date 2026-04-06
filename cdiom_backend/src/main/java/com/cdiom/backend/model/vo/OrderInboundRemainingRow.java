package com.cdiom.backend.model.vo;

import lombok.Data;

/**
 * 某采购订单下，各药品订单量、已占用到货量、剩余可入库量（与 committed-quantity 口径一致）
 */
@Data
public class OrderInboundRemainingRow {

    private Long drugId;
    private String drugName;
    private String specification;
    private Integer orderedQuantity;
    /** 合格（已入账或待第二人确认）+ 不合格登记数量 */
    private Integer committedQuantity;
    /** max(0, 订单数量 - 已占用) */
    private Integer remainingQuantity;
}
