package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.service.DrugInfoService;
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
}

