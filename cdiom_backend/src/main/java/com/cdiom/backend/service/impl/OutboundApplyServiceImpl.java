package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.OutboundApplyItemMapper;
import com.cdiom.backend.mapper.OutboundApplyMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.model.OutboundApply;
import com.cdiom.backend.model.OutboundApplyItem;
import com.cdiom.backend.service.InventoryService;
import com.cdiom.backend.service.OutboundApplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 出库申请服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundApplyServiceImpl implements OutboundApplyService {

    private final OutboundApplyMapper outboundApplyMapper;
    private final OutboundApplyItemMapper outboundApplyItemMapper;
    private final DrugInfoMapper drugInfoMapper;
    private final InventoryService inventoryService;

    @Override
    public Page<OutboundApply> getOutboundApplyList(Integer page, Integer size, String keyword, Long applicantId, Long approverId, String department, String status, LocalDate startDate, LocalDate endDate) {
        Page<OutboundApply> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OutboundApply> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OutboundApply::getApplyNumber, keyword)
                    .or().like(OutboundApply::getDepartment, keyword)
                    .or().like(OutboundApply::getPurpose, keyword));
        }
        
        if (applicantId != null) {
            wrapper.eq(OutboundApply::getApplicantId, applicantId);
        }
        
        if (approverId != null) {
            wrapper.eq(OutboundApply::getApproverId, approverId);
        }
        
        if (StringUtils.hasText(department)) {
            wrapper.like(OutboundApply::getDepartment, department);
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(OutboundApply::getStatus, status);
        }
        
        if (startDate != null) {
            wrapper.ge(OutboundApply::getCreateTime, startDate.atStartOfDay());
        }
        
        if (endDate != null) {
            wrapper.le(OutboundApply::getCreateTime, endDate.plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(OutboundApply::getCreateTime);
        
        return outboundApplyMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public OutboundApply getOutboundApplyById(Long id) {
        return outboundApplyMapper.selectById(id);
    }

    @Override
    public OutboundApply getOutboundApplyByApplyNumber(String applyNumber) {
        LambdaQueryWrapper<OutboundApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboundApply::getApplyNumber, applyNumber);
        return outboundApplyMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundApply createOutboundApply(OutboundApply outboundApply, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("出库申请明细不能为空");
        }
        
        // 生成申领单号
        String applyNumber = generateApplyNumber();
        outboundApply.setApplyNumber(applyNumber);
        outboundApply.setStatus("PENDING");
        
        // 保存申请
        outboundApplyMapper.insert(outboundApply);
        
        // 保存申请明细
        for (Map<String, Object> item : items) {
            OutboundApplyItem applyItem = new OutboundApplyItem();
            applyItem.setApplyId(outboundApply.getId());
            applyItem.setDrugId(Long.valueOf(item.get("drugId").toString()));
            if (item.get("batchNumber") != null) {
                applyItem.setBatchNumber(item.get("batchNumber").toString());
            }
            applyItem.setQuantity(Integer.valueOf(item.get("quantity").toString()));
            if (item.get("remark") != null) {
                applyItem.setRemark(item.get("remark").toString());
            }
            outboundApplyItemMapper.insert(applyItem);
        }
        
        log.info("创建出库申请：申领单号={}, 申请人ID={}", applyNumber, outboundApply.getApplicantId());
        
        return outboundApply;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOutboundApply(Long id, Long approverId, Long secondApproverId) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("出库申请不存在");
        }
        
        if (!"PENDING".equals(apply.getStatus())) {
            throw new RuntimeException("申请状态不是待审批，无法审批");
        }
        
        // 检查是否包含特殊药品，需要第二审批人
        List<OutboundApplyItem> items = outboundApplyItemMapper.selectByApplyId(id);
        boolean hasSpecialDrug = false;
        for (OutboundApplyItem item : items) {
            DrugInfo drug = drugInfoMapper.selectById(item.getDrugId());
            if (drug != null && drug.getIsSpecial() != null && drug.getIsSpecial() == 1) {
                hasSpecialDrug = true;
                break;
            }
        }
        
        if (hasSpecialDrug && secondApproverId == null) {
            throw new RuntimeException("申请包含特殊药品，需要第二审批人确认");
        }
        
        apply.setStatus("APPROVED");
        apply.setApproverId(approverId);
        apply.setSecondApproverId(secondApproverId);
        apply.setApproveTime(LocalDateTime.now());
        
        outboundApplyMapper.updateById(apply);
        
        log.info("审批通过出库申请：申请ID={}, 审批人ID={}", id, approverId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOutboundApply(Long id, Long approverId, String rejectReason) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("出库申请不存在");
        }
        
        if (!"PENDING".equals(apply.getStatus())) {
            throw new RuntimeException("申请状态不是待审批，无法驳回");
        }
        
        apply.setStatus("REJECTED");
        apply.setApproverId(approverId);
        apply.setRejectReason(rejectReason);
        apply.setApproveTime(LocalDateTime.now());
        
        outboundApplyMapper.updateById(apply);
        
        log.info("驳回出库申请：申请ID={}, 审批人ID={}, 驳回理由={}", id, approverId, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeOutbound(Long id, List<Map<String, Object>> outboundItems) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("出库申请不存在");
        }
        
        if (!"APPROVED".equals(apply.getStatus())) {
            throw new RuntimeException("申请状态不是已通过，无法出库");
        }
        
        List<OutboundApplyItem> items = outboundApplyItemMapper.selectByApplyId(id);
        
        // 执行出库
        for (Map<String, Object> outboundItem : outboundItems) {
            Long drugId = Long.valueOf(outboundItem.get("drugId").toString());
            String batchNumber = outboundItem.get("batchNumber") != null ? outboundItem.get("batchNumber").toString() : null;
            Integer actualQuantity = Integer.valueOf(outboundItem.get("actualQuantity").toString());
            
            // 查找对应的申请明细
            OutboundApplyItem item = items.stream()
                    .filter(i -> i.getDrugId().equals(drugId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("未找到对应的申请明细"));
            
            // 如果指定了批次，从指定批次出库
            if (StringUtils.hasText(batchNumber)) {
                inventoryService.decreaseInventory(drugId, batchNumber, actualQuantity);
            } else {
                // 如果未指定批次，按FIFO原则出库
                List<Inventory> availableBatches = inventoryService.getAvailableBatches(drugId, actualQuantity);
                int remainingQuantity = actualQuantity;
                
                for (Inventory batch : availableBatches) {
                    if (remainingQuantity <= 0) {
                        break;
                    }
                    
                    int batchQuantity = Math.min(remainingQuantity, batch.getQuantity());
                    inventoryService.decreaseInventory(drugId, batch.getBatchNumber(), batchQuantity);
                    remainingQuantity -= batchQuantity;
                }
                
                if (remainingQuantity > 0) {
                    throw new RuntimeException("库存不足，药品ID=" + drugId + "，需要出库：" + actualQuantity);
                }
            }
            
            // 更新申请明细的实际出库数量
            item.setActualQuantity(actualQuantity);
            outboundApplyItemMapper.updateById(item);
        }
        
        // 更新申请状态
        apply.setStatus("OUTBOUND");
        apply.setOutboundTime(LocalDateTime.now());
        outboundApplyMapper.updateById(apply);
        
        log.info("执行出库：申请ID={}, 出库时间={}", id, apply.getOutboundTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOutboundApply(Long id) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new RuntimeException("出库申请不存在");
        }
        
        if ("OUTBOUND".equals(apply.getStatus())) {
            throw new RuntimeException("申请已出库，无法取消");
        }
        
        apply.setStatus("CANCELLED");
        outboundApplyMapper.updateById(apply);
        
        log.info("取消出库申请：申请ID={}", id);
    }

    @Override
    public Long getPendingOutboundCount() {
        return outboundApplyMapper.countPendingOutbound();
    }

    @Override
    public Long getTodayOutboundCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        return outboundApplyMapper.countTodayOutbound(todayStart, todayEnd);
    }

    @Override
    public List<OutboundApplyItem> getOutboundApplyItems(Long applyId) {
        return outboundApplyItemMapper.selectByApplyId(applyId);
    }

    /**
     * 生成申领单号
     * 格式：OUT + 日期（YYYYMMDD）+ 3位序号
     */
    private String generateApplyNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询今天已生成的申请数量
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<OutboundApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OutboundApply::getCreateTime, today.atStartOfDay());
        wrapper.lt(OutboundApply::getCreateTime, today.plusDays(1).atStartOfDay());
        long count = outboundApplyMapper.selectCount(wrapper);
        String sequence = String.format("%03d", count + 1);
        return "OUT" + dateStr + sequence;
    }
}

