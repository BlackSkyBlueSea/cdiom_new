package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}

