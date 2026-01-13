package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.SupplierMapper;
import com.cdiom.backend.model.Supplier;
import com.cdiom.backend.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 供应商服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierMapper supplierMapper;

    @Override
    public Page<Supplier> getSupplierList(Integer page, Integer size, String keyword, Integer status, Integer auditStatus) {
        Page<Supplier> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Supplier::getName, keyword)
                    .or().like(Supplier::getContactPerson, keyword)
                    .or().like(Supplier::getPhone, keyword)
                    .or().like(Supplier::getCreditCode, keyword));
        }
        
        if (status != null) {
            wrapper.eq(Supplier::getStatus, status);
        }
        
        if (auditStatus != null) {
            wrapper.eq(Supplier::getAuditStatus, auditStatus);
        }
        
        wrapper.orderByDesc(Supplier::getCreateTime);
        
        return supplierMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Supplier getSupplierById(Long id) {
        return supplierMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Supplier createSupplier(Supplier supplier) {
        // 检查统一社会信用代码是否已存在
        if (StringUtils.hasText(supplier.getCreditCode())) {
            LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Supplier::getCreditCode, supplier.getCreditCode());
            if (supplierMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("统一社会信用代码已存在");
            }
        }
        
        // 设置默认值
        if (supplier.getStatus() == null) {
            supplier.setStatus(1); // 默认启用
        }
        if (supplier.getAuditStatus() == null) {
            supplier.setAuditStatus(0); // 默认待审核
        }
        
        supplierMapper.insert(supplier);
        return supplier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Supplier updateSupplier(Supplier supplier) {
        Supplier existing = supplierMapper.selectById(supplier.getId());
        if (existing == null) {
            throw new RuntimeException("供应商不存在");
        }
        
        // 如果修改了统一社会信用代码，检查是否重复
        if (StringUtils.hasText(supplier.getCreditCode()) 
                && !supplier.getCreditCode().equals(existing.getCreditCode())) {
            LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Supplier::getCreditCode, supplier.getCreditCode());
            if (supplierMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("统一社会信用代码已存在");
            }
        }
        
        supplierMapper.updateById(supplier);
        return supplier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        supplierMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierStatus(Long id, Integer status) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }
        supplier.setStatus(status);
        supplierMapper.updateById(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSupplier(Long id, Integer auditStatus, String auditReason, Long auditBy) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }
        
        supplier.setAuditStatus(auditStatus);
        supplier.setAuditReason(auditReason);
        supplier.setAuditBy(auditBy);
        supplier.setAuditTime(LocalDateTime.now());
        
        supplierMapper.updateById(supplier);
    }
}

