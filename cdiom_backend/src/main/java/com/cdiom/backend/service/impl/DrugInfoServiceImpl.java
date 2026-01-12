package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.service.DrugInfoService;
import com.cdiom.backend.service.JisuApiService;
import com.cdiom.backend.service.YuanyanyaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        // 检查国家本位码是否已存在
        if (StringUtils.hasText(drugInfo.getNationalCode())) {
            LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DrugInfo::getNationalCode, drugInfo.getNationalCode());
            if (drugInfoMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("国家本位码已存在");
            }
        }
        
        // 检查追溯码是否已存在
        if (StringUtils.hasText(drugInfo.getTraceCode())) {
            LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DrugInfo::getTraceCode, drugInfo.getTraceCode());
            if (drugInfoMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("药品追溯码已存在");
            }
        }
        
        // 设置默认值
        if (drugInfo.getIsSpecial() == null) {
            drugInfo.setIsSpecial(0);
        }
        if (!StringUtils.hasText(drugInfo.getUnit())) {
            drugInfo.setUnit("盒");
        }
        drugInfo.setCreateTime(LocalDateTime.now());
        drugInfo.setUpdateTime(LocalDateTime.now());
        
        drugInfoMapper.insert(drugInfo);
        return drugInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrugInfo updateDrugInfo(DrugInfo drugInfo) {
        DrugInfo existDrug = drugInfoMapper.selectById(drugInfo.getId());
        if (existDrug == null) {
            throw new RuntimeException("药品信息不存在");
        }
        
        // 如果修改了国家本位码，检查是否重复
        if (StringUtils.hasText(drugInfo.getNationalCode()) 
                && !drugInfo.getNationalCode().equals(existDrug.getNationalCode())) {
            LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DrugInfo::getNationalCode, drugInfo.getNationalCode());
            if (drugInfoMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("国家本位码已存在");
            }
        }
        
        // 如果修改了追溯码，检查是否重复
        if (StringUtils.hasText(drugInfo.getTraceCode()) 
                && !drugInfo.getTraceCode().equals(existDrug.getTraceCode())) {
            LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DrugInfo::getTraceCode, drugInfo.getTraceCode());
            if (drugInfoMapper.selectOne(wrapper) != null) {
                throw new RuntimeException("药品追溯码已存在");
            }
        }
        
        drugInfo.setUpdateTime(LocalDateTime.now());
        drugInfoMapper.updateById(drugInfo);
        return drugInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDrugInfo(Long id) {
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
}

