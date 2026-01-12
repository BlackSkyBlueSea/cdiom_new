package com.cdiom.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.annotation.RequiresPermission;
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
@RequiresPermission({"drug:view", "drug:manage"})
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

    /**
     * 根据商品码或本位码查询药品信息
     * 先查询本地数据库，如果未找到则查询极速数据API
     */
    @GetMapping("/search")
    public Result<DrugInfo> searchDrugByCode(@RequestParam String code) {
        try {
            DrugInfo drugInfo = drugInfoService.searchDrugByCode(code);
            if (drugInfo != null) {
                return Result.success("查询成功", drugInfo);
            } else {
                return Result.error("未找到药品信息");
            }
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据药品名称查询药品信息（调用万维易源API）
     */
    @GetMapping("/search/name")
    public Result<DrugInfo> searchDrugByName(@RequestParam String drugName) {
        try {
            DrugInfo drugInfo = drugInfoService.searchDrugByName(drugName);
            if (drugInfo != null) {
                return Result.success("查询成功", drugInfo);
            } else {
                return Result.error("未找到药品信息");
            }
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据批准文号查询药品信息（调用万维易源API）
     */
    @GetMapping("/search/approval")
    public Result<DrugInfo> searchDrugByApprovalNumber(@RequestParam String approvalNumber) {
        try {
            DrugInfo drugInfo = drugInfoService.searchDrugByApprovalNumber(approvalNumber);
            if (drugInfo != null) {
                return Result.success("查询成功", drugInfo);
            } else {
                return Result.error("未找到药品信息");
            }
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}


