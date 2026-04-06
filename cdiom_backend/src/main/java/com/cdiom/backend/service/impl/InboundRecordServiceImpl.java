package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.common.exception.ServiceException;
import com.cdiom.backend.mapper.DrugInfoMapper;
import com.cdiom.backend.mapper.InboundReceiptBatchMapper;
import com.cdiom.backend.mapper.InboundRecordMapper;
import com.cdiom.backend.mapper.PurchaseOrderItemMapper;
import com.cdiom.backend.mapper.PurchaseOrderMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.InboundReceiptBatch;
import com.cdiom.backend.model.InboundRecord;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.PurchaseOrderItem;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.event.UnqualifiedInboundRecordedEvent;
import com.cdiom.backend.inbound.InboundDispositionCodes;
import com.cdiom.backend.model.vo.InboundSplitResult;
import com.cdiom.backend.model.vo.OrderInboundRemainingRow;
import com.cdiom.backend.service.InboundRecordService;
import com.cdiom.backend.service.InventoryService;
import com.cdiom.backend.service.PurchaseOrderService;
import com.cdiom.backend.util.RetryUtil;
import com.cdiom.backend.util.SystemConfigUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 入库记录服务实现类
 *
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundRecordServiceImpl implements InboundRecordService {

    public static final String SECOND_CONFIRM_NONE = "NONE";
    public static final String SECOND_CONFIRM_PENDING = "PENDING_SECOND";
    public static final String SECOND_CONFIRM_CONFIRMED = "CONFIRMED";
    public static final String SECOND_CONFIRM_REJECTED = "REJECTED";
    public static final String SECOND_CONFIRM_WITHDRAWN = "WITHDRAWN";
    public static final String SECOND_CONFIRM_TIMEOUT = "TIMEOUT";

    private final InboundRecordMapper inboundRecordMapper;
    private final InboundReceiptBatchMapper inboundReceiptBatchMapper;
    private final DrugInfoMapper drugInfoMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InventoryService inventoryService;
    private final PurchaseOrderService purchaseOrderService;
    private final SystemConfigUtil systemConfigUtil;
    private final SysUserMapper sysUserMapper;
    private final InboundSecondConfirmMailNotifier inboundSecondConfirmMailNotifier;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${cdiom.inbound.second-confirm-timeout-hours:48}")
    private int secondConfirmTimeoutHours;

    @Override
    public Page<InboundRecord> getInboundRecordList(Integer page, Integer size, String keyword, Long orderId, Long drugId, String batchNumber, Long operatorId, LocalDate startDate, LocalDate endDate, String status, String expiryCheckStatus, String secondConfirmStatus, Long secondOperatorId) {
        Page<InboundRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InboundRecord> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            LambdaQueryWrapper<DrugInfo> drugNameWrapper = new LambdaQueryWrapper<>();
            drugNameWrapper.select(DrugInfo::getId).like(DrugInfo::getDrugName, keyword);
            List<Long> drugIdsMatchingName = drugInfoMapper.selectList(drugNameWrapper).stream()
                    .map(DrugInfo::getId)
                    .collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(InboundRecord::getRecordNumber, keyword)
                        .or().like(InboundRecord::getBatchNumber, keyword)
                        .or().like(InboundRecord::getManufacturer, keyword);
                if (!drugIdsMatchingName.isEmpty()) {
                    w.or().in(InboundRecord::getDrugId, drugIdsMatchingName);
                }
            });
        }

        if (orderId != null) {
            wrapper.eq(InboundRecord::getOrderId, orderId);
        }

        if (drugId != null) {
            wrapper.eq(InboundRecord::getDrugId, drugId);
        }

        if (StringUtils.hasText(batchNumber)) {
            wrapper.like(InboundRecord::getBatchNumber, batchNumber);
        }

        if (operatorId != null) {
            wrapper.eq(InboundRecord::getOperatorId, operatorId);
        }

        if (startDate != null) {
            wrapper.ge(InboundRecord::getCreateTime, startDate.atStartOfDay());
        }

        if (endDate != null) {
            wrapper.le(InboundRecord::getCreateTime, endDate.plusDays(1).atStartOfDay());
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(InboundRecord::getStatus, status);
        }

        if (StringUtils.hasText(expiryCheckStatus)) {
            wrapper.eq(InboundRecord::getExpiryCheckStatus, expiryCheckStatus);
        }

        if (StringUtils.hasText(secondConfirmStatus)) {
            wrapper.eq(InboundRecord::getSecondConfirmStatus, secondConfirmStatus);
        }

        if (secondOperatorId != null) {
            wrapper.eq(InboundRecord::getSecondOperatorId, secondOperatorId);
        }

        wrapper.orderByDesc(InboundRecord::getCreateTime);

        Page<InboundRecord> resultPage = inboundRecordMapper.selectPage(pageParam, wrapper);

        populateDisplayFields(resultPage.getRecords());

        return resultPage;
    }

    /**
     * 列表/详情：药品名、到货批次号、操作人用户名（便于追溯）
     */
    private void populateDisplayFields(List<InboundRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (InboundRecord record : records) {
            if (record.getDrugId() != null) {
                DrugInfo drug = drugInfoMapper.selectById(record.getDrugId());
                if (drug != null) {
                    record.setDrugName(drug.getDrugName());
                }
            }
        }
        Set<Long> batchIds = records.stream()
                .map(InboundRecord::getReceiptBatchId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!batchIds.isEmpty()) {
            List<InboundReceiptBatch> batches = inboundReceiptBatchMapper.selectList(
                    new LambdaQueryWrapper<InboundReceiptBatch>().in(InboundReceiptBatch::getId, batchIds));
            Map<Long, String> codeById = batches.stream()
                    .collect(Collectors.toMap(InboundReceiptBatch::getId, InboundReceiptBatch::getBatchCode));
            for (InboundRecord record : records) {
                if (record.getReceiptBatchId() != null) {
                    record.setReceiptBatchCode(codeById.get(record.getReceiptBatchId()));
                }
            }
        }
        Set<Long> userIds = new HashSet<>();
        for (InboundRecord r : records) {
            if (r.getOperatorId() != null) {
                userIds.add(r.getOperatorId());
            }
            if (r.getSecondOperatorId() != null) {
                userIds.add(r.getSecondOperatorId());
            }
        }
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
            Map<Long, String> nameById = users.stream().collect(Collectors.toMap(
                    SysUser::getId,
                    u -> u.getUsername() != null ? u.getUsername() : String.valueOf(u.getId()),
                    (a, b) -> a));
            for (InboundRecord r : records) {
                if (r.getOperatorId() != null) {
                    r.setOperatorName(nameById.get(r.getOperatorId()));
                }
                if (r.getSecondOperatorId() != null) {
                    r.setSecondOperatorName(nameById.get(r.getSecondOperatorId()));
                }
            }
        }
    }

    @Override
    public InboundRecord getInboundRecordById(Long id) {
        InboundRecord record = inboundRecordMapper.selectById(id);
        if (record != null) {
            populateDisplayFields(Collections.singletonList(record));
        }
        return record;
    }

    @Override
    public InboundRecord getInboundRecordByRecordNumber(String recordNumber) {
        LambdaQueryWrapper<InboundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundRecord::getRecordNumber, recordNumber);
        InboundRecord record = inboundRecordMapper.selectOne(wrapper);
        if (record != null) {
            populateDisplayFields(Collections.singletonList(record));
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundReceiptBatch createReceiptBatchHeader(Long orderId, String deliveryNoteNumber,
                                                        LocalDateTime arrivalAt, Long operatorId,
                                                        String remark, String deliveryNoteImage) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new ServiceException("订单状态不是已发货，无法登记到货批次");
        }
        if (!StringUtils.hasText(deliveryNoteNumber)) {
            throw new ServiceException("请填写随货同行单编号");
        }
        LocalDateTime at = arrivalAt != null ? arrivalAt : LocalDateTime.now();
        InboundReceiptBatch b = new InboundReceiptBatch();
        b.setBatchCode(generateReceiptBatchCode());
        b.setOrderId(orderId);
        b.setDeliveryNoteNumber(deliveryNoteNumber.trim());
        b.setArrivalTime(at);
        b.setOperatorId(operatorId);
        b.setRemark(remark);
        b.setDeliveryNoteImage(deliveryNoteImage);
        inboundReceiptBatchMapper.insert(b);
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundRecord createInboundRecordFromOrder(InboundRecord inboundRecord, Long orderId, Long drugId,
                                                      Long receiptBatchId, LocalDateTime batchArrivalAt) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new ServiceException("订单状态不是已发货，无法入库");
        }

        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) {
            throw new ServiceException("药品信息不存在");
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId)
                .eq(PurchaseOrderItem::getDrugId, drugId);
        PurchaseOrderItem orderItem = purchaseOrderItemMapper.selectOne(itemWrapper);

        if (orderItem == null) {
            throw new ServiceException("订单中不存在该药品的明细信息");
        }

        Integer committed = inboundRecordMapper.getInboundCommittedQuantityByOrderAndDrug(orderId, drugId);
        if (committed == null) {
            committed = 0;
        }

        Integer totalInboundQuantity = committed + inboundRecord.getQuantity();
        if (totalInboundQuantity > orderItem.getQuantity()) {
            throw new ServiceException(String.format(
                    "入库数量超过订单采购数量。订单采购数量：%d，已占用（含待确认）：%d，本次入库数量：%d，总计：%d",
                    orderItem.getQuantity(), committed, inboundRecord.getQuantity(), totalInboundQuantity
            ));
        }

        inboundRecord.setOrderId(orderId);
        inboundRecord.setDrugId(drugId);

        InboundReceiptBatch receiptBatch = resolveReceiptBatch(orderId, receiptBatchId, inboundRecord, batchArrivalAt);
        inboundRecord.setReceiptBatchId(receiptBatch.getId());
        if (!StringUtils.hasText(inboundRecord.getDeliveryNoteNumber())) {
            inboundRecord.setDeliveryNoteNumber(receiptBatch.getDeliveryNoteNumber());
        }
        if (!StringUtils.hasText(inboundRecord.getDeliveryNoteImage()) && StringUtils.hasText(receiptBatch.getDeliveryNoteImage())) {
            inboundRecord.setDeliveryNoteImage(receiptBatch.getDeliveryNoteImage());
        }
        if (inboundRecord.getArrivalDate() == null) {
            inboundRecord.setArrivalDate(receiptBatch.getArrivalTime().toLocalDate());
        }

        String expiryCheckStatus = checkExpiryDate(inboundRecord.getExpiryDate());
        inboundRecord.setExpiryCheckStatus(expiryCheckStatus);

        if ("FORCE".equals(expiryCheckStatus) && !StringUtils.hasText(inboundRecord.getExpiryCheckReason())) {
            throw new ServiceException("有效期不足90天，需要填写强制入库原因");
        }

        if (!StringUtils.hasText(inboundRecord.getStatus())) {
            inboundRecord.setStatus("QUALIFIED");
        }

        validateNormalizeStorageLocation(inboundRecord);

        Long operatorId = inboundRecord.getOperatorId();
        applySecondConfirmFieldsForCreate(inboundRecord, drug, operatorId);

        try {
            InboundRecord created = RetryUtil.executeWithRetry(() -> createInboundWithGeneratedNumber(inboundRecord));

            if ("QUALIFIED".equals(created.getStatus()) && SECOND_CONFIRM_CONFIRMED.equals(created.getSecondConfirmStatus())) {
                finalizeQualifiedInbound(created, drug);
            } else if (SECOND_CONFIRM_PENDING.equals(created.getSecondConfirmStatus())) {
                notifySecondOperatorIfNeeded(created);
            }

            log.info("创建入库记录：入库单号={}, 订单ID={}, 药品ID={}, 数量={}, 第二人状态={}",
                    created.getRecordNumber(), orderId, drugId, created.getQuantity(), created.getSecondConfirmStatus());
            created.setReceiptBatchCode(receiptBatch.getBatchCode());
            return created;
        } catch (Exception e) {
            if (e.getCause() instanceof DuplicateKeyException) {
                throw new ServiceException("当前入库操作过于繁忙，请稍后重试");
            }
            throw new ServiceException("创建入库记录失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, String> listDispositionOptions() {
        return InboundDispositionCodes.labels();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundSplitResult createInboundSplitFromOrder(InboundRecord base, Long orderId, Long drugId,
            int qualifiedQty, int unqualifiedQty, String unqualifiedReason,
            String unqualifiedDispositionCode, String unqualifiedDispositionRemark,
            Long receiptBatchId, LocalDateTime batchArrivalAt) {
        if (qualifiedQty < 1 || unqualifiedQty < 1) {
            throw new ServiceException("分批验收时合格数量与不合格数量均须至少为 1");
        }
        if (!StringUtils.hasText(unqualifiedReason)) {
            throw new ServiceException("请填写不合格原因（可追溯）");
        }

        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new ServiceException("订单状态不是已发货，无法入库");
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseOrderItem::getOrderId, orderId)
                .eq(PurchaseOrderItem::getDrugId, drugId);
        PurchaseOrderItem orderItem = purchaseOrderItemMapper.selectOne(itemWrapper);
        if (orderItem == null) {
            throw new ServiceException("订单中不存在该药品的明细信息");
        }

        Integer committed = inboundRecordMapper.getInboundCommittedQuantityByOrderAndDrug(orderId, drugId);
        if (committed == null) {
            committed = 0;
        }
        int totalNew = qualifiedQty + unqualifiedQty;
        if (committed + totalNew > orderItem.getQuantity()) {
            throw new ServiceException(String.format(
                    "入库数量超过订单采购数量。订单采购数量：%d，已占用：%d，本次合格+不合格：%d",
                    orderItem.getQuantity(), committed, totalNew
            ));
        }

        String traceId = UUID.randomUUID().toString().replace("-", "");
        String baseRm = base.getRemark() != null ? base.getRemark().trim() : "";
        String traceLine = "[验收拆行] 追溯码: " + traceId;

        InboundRecord q = new InboundRecord();
        BeanUtils.copyProperties(base, q, "id", "recordNumber", "createTime");
        q.setQuantity(qualifiedQty);
        q.setStatus("QUALIFIED");
        q.setRemark((baseRm.isEmpty() ? "" : baseRm + "\n") + traceLine + " · 本行：合格");

        InboundRecord u = new InboundRecord();
        BeanUtils.copyProperties(base, u, "id", "recordNumber", "createTime");
        u.setQuantity(unqualifiedQty);
        u.setStatus("UNQUALIFIED");
        u.setSecondOperatorId(null);
        u.setStorageLocation(null);
        u.setDispositionCode(unqualifiedDispositionCode);
        u.setDispositionRemark(unqualifiedDispositionRemark);
        u.setRemark((baseRm.isEmpty() ? "" : baseRm + "\n") + traceLine + " · 本行：不合格 · 原因: "
                + unqualifiedReason.trim());

        InboundRecord createdQ = createInboundRecordFromOrder(q, orderId, drugId, receiptBatchId, batchArrivalAt);
        u.setReceiptBatchId(createdQ.getReceiptBatchId());
        if (!StringUtils.hasText(u.getDeliveryNoteNumber()) && StringUtils.hasText(createdQ.getDeliveryNoteNumber())) {
            u.setDeliveryNoteNumber(createdQ.getDeliveryNoteNumber());
        }
        if (!StringUtils.hasText(u.getDeliveryNoteImage()) && StringUtils.hasText(createdQ.getDeliveryNoteImage())) {
            u.setDeliveryNoteImage(createdQ.getDeliveryNoteImage());
        }
        if (u.getArrivalDate() == null && createdQ.getArrivalDate() != null) {
            u.setArrivalDate(createdQ.getArrivalDate());
        }

        InboundRecord createdU = createInboundRecordFromOrder(u, orderId, drugId, createdQ.getReceiptBatchId(), batchArrivalAt);

        InboundSplitResult result = new InboundSplitResult();
        result.setQualifiedRecord(createdQ);
        result.setUnqualifiedRecord(createdU);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundRecord createInboundRecordTemporary(InboundRecord inboundRecord, Long drugId) {
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) {
            throw new ServiceException("药品信息不存在");
        }

        inboundRecord.setOrderId(null);
        inboundRecord.setDrugId(drugId);

        if (inboundRecord.getArrivalDate() == null) {
            inboundRecord.setArrivalDate(LocalDate.now());
        }

        String expiryCheckStatus = checkExpiryDate(inboundRecord.getExpiryDate());
        inboundRecord.setExpiryCheckStatus(expiryCheckStatus);

        if ("FORCE".equals(expiryCheckStatus) && !StringUtils.hasText(inboundRecord.getExpiryCheckReason())) {
            throw new ServiceException("有效期不足90天，需要填写强制入库原因");
        }

        if (!StringUtils.hasText(inboundRecord.getStatus())) {
            inboundRecord.setStatus("QUALIFIED");
        }

        validateNormalizeStorageLocation(inboundRecord);

        Long operatorId = inboundRecord.getOperatorId();
        applySecondConfirmFieldsForCreate(inboundRecord, drug, operatorId);

        try {
            InboundRecord created = RetryUtil.executeWithRetry(() -> createInboundWithGeneratedNumber(inboundRecord));

            if ("QUALIFIED".equals(created.getStatus()) && SECOND_CONFIRM_CONFIRMED.equals(created.getSecondConfirmStatus())) {
                finalizeQualifiedInbound(created, drug);
            } else if (SECOND_CONFIRM_PENDING.equals(created.getSecondConfirmStatus())) {
                notifySecondOperatorIfNeeded(created);
            }

            log.info("创建临时入库记录：入库单号={}, 药品ID={}, 数量={}, 第二人状态={}",
                    created.getRecordNumber(), drugId, created.getQuantity(), created.getSecondConfirmStatus());
            return created;
        } catch (Exception e) {
            if (e.getCause() instanceof DuplicateKeyException) {
                throw new ServiceException("当前入库操作过于繁忙，请稍后重试");
            }
            throw new ServiceException("创建入库记录失败：" + e.getMessage());
        }
    }

    /**
     * 初验不合格：不入第二人流程；合格且非特殊药：直接 CONFIRMED；合格且特殊药：PENDING_SECOND。
     */
    private void applySecondConfirmFieldsForCreate(InboundRecord inboundRecord, DrugInfo drug, Long operatorId) {
        boolean special = drug.getIsSpecial() != null && drug.getIsSpecial() == 1;
        boolean qualified = "QUALIFIED".equals(inboundRecord.getStatus());

        if (!qualified) {
            inboundRecord.setSecondConfirmStatus(SECOND_CONFIRM_NONE);
            inboundRecord.setSecondConfirmDeadline(null);
            inboundRecord.setSecondConfirmTime(null);
            return;
        }

        if (!special) {
            inboundRecord.setSecondConfirmStatus(SECOND_CONFIRM_CONFIRMED);
            inboundRecord.setSecondConfirmDeadline(null);
            inboundRecord.setSecondConfirmTime(null);
            inboundRecord.setSecondOperatorId(null);
            return;
        }

        if (inboundRecord.getSecondOperatorId() == null) {
            throw new ServiceException("特殊药品初验合格，必须指定第二操作人");
        }
        if (operatorId != null && inboundRecord.getSecondOperatorId().equals(operatorId)) {
            throw new ServiceException("第二操作人不能与第一操作人为同一人");
        }
        inboundRecord.setSecondConfirmStatus(SECOND_CONFIRM_PENDING);
        inboundRecord.setSecondConfirmDeadline(LocalDateTime.now().plusHours(secondConfirmTimeoutHours));
        inboundRecord.setSecondConfirmTime(null);
    }

    private void validateNormalizeStorageLocation(InboundRecord inboundRecord) {
        if ("QUALIFIED".equals(inboundRecord.getStatus())) {
            if (!StringUtils.hasText(inboundRecord.getStorageLocation())) {
                throw new ServiceException("合格入库须填写存储位置");
            }
            inboundRecord.setStorageLocation(inboundRecord.getStorageLocation().trim());
        } else if (StringUtils.hasText(inboundRecord.getStorageLocation())) {
            inboundRecord.setStorageLocation(inboundRecord.getStorageLocation().trim());
        }
    }

    private String resolveStorageLocationForInventory(InboundRecord created, DrugInfo drug) {
        if (StringUtils.hasText(created.getStorageLocation())) {
            return created.getStorageLocation().trim();
        }
        if (drug != null && StringUtils.hasText(drug.getStorageLocation())) {
            return drug.getStorageLocation().trim();
        }
        return null;
    }

    private void finalizeQualifiedInbound(InboundRecord created, DrugInfo drug) {
        String loc = resolveStorageLocationForInventory(created, drug);
        if (!StringUtils.hasText(loc)) {
            throw new ServiceException("存储位置不能为空，无法入账");
        }
        inventoryService.increaseInventory(
                created.getDrugId(),
                created.getBatchNumber(),
                created.getQuantity(),
                created.getExpiryDate(),
                loc,
                created.getProductionDate(),
                created.getManufacturer() != null ? created.getManufacturer() : drug.getManufacturer()
        );
        if (created.getOrderId() != null) {
            purchaseOrderService.updateOrderInboundStatus(created.getOrderId());
        }
    }

    private void notifySecondOperatorIfNeeded(InboundRecord created) {
        if (created.getSecondOperatorId() == null) {
            return;
        }
        SysUser u = sysUserMapper.selectById(created.getSecondOperatorId());
        if (u == null) {
            return;
        }
        inboundSecondConfirmMailNotifier.notifySecondOperatorPending(created, u.getEmail(), u.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundRecord secondConfirmInbound(Long id, Long currentUserId) {
        InboundRecord record = inboundRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("入库记录不存在");
        }
        if (!SECOND_CONFIRM_PENDING.equals(record.getSecondConfirmStatus())) {
            throw new ServiceException("当前入库单不是待第二人确认状态");
        }
        if (record.getSecondOperatorId() == null || !record.getSecondOperatorId().equals(currentUserId)) {
            throw new ServiceException("仅指定的第二操作人可确认");
        }
        if (!"QUALIFIED".equals(record.getStatus())) {
            throw new ServiceException("验收状态异常，无法确认");
        }

        DrugInfo drug = drugInfoMapper.selectById(record.getDrugId());
        if (drug == null) {
            throw new ServiceException("药品信息不存在");
        }

        LambdaUpdateWrapper<InboundRecord> uw = new LambdaUpdateWrapper<>();
        uw.eq(InboundRecord::getId, id)
                .eq(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_PENDING)
                .eq(InboundRecord::getSecondOperatorId, currentUserId)
                .set(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_CONFIRMED)
                .set(InboundRecord::getSecondConfirmTime, LocalDateTime.now())
                .set(InboundRecord::getSecondConfirmDeadline, null);
        int rows = inboundRecordMapper.update(null, uw);
        if (rows == 0) {
            throw new ServiceException("确认失败，单据状态已变化，请刷新后重试");
        }

        record = inboundRecordMapper.selectById(id);
        finalizeQualifiedInbound(record, drug);
        log.info("第二人确认入库完成：入库单号={}, 操作人={}", record.getRecordNumber(), currentUserId);
        return inboundRecordMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundRecord secondRejectInbound(Long id, Long currentUserId, String reason) {
        InboundRecord record = inboundRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("入库记录不存在");
        }
        if (!SECOND_CONFIRM_PENDING.equals(record.getSecondConfirmStatus())) {
            throw new ServiceException("当前入库单不是待第二人确认状态");
        }
        if (record.getSecondOperatorId() == null || !record.getSecondOperatorId().equals(currentUserId)) {
            throw new ServiceException("仅指定的第二操作人可驳回");
        }
        if (!StringUtils.hasText(reason)) {
            throw new ServiceException("请填写驳回原因");
        }

        LambdaUpdateWrapper<InboundRecord> uw = new LambdaUpdateWrapper<>();
        uw.eq(InboundRecord::getId, id)
                .eq(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_PENDING)
                .eq(InboundRecord::getSecondOperatorId, currentUserId)
                .set(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_REJECTED)
                .set(InboundRecord::getSecondRejectReason, reason.trim())
                .set(InboundRecord::getSecondConfirmDeadline, null);
        int rows = inboundRecordMapper.update(null, uw);
        if (rows == 0) {
            throw new ServiceException("驳回失败，单据状态已变化，请刷新后重试");
        }
        log.info("第二人驳回入库：入库单号={}, 原因={}", record.getRecordNumber(), reason);
        return inboundRecordMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundRecord withdrawPendingSecondInbound(Long id, Long currentUserId) {
        InboundRecord record = inboundRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("入库记录不存在");
        }
        if (!SECOND_CONFIRM_PENDING.equals(record.getSecondConfirmStatus())) {
            throw new ServiceException("仅待第二人确认的入库单可撤回");
        }
        if (record.getOperatorId() == null || !record.getOperatorId().equals(currentUserId)) {
            throw new ServiceException("仅第一操作人可撤回");
        }

        LambdaUpdateWrapper<InboundRecord> uw = new LambdaUpdateWrapper<>();
        uw.eq(InboundRecord::getId, id)
                .eq(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_PENDING)
                .eq(InboundRecord::getOperatorId, currentUserId)
                .set(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_WITHDRAWN)
                .set(InboundRecord::getSecondConfirmDeadline, null);
        int rows = inboundRecordMapper.update(null, uw);
        if (rows == 0) {
            throw new ServiceException("撤回失败，单据状态已变化，请刷新后重试");
        }
        log.info("第一人撤回待确认入库：入库单号={}", record.getRecordNumber());
        return inboundRecordMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int timeoutPendingSecondInbound() {
        LambdaQueryWrapper<InboundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_PENDING)
                .isNotNull(InboundRecord::getSecondConfirmDeadline)
                .lt(InboundRecord::getSecondConfirmDeadline, LocalDateTime.now());
        List<InboundRecord> list = inboundRecordMapper.selectList(wrapper);
        int n = 0;
        for (InboundRecord r : list) {
            LambdaUpdateWrapper<InboundRecord> uw = new LambdaUpdateWrapper<>();
            uw.eq(InboundRecord::getId, r.getId())
                    .eq(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_PENDING)
                    .set(InboundRecord::getSecondConfirmStatus, SECOND_CONFIRM_TIMEOUT)
                    .set(InboundRecord::getSecondConfirmDeadline, null);
            int rows = inboundRecordMapper.update(null, uw);
            if (rows > 0) {
                n++;
                log.info("入库待第二人确认已超时关闭：入库单号={}", r.getRecordNumber());
            }
        }
        return n;
    }

    @Override
    public String checkExpiryDate(LocalDate expiryDate) {
        if (expiryDate == null) {
            return "FORCE";
        }

        Integer expiryWarningDays = systemConfigUtil.getExpiryWarningDays();
        Integer expiryCriticalDays = systemConfigUtil.getExpiryCriticalDays();

        LocalDate today = LocalDate.now();
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);

        if (daysUntilExpiry >= expiryWarningDays) {
            return "PASS";
        } else if (daysUntilExpiry >= expiryCriticalDays) {
            return "WARNING";
        } else {
            return "FORCE";
        }
    }

    @Override
    public Integer getInboundQuantityByOrderAndDrug(Long orderId, Long drugId) {
        return inboundRecordMapper.getInboundQuantityByOrderAndDrug(orderId, drugId);
    }

    @Override
    public Integer getInboundCommittedQuantityByOrderAndDrug(Long orderId, Long drugId) {
        return inboundRecordMapper.getInboundCommittedQuantityByOrderAndDrug(orderId, drugId);
    }

    @Override
    public List<OrderInboundRemainingRow> listOrderInboundRemaining(Long orderId) {
        List<PurchaseOrderItem> items = purchaseOrderService.getOrderItems(orderId);
        List<OrderInboundRemainingRow> rows = new ArrayList<>();
        for (PurchaseOrderItem item : items) {
            Integer committed = inboundRecordMapper.getInboundCommittedQuantityByOrderAndDrug(orderId, item.getDrugId());
            int c = committed != null ? committed : 0;
            int ordered = item.getQuantity() != null ? item.getQuantity() : 0;
            int remaining = Math.max(0, ordered - c);
            OrderInboundRemainingRow row = new OrderInboundRemainingRow();
            row.setDrugId(item.getDrugId());
            row.setDrugName(item.getDrugName());
            row.setSpecification(item.getSpecification());
            row.setOrderedQuantity(ordered);
            row.setCommittedQuantity(c);
            row.setRemainingQuantity(remaining);
            rows.add(row);
        }
        return rows;
    }

    @Override
    public Long getTodayInboundCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        return inboundRecordMapper.countTodayInbound(todayStart, todayEnd);
    }

    private InboundReceiptBatch resolveReceiptBatch(Long orderId, Long receiptBatchId, InboundRecord inboundRecord,
                                                    LocalDateTime batchArrivalAt) {
        if (receiptBatchId != null) {
            InboundReceiptBatch b = inboundReceiptBatchMapper.selectById(receiptBatchId);
            if (b == null) {
                throw new ServiceException("到货批次不存在");
            }
            if (!b.getOrderId().equals(orderId)) {
                throw new ServiceException("到货批次与所选采购订单不一致");
            }
            return b;
        }
        if (!StringUtils.hasText(inboundRecord.getDeliveryNoteNumber())) {
            throw new ServiceException("请填写随货同行单编号；若与上一笔为同一车到货，请传入 receiptBatchId 沿用批次");
        }
        LocalDateTime at = batchArrivalAt != null
                ? batchArrivalAt
                : (inboundRecord.getArrivalDate() != null
                        ? inboundRecord.getArrivalDate().atStartOfDay()
                        : LocalDateTime.now());
        InboundReceiptBatch b = new InboundReceiptBatch();
        b.setBatchCode(generateReceiptBatchCode());
        b.setOrderId(orderId);
        b.setDeliveryNoteNumber(inboundRecord.getDeliveryNoteNumber().trim());
        b.setArrivalTime(at);
        b.setDeliveryNoteImage(inboundRecord.getDeliveryNoteImage());
        b.setOperatorId(inboundRecord.getOperatorId());
        inboundReceiptBatchMapper.insert(b);
        return b;
    }

    private String generateReceiptBatchCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<InboundReceiptBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InboundReceiptBatch::getCreateTime, today.atStartOfDay())
                .lt(InboundReceiptBatch::getCreateTime, today.plusDays(1).atStartOfDay());
        long count = inboundReceiptBatchMapper.selectCount(wrapper);
        String sequence = String.format("%03d", count + 1);
        return "RB" + dateStr + sequence;
    }

    private void applyDispositionForUnqualified(InboundRecord r) {
        if (!"UNQUALIFIED".equals(r.getStatus())) {
            r.setDispositionCode(null);
            r.setDispositionRemark(null);
            return;
        }
        String code = r.getDispositionCode();
        if (!StringUtils.hasText(code)) {
            r.setDispositionCode(InboundDispositionCodes.PENDING);
        } else {
            code = code.trim();
            if (!InboundDispositionCodes.isValid(code)) {
                throw new ServiceException("无效的不合格处置意向代码：" + code);
            }
            r.setDispositionCode(code);
        }
        if (StringUtils.hasText(r.getDispositionRemark())) {
            r.setDispositionRemark(r.getDispositionRemark().trim());
        } else {
            r.setDispositionRemark(null);
        }
    }

    private InboundRecord createInboundWithGeneratedNumber(InboundRecord inboundRecord) {
        applyDispositionForUnqualified(inboundRecord);
        String recordNumber = generateRecordNumber();
        inboundRecord.setRecordNumber(recordNumber);
        inboundRecordMapper.insert(inboundRecord);
        if ("UNQUALIFIED".equals(inboundRecord.getStatus())) {
            applicationEventPublisher.publishEvent(new UnqualifiedInboundRecordedEvent(this, inboundRecord));
        }
        return inboundRecord;
    }

    private String generateRecordNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<InboundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InboundRecord::getCreateTime, today.atStartOfDay());
        wrapper.lt(InboundRecord::getCreateTime, today.plusDays(1).atStartOfDay());
        long count = inboundRecordMapper.selectCount(wrapper);
        String sequence = String.format("%03d", count + 1);
        return "IN" + dateStr + sequence;
    }
}
