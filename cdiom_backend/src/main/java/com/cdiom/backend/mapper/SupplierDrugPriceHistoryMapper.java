package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SupplierDrugPriceHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商-药品价格历史记录Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SupplierDrugPriceHistoryMapper extends BaseMapper<SupplierDrugPriceHistory> {
}

