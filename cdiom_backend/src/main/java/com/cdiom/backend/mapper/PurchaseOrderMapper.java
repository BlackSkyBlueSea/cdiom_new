package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购订单Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
}



