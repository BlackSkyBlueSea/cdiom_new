package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.Supplier;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}

