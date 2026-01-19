package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import com.cdiom.backend.model.SupplierDrugPriceHistory;
import com.cdiom.backend.service.SupplierDrugPriceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商-药品价格历史记录控制器
 * 
 * @author cdiom
 */
@RestController
@RequestMapping("/api/v1/supplier-drug-price-history")
@RequiredArgsConstructor
@RequiresPermission({"price:history:view"})
public class SupplierDrugPriceHistoryController {

    private final SupplierDrugPriceHistoryService priceHistoryService;

    /**
     * 根据供应商ID和药品ID查询价格历史
     */
    @GetMapping("/list")
    public Result<List<SupplierDrugPriceHistory>> getPriceHistory(
            @RequestParam Long supplierId,
            @RequestParam Long drugId) {
        try {
            List<SupplierDrugPriceHistory> history = priceHistoryService.getPriceHistory(supplierId, drugId);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据协议ID查询价格历史
     */
    @GetMapping("/agreement/{agreementId}")
    public Result<List<SupplierDrugPriceHistory>> getPriceHistoryByAgreement(
            @PathVariable Long agreementId) {
        try {
            List<SupplierDrugPriceHistory> history = priceHistoryService.getPriceHistoryByAgreement(agreementId);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

