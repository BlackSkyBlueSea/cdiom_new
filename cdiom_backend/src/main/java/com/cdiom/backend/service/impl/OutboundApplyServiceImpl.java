package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.exception.ServiceException;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.InventoryMapper;
import com.cdiom.backend.mapper.OutboundApplyItemMapper;
import com.cdiom.backend.mapper.OutboundApplyMapper;
import com.cdiom.backend.mapper.SysRoleMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.model.OutboundApply;
import com.cdiom.backend.model.OutboundApplyItem;
import com.cdiom.backend.model.SysRole;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.InventoryService;
import com.cdiom.backend.service.OutboundApplyService;
import com.cdiom.backend.util.RetryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 出库申请服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundApplyServiceImpl implements OutboundApplyService {

    /** 医护人员角色 ID（与 sys_role 初始化数据一致） */
    private static final long MEDICAL_STAFF_ROLE_ID = 4L;

    private final OutboundApplyMapper outboundApplyMapper;
    private final OutboundApplyItemMapper outboundApplyItemMapper;
    private final DrugInfoMapper drugInfoMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryService inventoryService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

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

        Page<OutboundApply> pageResult = outboundApplyMapper.selectPage(pageParam, wrapper);
        for (OutboundApply apply : pageResult.getRecords()) {
            fillApplicantAndApproverNames(apply);
        }
        return pageResult;
    }

    @Override
    public List<String> listDepartments() {
        return outboundApplyMapper.listDistinctDepartments();
    }

    @Override
    public List<Map<String, Object>> listDrugBatchesForApply(Long drugId) {
        if (drugId == null) {
            throw new ServiceException("药品ID不能为空");
        }
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) {
            throw new ServiceException("药品不存在");
        }
        Page<Inventory> page = inventoryService.getInventoryList(
                1, 500, null, drugId, null, null, null, null, null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Inventory inv : page.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("batchNumber", inv.getBatchNumber());
            row.put("quantity", inv.getQuantity());
            row.put("expiryDate", inv.getExpiryDate());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public OutboundApply getOutboundApplyById(Long id) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply != null) {
            fillApplicantAndApproverNames(apply);
        }
        return apply;
    }

    /**
     * 填充申请人、审批人的姓名与角色名（用于列表/详情展示，避免仅显示用户名与角色混淆）
     */
    private void fillApplicantAndApproverNames(OutboundApply apply) {
        if (apply.getApplicantId() != null) {
            SysUser applicant = sysUserMapper.selectById(apply.getApplicantId());
            if (applicant != null) {
                apply.setApplicantName(applicant.getUsername());
                if (applicant.getRoleId() != null) {
                    SysRole role = sysRoleMapper.selectById(applicant.getRoleId());
                    apply.setApplicantRoleName(role != null ? role.getRoleName() : null);
                }
            }
        }
        if (apply.getApproverId() != null) {
            SysUser approver = sysUserMapper.selectById(apply.getApproverId());
            if (approver != null) {
                apply.setApproverName(approver.getUsername());
                if (approver.getRoleId() != null) {
                    SysRole role = sysRoleMapper.selectById(approver.getRoleId());
                    apply.setApproverRoleName(role != null ? role.getRoleName() : null);
                }
            }
        }
        if (apply.getProxyRegistrarId() != null) {
            SysUser proxy = sysUserMapper.selectById(apply.getProxyRegistrarId());
            if (proxy != null) {
                apply.setProxyRegistrarName(proxy.getUsername());
                if (proxy.getRoleId() != null) {
                    SysRole role = sysRoleMapper.selectById(proxy.getRoleId());
                    apply.setProxyRegistrarRoleName(role != null ? role.getRoleName() : null);
                }
            }
        }
        if (apply.getSecondApproverId() != null) {
            SysUser second = sysUserMapper.selectById(apply.getSecondApproverId());
            if (second != null) {
                apply.setSecondApproverName(second.getUsername());
                if (second.getRoleId() != null) {
                    SysRole role = sysRoleMapper.selectById(second.getRoleId());
                    apply.setSecondApproverRoleName(role != null ? role.getRoleName() : null);
                }
            }
        }
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
            throw new ServiceException("出库申请明细不能为空");
        }
        
        outboundApply.setStatus("PENDING");
        
        // 使用重试机制创建出库申请
        try {
            OutboundApply created = RetryUtil.executeWithRetry(() -> createApplyWithGeneratedNumber(outboundApply, items));
            log.info("创建出库申请：申领单号={}, 申请人ID={}", created.getApplyNumber(), created.getApplicantId());
            return created;
        } catch (Exception e) {
            if (e.getCause() instanceof DuplicateKeyException) {
                throw new ServiceException("当前出库申请创建过于繁忙，请稍后重试");
            }
            throw new ServiceException("创建出库申请失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundApply createOutboundApplyOnBehalf(Long proxyRegistrarId, Long applicantUserId, String department,
            String purpose, String remark, List<Map<String, Object>> items) {
        if (proxyRegistrarId == null) {
            throw new ServiceException("无法识别代录人，请重新登录后重试");
        }
        if (applicantUserId == null) {
            throw new ServiceException("请选择申领医护人员");
        }
        if (proxyRegistrarId.equals(applicantUserId)) {
            throw new ServiceException("申领人与代录人不能为同一人");
        }
        SysUser applicant = sysUserMapper.selectById(applicantUserId);
        if (applicant == null) {
            throw new ServiceException("所选申领人不存在");
        }
        if (applicant.getStatus() == null || applicant.getStatus() != 1) {
            throw new ServiceException("所选用户未启用，不能作为申领人");
        }
        if (applicant.getRoleId() == null || applicant.getRoleId() != MEDICAL_STAFF_ROLE_ID) {
            throw new ServiceException("代录出库的申领人须为医护人员");
        }
        OutboundApply apply = new OutboundApply();
        apply.setApplicantId(applicantUserId);
        apply.setProxyRegistrarId(proxyRegistrarId);
        apply.setDepartment(department);
        apply.setPurpose(purpose);
        apply.setRemark(remark);
        OutboundApply created = createOutboundApply(apply, items);
        log.info("代录出库申请：申领单号={}, 申领人ID={}, 代录人ID={}", created.getApplyNumber(), applicantUserId, proxyRegistrarId);
        return created;
    }

    @Override
    public List<Map<String, Object>> listMedicalApplicantsForProxy() {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getRoleId, MEDICAL_STAFF_ROLE_ID);
        w.eq(SysUser::getStatus, 1);
        w.orderByAsc(SysUser::getUsername);
        List<SysUser> list = sysUserMapper.selectList(w);
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysUser u : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            SysRole role = u.getRoleId() != null ? sysRoleMapper.selectById(u.getRoleId()) : null;
            row.put("roleName", role != null ? role.getRoleName() : null);
            out.add(row);
        }
        return out;
    }

    @Override
    public List<SysUser> listOutboundSecondApproverCandidates() {
        List<SysUser> list = sysUserMapper.selectUsersWithOutboundApprovePermissions();
        for (SysUser u : list) {
            u.setPassword(null);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOutboundApply(Long id, Long approverId, Long secondApproverId) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new ServiceException("出库申请不存在");
        }
        
        if (!"PENDING".equals(apply.getStatus())) {
            throw new ServiceException("申请状态不是待审批，无法审批");
        }
        
        // 验证：申请人和审批人不能是同一人
        if (apply.getApplicantId() != null && apply.getApplicantId().equals(approverId)) {
            throw new ServiceException("申请人和审批人不能是同一人");
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
        
        if (hasSpecialDrug) {
            if (secondApproverId == null) {
                throw new ServiceException("申请包含特殊药品，需要第二审批人确认");
            }
            // 验证：特殊药品的申请人和第二审批人不能是同一人
            if (apply.getApplicantId() != null && apply.getApplicantId().equals(secondApproverId)) {
                throw new ServiceException("特殊药品申请人和第二审批人不能是同一人");
            }
            // 验证：第一审批人和第二审批人不能是同一人
            if (approverId.equals(secondApproverId)) {
                throw new ServiceException("第一审批人和第二审批人不能是同一人");
            }
        }

        // 审批前校验库存：无货或不足时不允许通过，避免只走流程无实际意义
        List<String> insufficientList = new java.util.ArrayList<>();
        for (OutboundApplyItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) continue;
            int available = inventoryService.getTotalAvailableQuantity(item.getDrugId());
            if (available < item.getQuantity()) {
                DrugInfo drug = drugInfoMapper.selectById(item.getDrugId());
                String name = drug != null ? (drug.getDrugName() + (StringUtils.hasText(drug.getSpecification()) ? " " + drug.getSpecification() : "")) : "药品ID:" + item.getDrugId();
                insufficientList.add(name + " 需要" + item.getQuantity() + " 可用" + available);
            }
        }
        if (!insufficientList.isEmpty()) {
            throw new ServiceException("以下药品库存不足，无法审批通过：" + String.join("；", insufficientList));
        }

        apply.setApproverId(approverId);
        apply.setRejectReason(null);
        apply.setRejectOperatorId(null);
        if (hasSpecialDrug) {
            apply.setSecondApproverId(secondApproverId);
            apply.setStatus("PENDING_SECOND");
            apply.setFirstApproveTime(LocalDateTime.now());
            apply.setApproveTime(null);
            outboundApplyMapper.updateById(apply);
            log.info("出库申请第一审批通过，待第二审批：申请ID={}, 第一审批人ID={}, 第二审批人ID={}", id, approverId, secondApproverId);
        } else {
            apply.setSecondApproverId(null);
            apply.setFirstApproveTime(null);
            apply.setStatus("APPROVED");
            apply.setApproveTime(LocalDateTime.now());
            outboundApplyMapper.updateById(apply);
            log.info("审批通过出库申请：申请ID={}, 审批人ID={}", id, approverId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void secondApproveOutboundApply(Long id, Long secondApproverUserId) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new ServiceException("出库申请不存在");
        }
        if (!"PENDING_SECOND".equals(apply.getStatus())) {
            throw new ServiceException("申请状态不是待第二审批，无法执行第二审批");
        }
        if (apply.getSecondApproverId() == null || !apply.getSecondApproverId().equals(secondApproverUserId)) {
            throw new ServiceException("仅指定的第二审批人本人可确认通过");
        }
        if (apply.getApplicantId() != null && apply.getApplicantId().equals(secondApproverUserId)) {
            throw new ServiceException("申请人和第二审批人不能是同一人");
        }

        List<OutboundApplyItem> items = outboundApplyItemMapper.selectByApplyId(id);
        List<String> insufficientList = new java.util.ArrayList<>();
        for (OutboundApplyItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            int available = inventoryService.getTotalAvailableQuantity(item.getDrugId());
            if (available < item.getQuantity()) {
                DrugInfo drug = drugInfoMapper.selectById(item.getDrugId());
                String name = drug != null ? (drug.getDrugName() + (StringUtils.hasText(drug.getSpecification()) ? " " + drug.getSpecification() : "")) : "药品ID:" + item.getDrugId();
                insufficientList.add(name + " 需要" + item.getQuantity() + " 可用" + available);
            }
        }
        if (!insufficientList.isEmpty()) {
            throw new ServiceException("以下药品库存不足，无法通过第二审批：" + String.join("；", insufficientList));
        }

        apply.setStatus("APPROVED");
        apply.setApproveTime(LocalDateTime.now());
        apply.setRejectOperatorId(null);
        outboundApplyMapper.updateById(apply);
        log.info("出库申请第二审批通过：申请ID={}, 第二审批人ID={}", id, secondApproverUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOutboundApply(Long id, Long approverId, String rejectReason) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new ServiceException("出库申请不存在");
        }

        String st = apply.getStatus();
        if (!"PENDING".equals(st) && !"PENDING_SECOND".equals(st)) {
            throw new ServiceException("当前状态不可驳回");
        }

        if (apply.getApplicantId() != null && apply.getApplicantId().equals(approverId)) {
            throw new ServiceException("申请人不可驳回自己的申请");
        }

        if ("PENDING_SECOND".equals(st)) {
            boolean first = apply.getApproverId() != null && apply.getApproverId().equals(approverId);
            boolean second = apply.getSecondApproverId() != null && apply.getSecondApproverId().equals(approverId);
            if (!first && !second) {
                throw new ServiceException("仅第一审批人或第二审批人可驳回该申请");
            }
            apply.setRejectOperatorId(approverId);
        } else {
            apply.setApproverId(approverId);
            apply.setRejectOperatorId(null);
        }

        apply.setStatus("REJECTED");
        apply.setRejectReason(rejectReason);
        apply.setApproveTime(LocalDateTime.now());

        outboundApplyMapper.updateById(apply);

        log.info("驳回出库申请：申请ID={}, 操作人ID={}, 状态原值={}, 驳回理由={}", id, approverId, st, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeOutbound(Long id, List<Map<String, Object>> outboundItems) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new ServiceException("出库申请不存在");
        }
        
        if (!"APPROVED".equals(apply.getStatus())) {
            throw new ServiceException("申请状态不是已通过，无法出库");
        }
        
        List<OutboundApplyItem> items = outboundApplyItemMapper.selectByApplyId(id);
        if (outboundItems.size() != items.size()) {
            throw new ServiceException("出库明细条数与申请明细不一致，请刷新页面后重试");
        }

        // 执行出库（与申请明细按顺序一一对应，避免同一药品多行时错配）
        for (int i = 0; i < outboundItems.size(); i++) {
            Map<String, Object> outboundItem = outboundItems.get(i);
            OutboundApplyItem item = items.get(i);

            // 验证并转换 drugId
            Object drugIdObj = outboundItem.get("drugId");
            if (drugIdObj == null) {
                throw new ServiceException("出库明细第" + (i + 1) + "项：药品ID不能为空");
            }
            Long drugId;
            try {
                drugId = Long.valueOf(drugIdObj.toString());
            } catch (NumberFormatException e) {
                log.error("出库明细第{}项：药品ID类型转换失败，值={}, 错误={}", i + 1, drugIdObj, e.getMessage());
                throw new ServiceException("出库明细第" + (i + 1) + "项：药品ID格式不正确");
            }
            if (!item.getDrugId().equals(drugId)) {
                throw new ServiceException("出库明细第" + (i + 1) + "项与申请明细药品不一致，请刷新页面后重试");
            }

            String batchNumber = outboundItem.get("batchNumber") != null ? outboundItem.get("batchNumber").toString().trim() : null;
            // 执行时未再次指定批次时，沿用申请人在申请明细中填写的批次号（否则会被误判为 FIFO）
            if (!StringUtils.hasText(batchNumber)) {
                batchNumber = item.getBatchNumber() != null ? item.getBatchNumber().trim() : null;
            }
            
            // 验证并转换 actualQuantity
            Object actualQuantityObj = outboundItem.get("actualQuantity");
            if (actualQuantityObj == null) {
                throw new ServiceException("出库明细第" + (i + 1) + "项：实际出库数量不能为空");
            }
            Integer actualQuantity;
            try {
                actualQuantity = Integer.valueOf(actualQuantityObj.toString());
                if (actualQuantity <= 0) {
                    throw new ServiceException("出库明细第" + (i + 1) + "项：实际出库数量必须大于0");
                }
            } catch (NumberFormatException e) {
                log.error("出库明细第{}项：实际出库数量类型转换失败，值={}, 错误={}", i + 1, actualQuantityObj, e.getMessage());
                throw new ServiceException("出库明细第" + (i + 1) + "项：实际出库数量格式不正确");
            }
            
            // 如果指定了批次，从指定批次出库
            if (StringUtils.hasText(batchNumber)) {
                inventoryService.decreaseInventory(drugId, batchNumber, actualQuantity);
            } else {
                // 如果未指定批次，按FIFO原则出库
                // getAvailableBatches 方法已经验证了总数量是否足够，并只返回足够数量的批次
                List<Inventory> availableBatches = inventoryService.getAvailableBatches(drugId, actualQuantity);
                int remainingQuantity = actualQuantity;
                
                // 在扣减前再次验证（双重保险）
                int totalAvailableQuantity = availableBatches.stream()
                        .mapToInt(Inventory::getQuantity)
                        .sum();
                if (totalAvailableQuantity < actualQuantity) {
                    throw new ServiceException("库存不足，药品ID=" + drugId + "，需要出库：" + actualQuantity + "，可用数量：" + totalAvailableQuantity);
                }
                
                // 按FIFO顺序扣减库存
                for (Inventory batch : availableBatches) {
                    if (remainingQuantity <= 0) {
                        break;
                    }
                    
                    int batchQuantity = Math.min(remainingQuantity, batch.getQuantity());
                    inventoryService.decreaseInventory(drugId, batch.getBatchNumber(), batchQuantity);
                    remainingQuantity -= batchQuantity;
                }
                
                // 理论上不应该到达这里（因为已经验证过），但保留作为最后一道防线
                if (remainingQuantity > 0) {
                    throw new ServiceException("库存扣减异常，药品ID=" + drugId + "，剩余未扣减数量：" + remainingQuantity);
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
            throw new ServiceException("出库申请不存在");
        }
        
        if ("OUTBOUND".equals(apply.getStatus())) {
            throw new ServiceException("申请已出库，无法取消");
        }
        
        apply.setStatus("CANCELLED");
        outboundApplyMapper.updateById(apply);
        
        log.info("取消出库申请：申请ID={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawOutboundApply(Long id, Long applicantUserId) {
        OutboundApply apply = outboundApplyMapper.selectById(id);
        if (apply == null) {
            throw new ServiceException("出库申请不存在");
        }
        if (!applicantUserId.equals(apply.getApplicantId())) {
            throw new ServiceException("仅申请人本人可撤回");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new ServiceException("仅待审批状态的申请可撤回");
        }
        apply.setStatus("CANCELLED");
        outboundApplyMapper.updateById(apply);
        log.info("申请人撤回出库申请：申请ID={}, 申请人ID={}", id, applicantUserId);
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

    @Override
    public Map<String, Object> checkStockForApply(Long applyId) {
        List<OutboundApplyItem> items = outboundApplyItemMapper.selectByApplyId(applyId);
        List<Map<String, Object>> details = new java.util.ArrayList<>();
        boolean sufficient = true;
        for (OutboundApplyItem item : items) {
            int required = item.getQuantity() != null ? item.getQuantity() : 0;
            int available = inventoryService.getTotalAvailableQuantity(item.getDrugId());
            boolean itemOk = available >= required;
            if (!itemOk) sufficient = false;
            DrugInfo drug = drugInfoMapper.selectById(item.getDrugId());
            String drugName = drug != null ? (drug.getDrugName() + (StringUtils.hasText(drug.getSpecification()) ? " " + drug.getSpecification() : "")) : "药品ID:" + item.getDrugId();
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("drugId", item.getDrugId());
            row.put("drugName", drugName);
            row.put("required", required);
            row.put("available", available);
            row.put("sufficient", itemOk);
            details.add(row);
        }
        String message = sufficient ? "当前库存充足，可审批通过。" : "以下药品库存不足，无法审批通过，请补货或驳回申请。";
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("sufficient", sufficient);
        result.put("message", message);
        result.put("details", details);
        return result;
    }

    @Override
    public Map<String, Object> getOutboundPickSummary(LocalDate date, String scope) {
        String sc = StringUtils.hasText(scope) ? scope.trim() : "approve_day";
        LocalDate day = date != null ? date : LocalDate.now();

        LambdaQueryWrapper<OutboundApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboundApply::getStatus, "APPROVED");
        if ("approve_day".equals(sc)) {
            wrapper.ge(OutboundApply::getApproveTime, day.atStartOfDay());
            wrapper.lt(OutboundApply::getApproveTime, day.plusDays(1).atStartOfDay());
        } else if ("all_pending".equals(sc)) {
            // 全部待执行（已通过、未出库）
        } else {
            throw new ServiceException("scope 参数无效，仅支持 approve_day 或 all_pending");
        }
        wrapper.orderByAsc(OutboundApply::getApproveTime).orderByAsc(OutboundApply::getId);
        List<OutboundApply> applies = outboundApplyMapper.selectList(wrapper);

        List<Map<String, Object>> pickLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("scope", sc);
        result.put("date", day.toString());
        result.put("applyCount", applies.size());
        result.put("pickLines", pickLines);
        result.put("warnings", warnings);

        if (applies.isEmpty()) {
            result.put("summary", new ArrayList<>());
            return result;
        }

        Set<Long> drugIds = new HashSet<>();
        for (OutboundApply a : applies) {
            List<OutboundApplyItem> its = outboundApplyItemMapper.selectByApplyId(a.getId());
            for (OutboundApplyItem it : its) {
                if (it.getDrugId() != null) {
                    drugIds.add(it.getDrugId());
                }
            }
        }

        Map<Long, List<SimBatch>> pool = new HashMap<>();
        if (!drugIds.isEmpty()) {
            LambdaQueryWrapper<Inventory> invW = new LambdaQueryWrapper<>();
            invW.in(Inventory::getDrugId, drugIds);
            invW.gt(Inventory::getQuantity, 0);
            invW.ge(Inventory::getExpiryDate, LocalDate.now());
            List<Inventory> invRows = inventoryMapper.selectList(invW);
            for (Inventory inv : invRows) {
                SimBatch sb = new SimBatch();
                sb.batchNumber = inv.getBatchNumber();
                sb.quantity = inv.getQuantity() != null ? inv.getQuantity() : 0;
                sb.storageLocation = inv.getStorageLocation();
                sb.expiryDate = inv.getExpiryDate();
                pool.computeIfAbsent(inv.getDrugId(), k -> new ArrayList<>()).add(sb);
            }
            for (List<SimBatch> list : pool.values()) {
                list.sort(Comparator
                        .comparing((SimBatch b) -> b.expiryDate != null ? b.expiryDate : LocalDate.MAX)
                        .thenComparing(b -> b.batchNumber != null ? b.batchNumber : ""));
            }
        }

        for (OutboundApply apply : applies) {
            fillApplicantAndApproverNames(apply);
            List<OutboundApplyItem> items = outboundApplyItemMapper.selectByApplyId(apply.getId());
            for (OutboundApplyItem item : items) {
                Long drugId = item.getDrugId();
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                if (drugId == null || qty <= 0) {
                    continue;
                }
                DrugInfo drug = drugInfoMapper.selectById(drugId);
                String drugName = drug != null ? drug.getDrugName() : ("药品ID:" + drugId);
                String spec = drug != null ? drug.getSpecification() : null;

                String batchFromApply = item.getBatchNumber() != null ? item.getBatchNumber().trim() : null;
                String batchNumber = StringUtils.hasText(batchFromApply) ? batchFromApply : null;

                if (StringUtils.hasText(batchNumber)) {
                    SimBatch match = findSimBatch(pool.get(drugId), batchNumber);
                    if (match == null || match.quantity <= 0) {
                        warnings.add(String.format("申领单 %s：药品「%s」指定批次 %s 当前无可用量，将按先到期先出（FIFO）尝试",
                                apply.getApplyNumber(), drugName, batchNumber));
                        allocateFifoLines(apply, drugId, drugName, spec, qty, pool, pickLines, warnings);
                        continue;
                    }
                    int take = Math.min(qty, match.quantity);
                    match.quantity -= take;
                    pickLines.add(buildPickLine(apply, drugId, drugName, spec, match.batchNumber,
                            match.storageLocation, match.expiryDate, take));
                    if (take < qty) {
                        warnings.add(String.format("申领单 %s：药品「%s」批次 %s 仅满足 %d/%d，余量按 FIFO",
                                apply.getApplyNumber(), drugName, batchNumber, take, qty));
                        allocateFifoLines(apply, drugId, drugName, spec, qty - take, pool, pickLines, warnings);
                    }
                } else {
                    allocateFifoLines(apply, drugId, drugName, spec, qty, pool, pickLines, warnings);
                }
            }
        }

        Map<String, Map<String, Object>> summaryMap = new LinkedHashMap<>();
        for (Map<String, Object> line : pickLines) {
            Long drugId = (Long) line.get("drugId");
            String bn = line.get("batchNumber") != null ? line.get("batchNumber").toString() : "";
            String loc = line.get("storageLocation") != null ? line.get("storageLocation").toString() : "";
            String key = drugId + "\0" + bn + "\0" + loc;
            int q = (Integer) line.get("quantity");
            summaryMap.compute(key, (k, existing) -> {
                if (existing == null) {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("drugId", line.get("drugId"));
                    s.put("drugName", line.get("drugName"));
                    s.put("specification", line.get("specification"));
                    s.put("batchNumber", line.get("batchNumber"));
                    s.put("storageLocation", line.get("storageLocation"));
                    s.put("expiryDate", line.get("expiryDate"));
                    s.put("quantity", q);
                    return s;
                }
                existing.put("quantity", (Integer) existing.get("quantity") + q);
                return existing;
            });
        }

        List<Map<String, Object>> summary = new ArrayList<>(summaryMap.values());
        summary.sort(Comparator
                .comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("storageLocation", "")))
                .thenComparing(m -> String.valueOf(m.getOrDefault("drugName", "")))
                .thenComparing(m -> String.valueOf(m.getOrDefault("batchNumber", ""))));

        result.put("summary", summary);
        return result;
    }

    private static SimBatch findSimBatch(List<SimBatch> list, String batchNumber) {
        if (list == null || !StringUtils.hasText(batchNumber)) {
            return null;
        }
        String bn = batchNumber.trim();
        for (SimBatch b : list) {
            if (b.batchNumber != null && bn.equals(b.batchNumber.trim())) {
                return b;
            }
        }
        return null;
    }

    private void allocateFifoLines(OutboundApply apply, Long drugId, String drugName, String spec, int need,
            Map<Long, List<SimBatch>> pool, List<Map<String, Object>> pickLines, List<String> warnings) {
        if (need <= 0) {
            return;
        }
        List<SimBatch> list = pool.get(drugId);
        if (list == null || list.isEmpty()) {
            warnings.add(String.format("申领单 %s：药品「%s」无可用库存（需 %d）", apply.getApplyNumber(), drugName, need));
            return;
        }
        int remaining = need;
        for (SimBatch b : list) {
            if (remaining <= 0) {
                break;
            }
            if (b.quantity <= 0) {
                continue;
            }
            int take = Math.min(remaining, b.quantity);
            b.quantity -= take;
            remaining -= take;
            pickLines.add(buildPickLine(apply, drugId, drugName, spec, b.batchNumber,
                    b.storageLocation, b.expiryDate, take));
        }
        if (remaining > 0) {
            warnings.add(String.format("申领单 %s：药品「%s」FIFO 分配不足，尚缺 %d（可能与其他待执行单竞争库存）",
                    apply.getApplyNumber(), drugName, remaining));
        }
    }

    private static Map<String, Object> buildPickLine(OutboundApply apply, Long drugId, String drugName, String spec,
            String batchNumber, String storageLocation, LocalDate expiryDate, int quantity) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("applyNumber", apply.getApplyNumber());
        m.put("department", apply.getDepartment());
        m.put("approveTime", apply.getApproveTime());
        m.put("drugId", drugId);
        m.put("drugName", drugName);
        m.put("specification", spec);
        m.put("batchNumber", batchNumber);
        m.put("storageLocation", storageLocation);
        m.put("expiryDate", expiryDate);
        m.put("quantity", quantity);
        return m;
    }

    /**
     * 拣货汇总模拟用：与当前库存快照一致，按审批顺序扣减，用于预估批次与货位（执行顺序不同则可能与实际略有差异）
     */
    private static class SimBatch {
        String batchNumber;
        int quantity;
        String storageLocation;
        LocalDate expiryDate;
    }

    /**
     * 核心任务：生成单号 + 插入出库申请（供重试工具调用）
     */
    private OutboundApply createApplyWithGeneratedNumber(OutboundApply outboundApply, List<Map<String, Object>> items) {
        // 1. 生成唯一单号
        String applyNumber = generateApplyNumber();
        outboundApply.setApplyNumber(applyNumber);
        
        // 2. 保存申请（若单号重复，会抛出DuplicateKeyException）
        outboundApplyMapper.insert(outboundApply);
        
        // 3. 保存申请明细
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            OutboundApplyItem applyItem = new OutboundApplyItem();
            applyItem.setApplyId(outboundApply.getId());
            
            // 验证并转换 drugId
            Object drugIdObj = item.get("drugId");
            if (drugIdObj == null) {
                throw new ServiceException("出库申请明细第" + (i + 1) + "项：药品ID不能为空");
            }
            try {
                Long drugId = Long.valueOf(drugIdObj.toString());
                applyItem.setDrugId(drugId);
            } catch (NumberFormatException e) {
                log.error("出库申请明细第{}项：药品ID类型转换失败，值={}, 错误={}", i + 1, drugIdObj, e.getMessage());
                throw new ServiceException("出库申请明细第" + (i + 1) + "项：药品ID格式不正确");
            }
            
            if (item.get("batchNumber") != null) {
                String bn = item.get("batchNumber").toString().trim();
                if (StringUtils.hasText(bn)) {
                    applyItem.setBatchNumber(bn);
                }
            }
            
            // 验证并转换 quantity
            Object quantityObj = item.get("quantity");
            if (quantityObj == null) {
                throw new ServiceException("出库申请明细第" + (i + 1) + "项：数量不能为空");
            }
            try {
                Integer quantity = Integer.valueOf(quantityObj.toString());
                if (quantity <= 0) {
                    throw new ServiceException("出库申请明细第" + (i + 1) + "项：数量必须大于0");
                }
                applyItem.setQuantity(quantity);
            } catch (NumberFormatException e) {
                log.error("出库申请明细第{}项：数量类型转换失败，值={}, 错误={}", i + 1, quantityObj, e.getMessage());
                throw new ServiceException("出库申请明细第" + (i + 1) + "项：数量格式不正确");
            }
            
            if (item.get("remark") != null) {
                applyItem.setRemark(item.get("remark").toString());
            }
            outboundApplyItemMapper.insert(applyItem);
        }
        
        return outboundApply;
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

