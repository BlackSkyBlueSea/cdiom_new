package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SupplierApprovalLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商审批流程日志Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SupplierApprovalLogMapper extends BaseMapper<SupplierApprovalLog> {
}

