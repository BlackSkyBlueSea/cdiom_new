package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SupplierBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商黑名单Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SupplierBlacklistMapper extends BaseMapper<SupplierBlacklist> {
}

