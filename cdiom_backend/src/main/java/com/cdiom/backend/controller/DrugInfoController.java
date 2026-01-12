package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.service.DrugInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 药品信息管理控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/drugs")
@RequiredArgsConstructor
public class DrugInfoController {

    private final DrugInfoService drugInfoService;

    /**
     * 分页查询药品信息列表
     */
    @GetMapping
    public Result<Page<DrugInfo>> getDrugInfoList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isSpecial) {
        Page<DrugInfo> drugPage = drugInfoService.getDrugInfoList(page, size, keyword, isSpecial);
        return Result.success(drugPage);
    }

    /**
     * 根据ID查询药品信息
     */
    @GetMapping("/{id}")
    public Result<DrugInfo> getDrugInfoById(@PathVariable Long id) {
        DrugInfo drugInfo = drugInfoService.getDrugInfoById(id);
        return Result.success(drugInfo);
    }

    /**
     * 创建药品信息
     */
    @PostMapping
    public Result<DrugInfo> createDrugInfo(@RequestBody DrugInfo drugInfo) {
        try {
            DrugInfo createdDrug = drugInfoService.createDrugInfo(drugInfo);
            return Result.success("创建成功", createdDrug);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新药品信息
     */
    @PutMapping("/{id}")
    public Result<DrugInfo> updateDrugInfo(@PathVariable Long id, @RequestBody DrugInfo drugInfo) {
        try {
            drugInfo.setId(id);
            DrugInfo updatedDrug = drugInfoService.updateDrugInfo(drugInfo);
            return Result.success("更新成功", updatedDrug);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除药品信息
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDrugInfo(@PathVariable Long id) {
        try {
            drugInfoService.deleteDrugInfo(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

