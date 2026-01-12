package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.SysConfigMapper;
import com.cdiom.backend.model.SysConfig;
import com.cdiom.backend.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统参数配置服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public Page<SysConfig> getConfigList(Integer page, Integer size, String keyword, Integer configType) {
        Page<SysConfig> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysConfig::getConfigName, keyword)
                    .or().like(SysConfig::getConfigKey, keyword));
        }
        
        if (configType != null) {
            wrapper.eq(SysConfig::getConfigType, configType);
        }
        
        wrapper.orderByDesc(SysConfig::getCreateTime);
        
        return sysConfigMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SysConfig getConfigById(Long id) {
        return sysConfigMapper.selectById(id);
    }

    @Override
    public SysConfig getConfigByKey(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        return sysConfigMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfig createConfig(SysConfig config) {
        // 检查参数键名是否已存在
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, config.getConfigKey());
        if (sysConfigMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("参数键名已存在");
        }
        
        // 设置默认值
        if (config.getConfigType() == null) {
            config.setConfigType(1);
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        
        sysConfigMapper.insert(config);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfig updateConfig(SysConfig config) {
        SysConfig existConfig = sysConfigMapper.selectById(config.getId());
        if (existConfig == null) {
            throw new RuntimeException("参数配置不存在");
        }
        
        // 如果修改了参数键名，检查是否重复
        if (!existConfig.getConfigKey().equals(config.getConfigKey())) {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getConfigKey, config.getConfigKey());
            if (sysConfigMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("参数键名已存在");
            }
        }
        
        config.setUpdateTime(LocalDateTime.now());
        sysConfigMapper.updateById(config);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        sysConfigMapper.deleteById(id);
    }
}





