package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.*;
import com.cdiom.backend.model.*;
import com.cdiom.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysConfigMapper sysConfigMapper;
    private final SysNoticeMapper sysNoticeMapper;
    private final DrugInfoMapper drugInfoMapper;
    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final InventoryMapper inventoryMapper;
    private final InboundRecordMapper inboundRecordMapper;
    private final OutboundApplyMapper outboundApplyMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 用户统计
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getDeleted, 0);
        long userCount = sysUserMapper.selectCount(userWrapper);
        
        userWrapper.eq(SysUser::getStatus, 1);
        long activeUserCount = sysUserMapper.selectCount(userWrapper);
        
        statistics.put("totalUsers", userCount);
        statistics.put("activeUsers", activeUserCount);
        statistics.put("disabledUsers", userCount - activeUserCount);

        // 角色统计
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRole::getDeleted, 0);
        long roleCount = sysRoleMapper.selectCount(roleWrapper);
        statistics.put("totalRoles", roleCount);

        // 系统配置统计
        LambdaQueryWrapper<SysConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(SysConfig::getDeleted, 0);
        long configCount = sysConfigMapper.selectCount(configWrapper);
        statistics.put("totalConfigs", configCount);

        // 通知公告统计
        LambdaQueryWrapper<SysNotice> noticeWrapper = new LambdaQueryWrapper<>();
        noticeWrapper.eq(SysNotice::getDeleted, 0);
        long noticeCount = sysNoticeMapper.selectCount(noticeWrapper);
        
        noticeWrapper.eq(SysNotice::getStatus, 1);
        long activeNoticeCount = sysNoticeMapper.selectCount(noticeWrapper);
        
        statistics.put("totalNotices", noticeCount);
        statistics.put("activeNotices", activeNoticeCount);

        // 药品信息统计
        LambdaQueryWrapper<DrugInfo> drugWrapper = new LambdaQueryWrapper<>();
        drugWrapper.eq(DrugInfo::getDeleted, 0);
        long drugCount = drugInfoMapper.selectCount(drugWrapper);
        
        drugWrapper.eq(DrugInfo::getIsSpecial, 1);
        long specialDrugCount = drugInfoMapper.selectCount(drugWrapper);
        
        statistics.put("totalDrugs", drugCount);
        statistics.put("specialDrugs", specialDrugCount);
        statistics.put("normalDrugs", drugCount - specialDrugCount);

        // 今日登录统计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<LoginLog> loginWrapper = new LambdaQueryWrapper<>();
        loginWrapper.ge(LoginLog::getLoginTime, todayStart);
        loginWrapper.eq(LoginLog::getStatus, 1);
        long todayLoginCount = loginLogMapper.selectCount(loginWrapper);
        statistics.put("todayLogins", todayLoginCount);

        // 今日操作统计
        LambdaQueryWrapper<OperationLog> operationWrapper = new LambdaQueryWrapper<>();
        operationWrapper.ge(OperationLog::getOperationTime, todayStart);
        operationWrapper.eq(OperationLog::getStatus, 1);
        long todayOperationCount = operationLogMapper.selectCount(operationWrapper);
        statistics.put("todayOperations", todayOperationCount);

        return statistics;
    }

    @Override
    public Map<String, Object> getLoginTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> successCounts = new ArrayList<>();
        List<Long> failCounts = new ArrayList<>();

        // 获取最近7天的数据
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            LambdaQueryWrapper<LoginLog> successWrapper = new LambdaQueryWrapper<>();
            successWrapper.ge(LoginLog::getLoginTime, startOfDay);
            successWrapper.lt(LoginLog::getLoginTime, endOfDay);
            successWrapper.eq(LoginLog::getStatus, 1);
            long successCount = loginLogMapper.selectCount(successWrapper);

            LambdaQueryWrapper<LoginLog> failWrapper = new LambdaQueryWrapper<>();
            failWrapper.ge(LoginLog::getLoginTime, startOfDay);
            failWrapper.lt(LoginLog::getLoginTime, endOfDay);
            failWrapper.eq(LoginLog::getStatus, 0);
            long failCount = loginLogMapper.selectCount(failWrapper);

            dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
            successCounts.add(successCount);
            failCounts.add(failCount);
        }

        result.put("dates", dates);
        result.put("successCounts", successCounts);
        result.put("failCounts", failCounts);

        return result;
    }

    @Override
    public Map<String, Object> getOperationLogStatistics() {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        Map<String, Long> moduleStats = new HashMap<>();
        Map<String, Long> typeStats = new HashMap<>();

        // 获取最近7天的数据
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(OperationLog::getOperationTime, startOfDay);
            wrapper.lt(OperationLog::getOperationTime, endOfDay);
            wrapper.eq(OperationLog::getStatus, 1);
            long count = operationLogMapper.selectCount(wrapper);

            dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
            counts.add(count);
        }

        // 获取模块统计（最近7天）
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(7).atStartOfDay();
        LambdaQueryWrapper<OperationLog> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.ge(OperationLog::getOperationTime, sevenDaysAgo);
        moduleWrapper.eq(OperationLog::getStatus, 1);
        moduleWrapper.isNotNull(OperationLog::getModule);
        List<OperationLog> moduleLogs = operationLogMapper.selectList(moduleWrapper);
        
        Map<String, Long> moduleCountMap = moduleLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getModule() != null ? log.getModule() : "其他",
                        Collectors.counting()
                ));
        moduleStats.putAll(moduleCountMap);

        // 获取操作类型统计（最近7天）
        LambdaQueryWrapper<OperationLog> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.ge(OperationLog::getOperationTime, sevenDaysAgo);
        typeWrapper.eq(OperationLog::getStatus, 1);
        typeWrapper.isNotNull(OperationLog::getOperationType);
        List<OperationLog> typeLogs = operationLogMapper.selectList(typeWrapper);
        
        Map<String, Long> typeCountMap = typeLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getOperationType() != null ? log.getOperationType() : "其他",
                        Collectors.counting()
                ));
        typeStats.putAll(typeCountMap);

        result.put("dates", dates);
        result.put("counts", counts);
        result.put("moduleStats", moduleStats);
        result.put("typeStats", typeStats);

        return result;
    }

    @Override
    public Map<String, Object> getWarehouseDashboard() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 预警日期计算
            LocalDate today = LocalDate.now();
            LocalDate yellowWarningDate = today.plusDays(180); // 180天后
            LocalDate redWarningDate = today.plusDays(90);   // 90天后

            // 近效期预警统计（基于药品有效期）
            // 黄色预警：90-180天
            Long yellowWarningCount = inventoryMapper.countYellowWarning(today, yellowWarningDate);
            if (yellowWarningCount == null) {
                yellowWarningCount = 0L;
            }
            
            // 红色预警：≤90天
            Long redWarningCount = inventoryMapper.countRedWarning(today, redWarningDate);
            if (redWarningCount == null) {
                redWarningCount = 0L;
            }

            // 待办任务统计
            // 待入库订单数：状态为SHIPPED（已发货）的订单
            LambdaQueryWrapper<PurchaseOrder> pendingInboundWrapper = new LambdaQueryWrapper<>();
            pendingInboundWrapper.eq(PurchaseOrder::getStatus, "SHIPPED");
            long pendingInboundCount = purchaseOrderMapper.selectCount(pendingInboundWrapper);
            
            // 待审批出库数：状态为PENDING（待审批）的出库申请
            Long pendingOutboundCount = outboundApplyMapper.countPendingOutbound();
            if (pendingOutboundCount == null) {
                pendingOutboundCount = 0L;
            }

            // 今日出入库统计
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
            
            // 今日入库数
            Long todayInboundCount = inboundRecordMapper.countTodayInbound(todayStart, todayEnd);
            if (todayInboundCount == null) {
                todayInboundCount = 0L;
            }
            
            // 今日出库数
            Long todayOutboundCount = outboundApplyMapper.countTodayOutbound(todayStart, todayEnd);
            if (todayOutboundCount == null) {
                todayOutboundCount = 0L;
            }

            // 库存总量统计
            Long totalInventory = inventoryMapper.getTotalInventory();
            if (totalInventory == null) {
                totalInventory = 0L;
            }

            // 最近7天出入库趋势
            List<String> dates = new ArrayList<>();
            List<Long> inboundCounts = new ArrayList<>();
            List<Long> outboundCounts = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                
                // 当日入库数
                Long dayInboundCount = inboundRecordMapper.countTodayInbound(startOfDay, endOfDay);
                if (dayInboundCount == null) {
                    dayInboundCount = 0L;
                }
                
                // 当日出库数
                Long dayOutboundCount = outboundApplyMapper.countTodayOutbound(startOfDay, endOfDay);
                if (dayOutboundCount == null) {
                    dayOutboundCount = 0L;
                }
                
                dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
                inboundCounts.add(dayInboundCount);
                outboundCounts.add(dayOutboundCount);
            }

            Map<String, Long> nearExpiryWarning = new HashMap<>();
            nearExpiryWarning.put("yellow", yellowWarningCount);
            nearExpiryWarning.put("red", redWarningCount);

            Map<String, Long> pendingTasks = new HashMap<>();
            pendingTasks.put("pendingInbound", pendingInboundCount);
            pendingTasks.put("pendingOutbound", pendingOutboundCount);

            Map<String, Long> todayStats = new HashMap<>();
            todayStats.put("inbound", todayInboundCount);
            todayStats.put("outbound", todayOutboundCount);

            result.put("nearExpiryWarning", nearExpiryWarning);
            result.put("pendingTasks", pendingTasks);
            result.put("todayStats", todayStats);
            result.put("totalInventory", totalInventory);
            result.put("dates", dates);
            result.put("inboundCounts", inboundCounts);
            result.put("outboundCounts", outboundCounts);

            return result;
        } catch (Exception e) {
            // 记录异常日志，但返回默认空数据，避免前端报错
            log.error("获取仓库管理员仪表盘数据异常", e);
            Map<String, Object> result = new HashMap<>();
            Map<String, Long> nearExpiryWarning = new HashMap<>();
            nearExpiryWarning.put("yellow", 0L);
            nearExpiryWarning.put("red", 0L);
            Map<String, Long> pendingTasks = new HashMap<>();
            pendingTasks.put("pendingInbound", 0L);
            pendingTasks.put("pendingOutbound", 0L);
            Map<String, Long> todayStats = new HashMap<>();
            todayStats.put("inbound", 0L);
            todayStats.put("outbound", 0L);
            result.put("nearExpiryWarning", nearExpiryWarning);
            result.put("pendingTasks", pendingTasks);
            result.put("todayStats", todayStats);
            result.put("totalInventory", 0L);
            result.put("dates", new ArrayList<>());
            result.put("inboundCounts", new ArrayList<>());
            result.put("outboundCounts", new ArrayList<>());
            return result;
        }
    }

    @Override
    public Map<String, Object> getPurchaserDashboard(Long purchaserId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 订单统计
            LambdaQueryWrapper<PurchaseOrder> allWrapper = new LambdaQueryWrapper<>();
            allWrapper.eq(PurchaseOrder::getPurchaserId, purchaserId);
            long totalOrders = purchaseOrderMapper.selectCount(allWrapper);
            
            // 订单状态统计
            Map<String, Long> statusStats = new HashMap<>();
            String[] statuses = {"PENDING", "REJECTED", "CONFIRMED", "SHIPPED", "RECEIVED", "CANCELLED"};
            for (String status : statuses) {
                LambdaQueryWrapper<PurchaseOrder> statusWrapper = new LambdaQueryWrapper<>();
                statusWrapper.eq(PurchaseOrder::getPurchaserId, purchaserId);
                statusWrapper.eq(PurchaseOrder::getStatus, status);
                long count = purchaseOrderMapper.selectCount(statusWrapper);
                statusStats.put(status, count);
            }
            
            // 订单金额统计
            List<PurchaseOrder> orders = purchaseOrderMapper.selectList(allWrapper);
            double totalAmount = orders.stream()
                    .filter(o -> o.getTotalAmount() != null)
                    .mapToDouble(o -> o.getTotalAmount().doubleValue())
                    .sum();
            
            // 最近7天订单趋势
            List<String> dates = new ArrayList<>();
            List<Long> orderCounts = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                
                LambdaQueryWrapper<PurchaseOrder> trendWrapper = new LambdaQueryWrapper<>();
                trendWrapper.eq(PurchaseOrder::getPurchaserId, purchaserId);
                trendWrapper.ge(PurchaseOrder::getCreateTime, startOfDay);
                trendWrapper.lt(PurchaseOrder::getCreateTime, endOfDay);
                long count = purchaseOrderMapper.selectCount(trendWrapper);
                
                dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
                orderCounts.add(count);
            }
            
            // 供应商统计
            LambdaQueryWrapper<PurchaseOrder> supplierWrapper = new LambdaQueryWrapper<>();
            supplierWrapper.eq(PurchaseOrder::getPurchaserId, purchaserId);
            supplierWrapper.select(PurchaseOrder::getSupplierId);
            List<PurchaseOrder> supplierOrders = purchaseOrderMapper.selectList(supplierWrapper);
            long uniqueSuppliers = supplierOrders.stream()
                    .map(PurchaseOrder::getSupplierId)
                    .distinct()
                    .count();
            
            result.put("totalOrders", totalOrders);
            result.put("statusStats", statusStats);
            result.put("totalAmount", totalAmount);
            result.put("dates", dates);
            result.put("orderCounts", orderCounts);
            result.put("uniqueSuppliers", uniqueSuppliers);
            
            return result;
        } catch (Exception e) {
            log.error("获取采购专员仪表盘数据异常", e);
            return getEmptyPurchaserDashboard();
        }
    }

    @Override
    public Map<String, Object> getMedicalStaffDashboard(Long applicantId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 出库申请统计
            LambdaQueryWrapper<OutboundApply> allWrapper = new LambdaQueryWrapper<>();
            allWrapper.eq(OutboundApply::getApplicantId, applicantId);
            long totalApplies = outboundApplyMapper.selectCount(allWrapper);
            
            // 申请状态统计
            Map<String, Long> statusStats = new HashMap<>();
            String[] statuses = {"PENDING", "APPROVED", "REJECTED", "OUTBOUND", "CANCELLED"};
            for (String status : statuses) {
                LambdaQueryWrapper<OutboundApply> statusWrapper = new LambdaQueryWrapper<>();
                statusWrapper.eq(OutboundApply::getApplicantId, applicantId);
                statusWrapper.eq(OutboundApply::getStatus, status);
                long count = outboundApplyMapper.selectCount(statusWrapper);
                statusStats.put(status, count);
            }
            
            // 最近7天申请趋势
            List<String> dates = new ArrayList<>();
            List<Long> applyCounts = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                
                LambdaQueryWrapper<OutboundApply> trendWrapper = new LambdaQueryWrapper<>();
                trendWrapper.eq(OutboundApply::getApplicantId, applicantId);
                trendWrapper.ge(OutboundApply::getCreateTime, startOfDay);
                trendWrapper.lt(OutboundApply::getCreateTime, endOfDay);
                long count = outboundApplyMapper.selectCount(trendWrapper);
                
                dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
                applyCounts.add(count);
            }
            
            result.put("totalApplies", totalApplies);
            result.put("statusStats", statusStats);
            result.put("dates", dates);
            result.put("applyCounts", applyCounts);
            
            return result;
        } catch (Exception e) {
            log.error("获取医护人员仪表盘数据异常", e);
            return getEmptyMedicalStaffDashboard();
        }
    }

    @Override
    public Map<String, Object> getSupplierDashboard(Long supplierId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 订单统计
            LambdaQueryWrapper<PurchaseOrder> allWrapper = new LambdaQueryWrapper<>();
            allWrapper.eq(PurchaseOrder::getSupplierId, supplierId);
            long totalOrders = purchaseOrderMapper.selectCount(allWrapper);
            
            // 订单状态统计
            Map<String, Long> statusStats = new HashMap<>();
            String[] statuses = {"PENDING", "REJECTED", "CONFIRMED", "SHIPPED", "RECEIVED", "CANCELLED"};
            for (String status : statuses) {
                LambdaQueryWrapper<PurchaseOrder> statusWrapper = new LambdaQueryWrapper<>();
                statusWrapper.eq(PurchaseOrder::getSupplierId, supplierId);
                statusWrapper.eq(PurchaseOrder::getStatus, status);
                long count = purchaseOrderMapper.selectCount(statusWrapper);
                statusStats.put(status, count);
            }
            
            // 订单金额统计
            List<PurchaseOrder> orders = purchaseOrderMapper.selectList(allWrapper);
            double totalAmount = orders.stream()
                    .filter(o -> o.getTotalAmount() != null)
                    .mapToDouble(o -> o.getTotalAmount().doubleValue())
                    .sum();
            
            double pendingAmount = orders.stream()
                    .filter(o -> o.getTotalAmount() != null && "PENDING".equals(o.getStatus()))
                    .mapToDouble(o -> o.getTotalAmount().doubleValue())
                    .sum();
            
            double confirmedAmount = orders.stream()
                    .filter(o -> o.getTotalAmount() != null && "CONFIRMED".equals(o.getStatus()))
                    .mapToDouble(o -> o.getTotalAmount().doubleValue())
                    .sum();
            
            // 待处理订单数（PENDING状态）
            long pendingOrders = statusStats.getOrDefault("PENDING", 0L);
            
            // 最近30天订单趋势
            List<String> dates = new ArrayList<>();
            List<Long> orderCounts = new ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                
                LambdaQueryWrapper<PurchaseOrder> trendWrapper = new LambdaQueryWrapper<>();
                trendWrapper.eq(PurchaseOrder::getSupplierId, supplierId);
                trendWrapper.ge(PurchaseOrder::getCreateTime, startOfDay);
                trendWrapper.lt(PurchaseOrder::getCreateTime, endOfDay);
                long count = purchaseOrderMapper.selectCount(trendWrapper);
                
                dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
                orderCounts.add(count);
            }
            
            result.put("totalOrders", totalOrders);
            result.put("statusStats", statusStats);
            result.put("totalAmount", totalAmount);
            result.put("pendingAmount", pendingAmount);
            result.put("confirmedAmount", confirmedAmount);
            result.put("pendingOrders", pendingOrders); // 待处理订单数
            result.put("dates", dates);
            result.put("orderCounts", orderCounts);
            
            return result;
        } catch (Exception e) {
            log.error("获取供应商仪表盘数据异常", e);
            return getEmptySupplierDashboard();
        }
    }

    private Map<String, Object> getEmptyPurchaserDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalOrders", 0L);
        result.put("statusStats", new HashMap<String, Long>());
        result.put("totalAmount", 0.0);
        result.put("dates", new ArrayList<>());
        result.put("orderCounts", new ArrayList<>());
        result.put("uniqueSuppliers", 0L);
        return result;
    }

    private Map<String, Object> getEmptyMedicalStaffDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalApplies", 0L);
        result.put("statusStats", new HashMap<String, Long>());
        result.put("dates", new ArrayList<>());
        result.put("applyCounts", new ArrayList<>());
        return result;
    }

    private Map<String, Object> getEmptySupplierDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalOrders", 0L);
        result.put("statusStats", new HashMap<String, Long>());
        result.put("totalAmount", 0.0);
        result.put("pendingAmount", 0.0);
        result.put("confirmedAmount", 0.0);
        result.put("dates", new ArrayList<>());
        result.put("orderCounts", new ArrayList<>());
        return result;
    }
}


