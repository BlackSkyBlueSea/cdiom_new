package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.SupplierDrugAgreementMapper;
import com.cdiom.backend.model.SupplierDrugAgreement;
import com.cdiom.backend.service.SupplierDrugAgreementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 供应商-药品价格协议服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierDrugAgreementServiceImpl implements SupplierDrugAgreementService {

    private final SupplierDrugAgreementMapper agreementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierDrugAgreement createAgreement(SupplierDrugAgreement agreement) {
        agreementMapper.insert(agreement);
        return agreement;
    }

    @Override
    public SupplierDrugAgreement getAgreementById(Long id) {
        return agreementMapper.selectById(id);
    }

    @Override
    public SupplierDrugAgreement getCurrentAgreement(Long supplierId, Long drugId) {
        LambdaQueryWrapper<SupplierDrugAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrugAgreement::getSupplierId, supplierId)
               .eq(SupplierDrugAgreement::getDrugId, drugId)
               .eq(SupplierDrugAgreement::getDeleted, 0)
               .le(SupplierDrugAgreement::getEffectiveDate, LocalDate.now())
               .and(w -> w.ge(SupplierDrugAgreement::getExpiryDate, LocalDate.now())
                          .or()
                          .isNull(SupplierDrugAgreement::getExpiryDate))
               .orderByDesc(SupplierDrugAgreement::getEffectiveDate)
               .last("LIMIT 1");
        return agreementMapper.selectOne(wrapper);
    }

    @Override
    public List<SupplierDrugAgreement> getAgreementsBySupplierAndDrug(Long supplierId, Long drugId) {
        LambdaQueryWrapper<SupplierDrugAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierDrugAgreement::getSupplierId, supplierId)
               .eq(SupplierDrugAgreement::getDrugId, drugId)
               .eq(SupplierDrugAgreement::getDeleted, 0)
               .orderByDesc(SupplierDrugAgreement::getEffectiveDate);
        return agreementMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierDrugAgreement updateAgreement(SupplierDrugAgreement agreement) {
        agreementMapper.updateById(agreement);
        return agreement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgreement(Long id) {
        SupplierDrugAgreement agreement = agreementMapper.selectById(id);
        if (agreement != null) {
            agreement.setDeleted(1);
            agreementMapper.updateById(agreement);
        }
    }
}

