package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.SysConfig;
import com.cdiom.backend.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数配置控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
@RequiresPermission("config:manage")
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /**
     * 分页查询参数配置列表
     */
    @GetMapping
    public Result<Page<SysConfig>> getConfigList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer configType) {
        Page<SysConfig> configPage = sysConfigService.getConfigList(page, size, keyword, configType);
        return Result.success(configPage);
    }

    /**
     * 根据ID查询参数配置
     */
    @GetMapping("/{id}")
    public Result<SysConfig> getConfigById(@PathVariable Long id) {
        SysConfig config = sysConfigService.getConfigById(id);
        return Result.success(config);
    }

    /**
     * 根据键名查询参数配置
     */
    @GetMapping("/key/{configKey}")
    public Result<SysConfig> getConfigByKey(@PathVariable String configKey) {
        SysConfig config = sysConfigService.getConfigByKey(configKey);
        return Result.success(config);
    }

    /**
     * 创建参数配置
     */
    @PostMapping
    public Result<SysConfig> createConfig(@RequestBody SysConfig config) {
        try {
            SysConfig createdConfig = sysConfigService.createConfig(config);
            return Result.success("创建成功", createdConfig);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新参数配置
     */
    @PutMapping("/{id}")
    public Result<SysConfig> updateConfig(@PathVariable Long id, @RequestBody SysConfig config) {
        try {
            config.setId(id);
            SysConfig updatedConfig = sysConfigService.updateConfig(config);
            return Result.success("更新成功", updatedConfig);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除参数配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        try {
            sysConfigService.deleteConfig(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

