package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.DrugInfoService;
import com.cdiom.backend.service.JisuApiService;
import com.cdiom.backend.service.SupplierDrugService;
import com.cdiom.backend.service.YuanyanyaoService;
import com.cdiom.backend.common.exception.ServiceException;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 药品信息服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class DrugInfoServiceImpl implements DrugInfoService {

    private final DrugInfoMapper drugInfoMapper;
    private final YuanyanyaoService yuanyanyaoService;
    private final JisuApiService jisuApiService;
    private final SupplierDrugService supplierDrugService;
    private final AuthService authService;

    @Override
    public Page<DrugInfo> getDrugInfoList(Integer page, Integer size, String keyword, Integer isSpecial) {
        Page<DrugInfo> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DrugInfo::getDrugName, keyword)
                    .or().like(DrugInfo::getNationalCode, keyword)
                    .or().like(DrugInfo::getApprovalNumber, keyword)
                    .or().like(DrugInfo::getManufacturer, keyword));
        }
        
        if (isSpecial != null) {
            wrapper.eq(DrugInfo::getIsSpecial, isSpecial);
        }
        
        wrapper.orderByDesc(DrugInfo::getCreateTime);
        
        return drugInfoMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public DrugInfo getDrugInfoById(Long id) {
        return drugInfoMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrugInfo createDrugInfo(DrugInfo drugInfo) {
        // 获取当前用户
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("未登录或登录已过期");
        }
        
        // ========== 必填字段验证 ==========
        if (!StringUtils.hasText(drugInfo.getNationalCode())) {
            throw new ServiceException("国家本位码不能为空");
        }
        if (!StringUtils.hasText(drugInfo.getDrugName())) {
            throw new ServiceException("药品名称不能为空");
        }
        
        // ========== 字段长度验证 ==========
        if (drugInfo.getNationalCode() != null && drugInfo.getNationalCode().length() > 50) {
            throw new ServiceException("国家本位码长度不能超过50个字符");
        }
        if (drugInfo.getTraceCode() != null && drugInfo.getTraceCode().length() > 100) {
            throw new ServiceException("药品追溯码长度不能超过100个字符");
        }
        if (drugInfo.getDrugName() != null && drugInfo.getDrugName().length() > 200) {
            throw new ServiceException("药品名称长度不能超过200个字符");
        }
        if (drugInfo.getProductCode() != null && drugInfo.getProductCode().length() > 100) {
            throw new ServiceException("商品码长度不能超过100个字符");
        }
        if (drugInfo.getUnit() != null && drugInfo.getUnit().length() > 20) {
            throw new ServiceException("单位长度不能超过20个字符");
        }
        
        // ========== 唯一性验证 ==========
        // 检查国家本位码是否已存在
        LambdaQueryWrapper<DrugInfo> nationalCodeWrapper = new LambdaQueryWrapper<>();
        nationalCodeWrapper.eq(DrugInfo::getNationalCode, drugInfo.getNationalCode());
        if (drugInfoMapper.selectOne(nationalCodeWrapper) != null) {
            throw new ServiceException("国家本位码已存在，请勿重复添加");
        }
        
        // 检查追溯码是否已存在（如果提供了追溯码）
        if (StringUtils.hasText(drugInfo.getTraceCode())) {
            LambdaQueryWrapper<DrugInfo> traceCodeWrapper = new LambdaQueryWrapper<>();
            traceCodeWrapper.eq(DrugInfo::getTraceCode, drugInfo.getTraceCode());
            if (drugInfoMapper.selectOne(traceCodeWrapper) != null) {
                throw new ServiceException("药品追溯码已存在，请勿重复添加");
            }
        }
        
        // ========== 日期验证 ==========
        // 如果提供了有效期，验证有效期不能是过去日期
        if (drugInfo.getExpiryDate() != null) {
            LocalDate today = LocalDate.now();
            if (drugInfo.getExpiryDate().isBefore(today)) {
                throw new ServiceException("有效期不能是过去日期");
            }
        }
        
        // ========== 业务逻辑验证 ==========
        // 验证特殊药品标识的有效值
        if (drugInfo.getIsSpecial() != null && drugInfo.getIsSpecial() != 0 && drugInfo.getIsSpecial() != 1) {
            throw new ServiceException("特殊药品标识值无效，只能为0（普通药品）或1（特殊药品）");
        }
        
        // ========== 设置默认值 ==========
        if (drugInfo.getIsSpecial() == null) {
            drugInfo.setIsSpecial(0); // 默认为普通药品
        }
        if (!StringUtils.hasText(drugInfo.getUnit())) {
            drugInfo.setUnit("盒"); // 默认单位为"盒"
        }
        
        // ========== 设置审计字段 ==========
        drugInfo.setCreateBy(currentUser.getId());
        drugInfo.setCreateTime(LocalDateTime.now());
        drugInfo.setUpdateTime(LocalDateTime.now());
        
        // ========== 保存数据 ==========
        drugInfoMapper.insert(drugInfo);
        return drugInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrugInfo updateDrugInfo(DrugInfo drugInfo) {
        // 验证ID是否存在
        if (drugInfo.getId() == null) {
            throw new ServiceException("药品ID不能为空");
        }
        
        DrugInfo existDrug = drugInfoMapper.selectById(drugInfo.getId());
        if (existDrug == null) {
            throw new ServiceException("药品信息不存在");
        }
        
        // ========== 必填字段验证 ==========
        if (!StringUtils.hasText(drugInfo.getNationalCode())) {
            throw new ServiceException("国家本位码不能为空");
        }
        if (!StringUtils.hasText(drugInfo.getDrugName())) {
            throw new ServiceException("药品名称不能为空");
        }
        
        // ========== 字段长度验证 ==========
        if (drugInfo.getNationalCode() != null && drugInfo.getNationalCode().length() > 50) {
            throw new ServiceException("国家本位码长度不能超过50个字符");
        }
        if (drugInfo.getTraceCode() != null && drugInfo.getTraceCode().length() > 100) {
            throw new ServiceException("药品追溯码长度不能超过100个字符");
        }
        if (drugInfo.getDrugName() != null && drugInfo.getDrugName().length() > 200) {
            throw new ServiceException("药品名称长度不能超过200个字符");
        }
        if (drugInfo.getProductCode() != null && drugInfo.getProductCode().length() > 100) {
            throw new ServiceException("商品码长度不能超过100个字符");
        }
        if (drugInfo.getUnit() != null && drugInfo.getUnit().length() > 20) {
            throw new ServiceException("单位长度不能超过20个字符");
        }
        
        // ========== 唯一性验证 ==========
        // 如果修改了国家本位码，检查是否重复
        if (!drugInfo.getNationalCode().equals(existDrug.getNationalCode())) {
            LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DrugInfo::getNationalCode, drugInfo.getNationalCode());
            DrugInfo duplicate = drugInfoMapper.selectOne(wrapper);
            if (duplicate != null && !duplicate.getId().equals(drugInfo.getId())) {
                throw new ServiceException("国家本位码已存在，请勿重复使用");
            }
        }
        
        // 如果修改了追溯码，检查是否重复（如果提供了追溯码）
        if (StringUtils.hasText(drugInfo.getTraceCode()) 
                && !drugInfo.getTraceCode().equals(existDrug.getTraceCode())) {
            LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DrugInfo::getTraceCode, drugInfo.getTraceCode());
            DrugInfo duplicate = drugInfoMapper.selectOne(wrapper);
            if (duplicate != null && !duplicate.getId().equals(drugInfo.getId())) {
                throw new ServiceException("药品追溯码已存在，请勿重复使用");
            }
        }
        
        // ========== 日期验证 ==========
        // 如果提供了有效期，验证有效期不能是过去日期
        if (drugInfo.getExpiryDate() != null) {
            LocalDate today = LocalDate.now();
            if (drugInfo.getExpiryDate().isBefore(today)) {
                throw new ServiceException("有效期不能是过去日期");
            }
        }
        
        // ========== 业务逻辑验证 ==========
        // 验证特殊药品标识的有效值
        if (drugInfo.getIsSpecial() != null && drugInfo.getIsSpecial() != 0 && drugInfo.getIsSpecial() != 1) {
            throw new ServiceException("特殊药品标识值无效，只能为0（普通药品）或1（特殊药品）");
        }
        
        // ========== 设置默认值 ==========
        if (drugInfo.getIsSpecial() == null) {
            drugInfo.setIsSpecial(existDrug.getIsSpecial() != null ? existDrug.getIsSpecial() : 0);
        }
        if (!StringUtils.hasText(drugInfo.getUnit())) {
            drugInfo.setUnit(StringUtils.hasText(existDrug.getUnit()) ? existDrug.getUnit() : "盒");
        }
        
        // ========== 设置审计字段 ==========
        // 保留原有的创建信息
        drugInfo.setCreateBy(existDrug.getCreateBy());
        drugInfo.setCreateTime(existDrug.getCreateTime());
        drugInfo.setUpdateTime(LocalDateTime.now());
        
        // ========== 更新数据 ==========
        drugInfoMapper.updateById(drugInfo);
        return drugInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDrugInfo(Long id) {
        if (id == null) {
            throw new ServiceException("药品ID不能为空");
        }
        
        DrugInfo existDrug = drugInfoMapper.selectById(id);
        if (existDrug == null) {
            throw new ServiceException("药品信息不存在");
        }
        
        // 执行逻辑删除
        drugInfoMapper.deleteById(id);
    }

    @Override
    public DrugInfo searchDrugByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }

        // 先查询本地数据库
        LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(DrugInfo::getProductCode, code)
                .or().eq(DrugInfo::getNationalCode, code)
                .or().eq(DrugInfo::getTraceCode, code)
                .or().like(DrugInfo::getApprovalNumber, code));
        DrugInfo localDrug = drugInfoMapper.selectOne(wrapper);
        
        if (localDrug != null) {
            return localDrug;
        }

        // 如果本地数据库未找到，调用极速数据API（根据商品码查询）
        DrugInfo jisuDrug = jisuApiService.searchByProductCode(code);
        if (jisuDrug != null) {
            return jisuDrug;
        }
        
        return null;
    }

    @Override
    public DrugInfo searchDrugByName(String drugName) {
        if (!StringUtils.hasText(drugName)) {
            return null;
        }
        
        // 先调用万维易源API获取基本信息
        DrugInfo drugInfo = yuanyanyaoService.searchByDrugName(drugName.trim());
        if (drugInfo == null) {
            return null;
        }
        
        // 如果获取到了批准文号，再调用极速数据API补充信息
        if (StringUtils.hasText(drugInfo.getApprovalNumber())) {
            DrugInfo jisuDrugInfo = jisuApiService.searchByApprovalNumber(drugInfo.getApprovalNumber());
            if (jisuDrugInfo != null) {
                // 合并信息：极速数据API补充的信息优先
                mergeDrugInfo(drugInfo, jisuDrugInfo);
            }
        }
        
        return drugInfo;
    }

    @Override
    public DrugInfo searchDrugByApprovalNumber(String approvalNumber) {
        if (!StringUtils.hasText(approvalNumber)) {
            return null;
        }
        
        // 先调用万维易源API获取基本信息
        DrugInfo drugInfo = yuanyanyaoService.searchByApprovalNumber(approvalNumber.trim());
        if (drugInfo == null) {
            return null;
        }
        
        // 再调用极速数据API补充信息
        DrugInfo jisuDrugInfo = jisuApiService.searchByApprovalNumber(approvalNumber.trim());
        if (jisuDrugInfo != null) {
            // 合并信息：极速数据API补充的信息优先
            mergeDrugInfo(drugInfo, jisuDrugInfo);
        }
        
        return drugInfo;
    }

    /**
     * 合并药品信息
     * 将极速数据API返回的信息合并到万维易源API返回的信息中
     * 极速数据API的字段优先（用于补充国家本位码、条形码、描述、规格）
     */
    private void mergeDrugInfo(DrugInfo target, DrugInfo source) {
        // 补充国家本位码（如果万维易源API没有）
        if (!StringUtils.hasText(target.getNationalCode()) && StringUtils.hasText(source.getNationalCode())) {
            target.setNationalCode(source.getNationalCode());
        }
        
        // 补充描述/说明书（如果万维易源API没有）
        if (!StringUtils.hasText(target.getDescription()) && StringUtils.hasText(source.getDescription())) {
            target.setDescription(source.getDescription());
        }
        
        // 用极速数据API的规格替换（根据用户需求）
        if (StringUtils.hasText(source.getSpecification())) {
            target.setSpecification(source.getSpecification());
        }
        
        // 补充商品码/条形码（如果万维易源API没有）
        if (!StringUtils.hasText(target.getProductCode()) && StringUtils.hasText(source.getProductCode())) {
            target.setProductCode(source.getProductCode());
        }
        
        // 如果万维易源API没有剂型，使用极速数据API的剂型
        if (!StringUtils.hasText(target.getDosageForm()) && StringUtils.hasText(source.getDosageForm())) {
            target.setDosageForm(source.getDosageForm());
        }
    }

    @Override
    public Page<DrugInfo> getDrugInfoListBySupplierId(Long supplierId, Integer page, Integer size, String keyword) {
        // 先获取该供应商提供的药品ID列表
        List<Long> drugIds = supplierDrugService.getDrugIdsBySupplierId(supplierId);
        
        if (drugIds.isEmpty()) {
            // 如果没有关联的药品，返回空分页
            return new Page<>(page, size, 0);
        }
        
        // 根据药品ID列表查询药品信息
        Page<DrugInfo> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DrugInfo::getId, drugIds);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DrugInfo::getDrugName, keyword)
                    .or().like(DrugInfo::getNationalCode, keyword)
                    .or().like(DrugInfo::getApprovalNumber, keyword)
                    .or().like(DrugInfo::getManufacturer, keyword));
        }
        
        wrapper.orderByDesc(DrugInfo::getCreateTime);
        
        return drugInfoMapper.selectPage(pageParam, wrapper);
    }
}

