package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.SysConfig;

/**
 * 系统参数配置服务接口
 * 
 * @author cdiom
 */
public interface SysConfigService {

    /**
     * 分页查询参数配置列表
     */
    Page<SysConfig> getConfigList(Integer page, Integer size, String keyword, Integer configType);

    /**
     * 根据ID查询参数配置
     */
    SysConfig getConfigById(Long id);

    /**
     * 根据键名查询参数配置
     */
    SysConfig getConfigByKey(String configKey);

    /**
     * 创建参数配置
     */
    SysConfig createConfig(SysConfig config);

    /**
     * 更新参数配置
     */
    SysConfig updateConfig(SysConfig config);

    /**
     * 删除参数配置（逻辑删除）
     */
    void deleteConfig(Long id);
}






