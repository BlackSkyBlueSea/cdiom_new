package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}

