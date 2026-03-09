package com.cdiom.backend.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商-药品信息（含协议价），用于列表展示与采购订单带价
 */
@Data
public class SupplierDrugVO {

    /** 药品ID */
    private Long id;
    /** 药品名称 */
    private String drugName;
    /** 规格 */
    private String specification;
    /** 国家本位码 */
    private String nationalCode;
    /** 剂型 */
    private String dosageForm;
    /** 批准文号 */
    private String approvalNumber;
    /** 生产厂家 */
    private String manufacturer;
    /** 单位 */
    private String unit;
    /** 该供应商对该药品的协议价/单价 */
    private BigDecimal unitPrice;
    /** 供应商-药品关联表主键（供应商维护时用于编辑/删除） */
    private Long supplierDrugId;
}
