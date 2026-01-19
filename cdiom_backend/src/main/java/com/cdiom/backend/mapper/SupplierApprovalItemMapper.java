package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SupplierApprovalItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商准入审批明细Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SupplierApprovalItemMapper extends BaseMapper<SupplierApprovalItem> {
}

