package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 采购订单明细Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    /**
     * 根据订单ID查询订单明细
     */
    @Select("SELECT * FROM purchase_order_item WHERE order_id = #{orderId}")
    List<PurchaseOrderItem> selectByOrderId(Long orderId);
}



