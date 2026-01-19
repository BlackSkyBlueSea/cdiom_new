package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.PriceWarningConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 价格预警配置Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface PriceWarningConfigMapper extends BaseMapper<PriceWarningConfig> {
}

