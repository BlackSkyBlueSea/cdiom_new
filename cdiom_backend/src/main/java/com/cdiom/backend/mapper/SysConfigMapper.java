package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统参数配置Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}

