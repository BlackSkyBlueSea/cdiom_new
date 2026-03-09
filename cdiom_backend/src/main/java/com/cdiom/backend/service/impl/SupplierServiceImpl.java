package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.exception.ServiceException;
import com.cdiom.backend.mapper.PurchaseOrderMapper;
import com.cdiom.backend.mapper.SupplierDrugMapper;
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
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SupplierDrugMapper supplierDrugMapper;

    @Override
    public Page<Supplier> getSupplierList(Integer page, Integer size, String keyword, Integer status, Integer auditStatus) {
        Page<Supplier> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        
        // 逻辑删除过滤（MyBatis-Plus会自动处理@TableLogic注解）
        
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
    public Supplier getSupplierByCreateBy(Long createBy) {
        if (createBy == null) return null;
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getCreateBy, createBy);
        wrapper.last("LIMIT 1");
        return supplierMapper.selectOne(wrapper);
    }

    @Override
    public Supplier findSupplierForUser(Long userId, String phone) {
        // 1. 先按创建人匹配（兼容老逻辑）
        if (userId != null) {
            LambdaQueryWrapper<Supplier> byCreator = new LambdaQueryWrapper<>();
            byCreator.eq(Supplier::getCreateBy, userId);
            byCreator.eq(Supplier::getDeleted, 0);
            byCreator.last("LIMIT 1");
            Supplier s = supplierMapper.selectOne(byCreator);
            if (s != null) {
                return s;
            }
        }

        // 2. 再按手机号匹配：供应商联系人电话 == 当前用户手机号
        if (StringUtils.hasText(phone)) {
            LambdaQueryWrapper<Supplier> byPhone = new LambdaQueryWrapper<>();
            byPhone.eq(Supplier::getPhone, phone);
            byPhone.eq(Supplier::getDeleted, 0);
            byPhone.last("LIMIT 1");
            return supplierMapper.selectOne(byPhone);
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Supplier createSupplier(Supplier supplier) {
        // 检查统一社会信用代码是否已存在
        if (StringUtils.hasText(supplier.getCreditCode())) {
            LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Supplier::getCreditCode, supplier.getCreditCode());
            if (supplierMapper.selectOne(wrapper) != null) {
                throw new ServiceException("统一社会信用代码已存在");
            }
        }
        
        // 设置默认值：新建供应商默认待审核、禁用，审核通过后再启用
        if (supplier.getStatus() == null) {
            supplier.setStatus(0); // 默认禁用，审核通过后改为启用
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
            throw new ServiceException("供应商不存在");
        }
        
        // 如果修改了统一社会信用代码，检查是否重复
        if (StringUtils.hasText(supplier.getCreditCode()) 
                && !supplier.getCreditCode().equals(existing.getCreditCode())) {
            LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Supplier::getCreditCode, supplier.getCreditCode());
            if (supplierMapper.selectOne(wrapper) != null) {
                throw new ServiceException("统一社会信用代码已存在");
            }
        }
        
        // 状态仅允许 0-禁用/1-启用，不允许 2（待审核由 auditStatus 表示）
        if (supplier.getStatus() != null && supplier.getStatus() != 0 && supplier.getStatus() != 1) {
            throw new ServiceException("状态只能为启用(1)或禁用(0)");
        }
        
        supplierMapper.updateById(supplier);
        return supplier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new ServiceException("供应商不存在");
        }
        
        // 检查是否有采购订单关联
        LambdaQueryWrapper<com.cdiom.backend.model.PurchaseOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(com.cdiom.backend.model.PurchaseOrder::getSupplierId, id);
        Long orderCount = purchaseOrderMapper.selectCount(orderWrapper);
        if (orderCount > 0) {
            throw new ServiceException("该供应商存在关联的采购订单，无法删除");
        }
        
        // 检查是否有供应商-药品关联（通过中间表）
        LambdaQueryWrapper<com.cdiom.backend.model.SupplierDrug> supplierDrugWrapper = new LambdaQueryWrapper<>();
        supplierDrugWrapper.eq(com.cdiom.backend.model.SupplierDrug::getSupplierId, id);
        Long supplierDrugCount = supplierDrugMapper.selectCount(supplierDrugWrapper);
        if (supplierDrugCount > 0) {
            throw new ServiceException("该供应商存在关联的药品信息，无法删除");
        }
        
        // 执行逻辑删除
        supplierMapper.deleteById(id);
        log.info("供应商删除成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierStatus(Long id, Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new ServiceException("状态只能为启用(1)或禁用(0)");
        }
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new ServiceException("供应商不存在");
        }
        supplier.setStatus(status);
        supplierMapper.updateById(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSupplier(Long id, Integer auditStatus, String auditReason, Long auditBy) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new ServiceException("供应商不存在");
        }
        
        supplier.setAuditStatus(auditStatus);
        supplier.setAuditReason(auditReason);
        supplier.setAuditBy(auditBy);
        supplier.setAuditTime(LocalDateTime.now());
        // 审核通过时启用，驳回时禁用，与审核状态保持一致
        if (auditStatus != null) {
            if (auditStatus == 1) {
                supplier.setStatus(1); // 已通过 -> 启用
            } else if (auditStatus == 2) {
                supplier.setStatus(0); // 已驳回 -> 禁用
            }
        }
        
        supplierMapper.updateById(supplier);
    }
}

