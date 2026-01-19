package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.SupplierDrugMapper;
import com.cdiom.backend.model.SupplierDrug;
import com.cdiom.backend.model.SupplierDrugPriceHistory;
import com.cdiom.backend.service.SupplierDrugService;
import com.cdiom.backend.service.SupplierDrugPriceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商-药品关联服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierDrugServiceImpl implements SupplierDrugService {

    private final SupplierDrugMapper supplierDrugMapper;
    private final SupplierDrugPriceHistoryService priceHistoryService;

    @Override
    public List<Long> getDrugIdsBySupplierId(Long supplierId) {
        LambdaQueryWrapper<SupplierDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrug::getSupplierId, supplierId)
               .eq(SupplierDrug::getIsActive, 1);
        List<SupplierDrug> list = supplierDrugMapper.selectList(wrapper);
        return list.stream()
                   .map(SupplierDrug::getDrugId)
                   .collect(Collectors.toList());
    }

    @Override
    public List<Long> getSupplierIdsByDrugId(Long drugId) {
        LambdaQueryWrapper<SupplierDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrug::getDrugId, drugId)
               .eq(SupplierDrug::getIsActive, 1);
        List<SupplierDrug> list = supplierDrugMapper.selectList(wrapper);
        return list.stream()
                   .map(SupplierDrug::getSupplierId)
                   .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierDrug addSupplierDrug(Long supplierId, Long drugId, BigDecimal unitPrice, Long createBy) {
        // 检查是否已存在
        LambdaQueryWrapper<SupplierDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrug::getSupplierId, supplierId)
               .eq(SupplierDrug::getDrugId, drugId);
        SupplierDrug existing = supplierDrugMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 如果已存在但已删除，恢复并更新
            if (existing.getDeleted() == 1) {
                existing.setDeleted(0);
                existing.setIsActive(1);
                existing.setUnitPrice(unitPrice);
                supplierDrugMapper.updateById(existing);
                return existing;
            } else {
                throw new RuntimeException("该供应商-药品关联已存在");
            }
        }
        
        // 创建新关联
        SupplierDrug supplierDrug = new SupplierDrug();
        supplierDrug.setSupplierId(supplierId);
        supplierDrug.setDrugId(drugId);
        supplierDrug.setUnitPrice(unitPrice);
        supplierDrug.setIsActive(1);
        supplierDrug.setCreateBy(createBy);
        
        supplierDrugMapper.insert(supplierDrug);
        return supplierDrug;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSupplierDrug(Long supplierId, Long drugId) {
        LambdaQueryWrapper<SupplierDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrug::getSupplierId, supplierId)
               .eq(SupplierDrug::getDrugId, drugId);
        supplierDrugMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierDrug updateSupplierDrugPrice(Long supplierId, Long drugId, BigDecimal unitPrice, 
                                                 Long agreementId, String changeReason, Long operatorId, 
                                                 String operatorName, String ipAddress) {
        LambdaQueryWrapper<SupplierDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrug::getSupplierId, supplierId)
               .eq(SupplierDrug::getDrugId, drugId);
        SupplierDrug supplierDrug = supplierDrugMapper.selectOne(wrapper);
        
        if (supplierDrug == null) {
            throw new RuntimeException("供应商-药品关联不存在");
        }
        
        // 记录变更前价格
        BigDecimal priceBefore = supplierDrug.getUnitPrice();
        
        // 更新价格
        supplierDrug.setUnitPrice(unitPrice);
        if (agreementId != null) {
            supplierDrug.setCurrentAgreementId(agreementId);
        }
        supplierDrugMapper.updateById(supplierDrug);
        
        // 记录价格变更历史
        SupplierDrugPriceHistory history = new SupplierDrugPriceHistory();
        history.setSupplierId(supplierId);
        history.setDrugId(drugId);
        history.setAgreementId(agreementId);
        history.setPriceBefore(priceBefore);
        history.setPriceAfter(unitPrice);
        history.setChangeReason(changeReason);
        history.setChangeType(agreementId != null ? "AGREEMENT" : "MANUAL");
        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);
        history.setIpAddress(ipAddress);
        priceHistoryService.recordPriceChange(history);
        
        log.info("价格更新成功: supplierId={}, drugId={}, priceBefore={}, priceAfter={}, agreementId={}", 
                supplierId, drugId, priceBefore, unitPrice, agreementId);
        
        return supplierDrug;
    }
    
    /**
     * 更新供应商-药品关联的单价（兼容旧接口）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierDrug updateSupplierDrugPrice(Long supplierId, Long drugId, BigDecimal unitPrice) {
        return updateSupplierDrugPrice(supplierId, drugId, unitPrice, null, null, null, null, null);
    }

    @Override
    public SupplierDrug getSupplierDrug(Long supplierId, Long drugId) {
        LambdaQueryWrapper<SupplierDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrug::getSupplierId, supplierId)
               .eq(SupplierDrug::getDrugId, drugId);
        return supplierDrugMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddSupplierDrugs(Long supplierId, List<Long> drugIds, Long createBy) {
        for (Long drugId : drugIds) {
            try {
                addSupplierDrug(supplierId, drugId, null, createBy);
            } catch (Exception e) {
                log.warn("添加供应商-药品关联失败: supplierId={}, drugId={}, error={}", 
                        supplierId, drugId, e.getMessage());
            }
        }
    }
}




