package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cdiom.backend.mapper.SysRoleMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.model.LoginLog;
import com.cdiom.backend.model.SysRole;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.IpLocationService;
import com.cdiom.backend.service.LoginLogService;
import com.cdiom.backend.util.JwtUtil;
import com.cdiom.backend.util.LoginConfigUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务实现类
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final LoginLogService loginLogService;
    private final IpLocationService ipLocationService;
    private final JwtUtil jwtUtil;
    private final LoginConfigUtil loginConfigUtil;
    private final PlatformTransactionManager transactionManager;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String DEBUG_LOG_PATH = System.getProperty("user.dir") + "/.cursor/debug.log";
    private static final String SESSION_ID = "debug-session-" + UUID.randomUUID().toString().substring(0, 8);
    
    // #region agent log
    private void debugLog(String hypothesisId, String location, String message, java.util.Map<String, Object> data) {
        try {
            StringBuilder dataJson = new StringBuilder("{");
            if (data != null) {
                boolean first = true;
                for (java.util.Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!first) dataJson.append(",");
                    dataJson.append("\"").append(entry.getKey()).append("\":");
                    Object value = entry.getValue();
                    if (value == null) {
                        dataJson.append("null");
                    } else if (value instanceof String) {
                        dataJson.append("\"").append(value.toString().replace("\"", "\\\"").replace("\n", "\\n")).append("\"");
                    } else if (value instanceof Number || value instanceof Boolean) {
                        dataJson.append(value);
                    } else {
                        dataJson.append("\"").append(value.toString().replace("\"", "\\\"").replace("\n", "\\n")).append("\"");
                    }
                    first = false;
                }
            }
            dataJson.append("}");
            String logEntry = String.format("{\"sessionId\":\"%s\",\"runId\":\"run1\",\"hypothesisId\":\"%s\",\"location\":\"%s\",\"message\":\"%s\",\"data\":%s,\"timestamp\":%d}%n",
                SESSION_ID, hypothesisId, location, message.replace("\"", "\\\""), dataJson.toString(), System.currentTimeMillis());
            
            // 确保目录存在
            java.nio.file.Path logPath = Paths.get(DEBUG_LOG_PATH);
            java.nio.file.Path parentDir = logPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            Files.write(logPath, logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // 同时在标准日志中输出，方便调试
            log.debug("[DEBUG] {} - {}: {}", hypothesisId, location, message);
        } catch (Exception e) {
            // 输出到标准日志，方便排查问题
            log.error("Failed to write debug log: {}", e.getMessage(), e);
        }
    }
    // #endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object[] login(String username, String password, String ip, String browser, String os) {
        // #region agent log
        log.info("=== DEBUG MODE: Login attempt started ===");
        debugLog("A", "AuthServiceImpl.login:48", "登录请求开始", 
            java.util.Map.of("username", username != null ? username : "null", "ip", ip != null ? ip : "null"));
        // #endregion
        
        // 1. 查询用户（支持用户名或手机号登录）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SysUser::getUsername, username)
                .or().eq(SysUser::getPhone, username));
        SysUser user = sysUserMapper.selectOne(wrapper);
        
        // #region agent log
        debugLog("A", "AuthServiceImpl.login:53", "初始用户查询结果", 
            java.util.Map.of("userId", user != null ? user.getId() : "null", 
                "loginFailCount", user != null && user.getLoginFailCount() != null ? user.getLoginFailCount() : "null",
                "lastLoginFailTime", user != null && user.getLastLoginFailTime() != null ? user.getLastLoginFailTime().toString() : "null"));
        // #endregion

        // 2. 初始化登录日志
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setIp(ip);
        loginLog.setBrowser(browser);
        loginLog.setOs(os);
        // 异步获取IP定位（不阻塞登录流程）
        try {
            String location = ipLocationService.getLocationByIp(ip);
            loginLog.setLocation(location);
        } catch (Exception e) {
            // 如果获取地理位置失败，不影响登录流程，设置为IP地址
            loginLog.setLocation(ip);
        }

        // 3. 前置校验：账号是否处于锁定状态（核心）
        LocalDateTime currentTime = LocalDateTime.now();
        if (user != null) {
            LocalDateTime lockTime = user.getLockTime();
            if (lockTime != null && currentTime.isBefore(lockTime)) {
                long remainingMinutes = Duration.between(currentTime, lockTime).toMinutes();
                loginLog.setStatus(0);
                loginLog.setMsg(String.format("用户已被锁定，剩余解锁时间：%d 分钟", remainingMinutes));
                loginLog.setUserId(user.getId());
                loginLogService.saveLog(loginLog);
                throw new RuntimeException(loginLog.getMsg());
            }

            // 4. 校验用户状态（是否禁用）
            if (user.getStatus() == null || user.getStatus() == 0) {
                loginLog.setStatus(0);
                loginLog.setMsg("用户已被禁用");
                loginLog.setUserId(user.getId());
                loginLogService.saveLog(loginLog);
                throw new RuntimeException("用户已被禁用");
            }
        }

        // 5. 密码验证与防暴力破解逻辑（核心）
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            // 5.1 处理用户不存在/密码错误场景
            if (user != null) {
                // #region agent log
                debugLog("B", "AuthServiceImpl.login:96", "重新查询用户前", 
                    java.util.Map.of("userId", user.getId()));
                // #endregion
                
                // 重新从数据库查询最新用户信息，确保获取最新的失败次数和失败时间
                // 使用selectOne避免MyBatis一级缓存，确保获取最新数据
                LambdaQueryWrapper<SysUser> latestWrapper = new LambdaQueryWrapper<>();
                latestWrapper.eq(SysUser::getId, user.getId());
                SysUser latestUser = sysUserMapper.selectOne(latestWrapper);
                
                // #region agent log
                debugLog("B", "AuthServiceImpl.login:98", "重新查询用户后", 
                    java.util.Map.of("userId", latestUser != null ? latestUser.getId() : "null",
                        "loginFailCount", latestUser != null && latestUser.getLoginFailCount() != null ? latestUser.getLoginFailCount() : "null",
                        "lastLoginFailTime", latestUser != null && latestUser.getLastLoginFailTime() != null ? latestUser.getLastLoginFailTime().toString() : "null",
                        "lockTime", latestUser != null && latestUser.getLockTime() != null ? latestUser.getLockTime().toString() : "null"));
                // #endregion
                
                if (latestUser == null) {
                    loginLog.setStatus(0);
                    loginLog.setMsg("用户不存在");
                    loginLogService.saveLog(loginLog);
                    throw new RuntimeException("用户不存在");
                }
                
                // 动态获取配置规则
                int failThreshold = loginConfigUtil.getLoginFailThreshold();
                int timeWindow = loginConfigUtil.getLoginFailTimeWindow();
                int lockDuration = loginConfigUtil.getLoginLockDuration();
                Duration FAIL_TIME_WINDOW = Duration.ofMinutes(timeWindow);
                
                // #region agent log
                debugLog("C", "AuthServiceImpl.login:109", "配置读取结果", 
                    java.util.Map.of("failThreshold", failThreshold, "timeWindow", timeWindow, "lockDuration", lockDuration));
                // #endregion

                // 计算两次失败的时间间隔（使用最新查询的用户信息）
                LocalDateTime lastFailTime = latestUser.getLastLoginFailTime();
                int currentFailCount = latestUser.getLoginFailCount() == null ? 0 : latestUser.getLoginFailCount();
                int newFailCount;
                String logMsg;

                // #region agent log
                debugLog("D", "AuthServiceImpl.login:113", "失败次数计算前", 
                    java.util.Map.of("currentFailCount", currentFailCount, 
                        "lastFailTime", lastFailTime != null ? lastFailTime.toString() : "null",
                        "currentTime", currentTime.toString()));
                // #endregion

                log.debug("登录失败 - 用户ID: {}, 当前失败次数: {}, 最后失败时间: {}", 
                    latestUser.getId(), currentFailCount, lastFailTime);

                if (lastFailTime != null) {
                    Duration interval = Duration.between(lastFailTime, currentTime);
                    long intervalSeconds = interval.toSeconds();
                    long intervalMinutes = interval.toMinutes();
                    
                    // #region agent log
                    debugLog("D", "AuthServiceImpl.login:121", "时间间隔计算", 
                        java.util.Map.of("intervalSeconds", intervalSeconds, 
                            "intervalMinutes", intervalMinutes,
                            "timeWindowMinutes", timeWindow,
                            "withinWindow", intervalSeconds >= 0 && intervalMinutes <= timeWindow));
                    // #endregion
                    
                    log.debug("时间间隔: {} 秒, 时间窗口: {} 分钟", intervalSeconds, timeWindow);
                    
                    if (interval.compareTo(FAIL_TIME_WINDOW) <= 0 && intervalSeconds >= 0) {
                        // 间隔≤时间窗口且>=0：累加失败次数（暴力破解嫌疑）
                        newFailCount = currentFailCount + 1;
                        // #region agent log
                        debugLog("D", "AuthServiceImpl.login:128", "累加失败次数", 
                            java.util.Map.of("oldCount", currentFailCount, "newCount", newFailCount));
                        // #endregion
                        log.debug("时间窗口内，累加失败次数: {} -> {}", currentFailCount, newFailCount);
                    } else {
                        // 间隔>时间窗口或<0（时间异常）：重置次数为1（正常零散尝试）
                        newFailCount = 1;
                        // #region agent log
                        debugLog("D", "AuthServiceImpl.login:133", "重置失败次数", 
                            java.util.Map.of("reason", "超出时间窗口或时间异常", "newCount", newFailCount));
                        // #endregion
                        log.debug("超出时间窗口，重置失败次数为: {}", newFailCount);
                    }
                } else {
                    // 首次失败：次数为1
                    newFailCount = 1;
                    // #region agent log
                    debugLog("D", "AuthServiceImpl.login:138", "首次失败", 
                        java.util.Map.of("newCount", newFailCount));
                    // #endregion
                    log.debug("首次失败，设置失败次数为: {}", newFailCount);
                }

                // 5.2 判断是否触发锁定，更新用户状态
                latestUser.setLastLoginFailTime(currentTime); // 更新最后失败时间
                
                // #region agent log
                debugLog("E", "AuthServiceImpl.login:142", "准备更新用户状态", 
                    java.util.Map.of("newFailCount", newFailCount, 
                        "failThreshold", failThreshold,
                        "willLock", newFailCount >= failThreshold,
                        "newLastLoginFailTime", currentTime.toString()));
                // #endregion
                
                if (newFailCount >= failThreshold) {
                    // 达到阈值：触发锁定
                    LocalDateTime lockExpireTime = currentTime.plusHours(lockDuration);
                    latestUser.setLoginFailCount(newFailCount);
                    latestUser.setLockTime(lockExpireTime);
                    logMsg = String.format("%d分钟内连续登录失败%d次，账号已锁定%d小时", timeWindow, failThreshold, lockDuration);
                    // #region agent log
                    debugLog("E", "AuthServiceImpl.login:149", "触发账号锁定", 
                        java.util.Map.of("userId", latestUser.getId(), 
                            "failCount", newFailCount,
                            "lockExpireTime", lockExpireTime.toString()));
                    // #endregion
                    log.warn("账号锁定 - 用户ID: {}, 失败次数: {}, 锁定到期时间: {}", 
                        latestUser.getId(), newFailCount, lockExpireTime);
                } else {
                    // 未达阈值：仅更新失败次数
                    latestUser.setLoginFailCount(newFailCount);
                    logMsg = String.format("用户名或密码错误，%d分钟内剩余尝试次数：%d", timeWindow, failThreshold - newFailCount);
                    // #region agent log
                    debugLog("E", "AuthServiceImpl.login:156", "仅更新失败次数", 
                        java.util.Map.of("userId", latestUser.getId(), 
                            "newFailCount", newFailCount,
                            "remainingAttempts", failThreshold - newFailCount));
                    // #endregion
                    log.debug("更新失败次数 - 用户ID: {}, 新失败次数: {}, 剩余尝试次数: {}", 
                        latestUser.getId(), newFailCount, failThreshold - newFailCount);
                }
                
                // #region agent log
                debugLog("A", "AuthServiceImpl.login:158", "调用updateUserFailStatus前", 
                    java.util.Map.of("userId", latestUser.getId(),
                        "loginFailCount", latestUser.getLoginFailCount(),
                        "lastLoginFailTime", latestUser.getLastLoginFailTime() != null ? latestUser.getLastLoginFailTime().toString() : "null",
                        "lockTime", latestUser.getLockTime() != null ? latestUser.getLockTime().toString() : "null"));
                // #endregion
                
                // 独立事务更新用户状态，确保失败次数能够持久化
                updateUserFailStatus(latestUser);
                
                // #region agent log
                debugLog("A", "AuthServiceImpl.login:160", "调用updateUserFailStatus后", 
                    java.util.Map.of("userId", latestUser.getId()));
                // #endregion

                // 5.3 完善失败日志
                loginLog.setUserId(latestUser.getId());
                loginLog.setMsg(logMsg);
            } else {
                // 用户不存在：仅记录基础日志
                loginLog.setMsg("用户名或密码错误");
            }

            loginLog.setStatus(0);
            loginLogService.saveLog(loginLog);
            
            // #region agent log
            debugLog("A", "AuthServiceImpl.login:324", "准备抛出异常前", 
                java.util.Map.of("logMsg", loginLog.getMsg()));
            // #endregion
            
            // 在抛出异常之前，确保独立事务已完全提交
            // 虽然使用了REQUIRES_NEW，但为了确保数据持久化，我们在这里再次验证
            throw new RuntimeException(loginLog.getMsg());
        }

        // 6. 登录成功：重置失败/锁定状态（核心）
        resetUserFailStatus(user);

        // 7. 记录成功日志
        loginLog.setUserId(user.getId());
        loginLog.setStatus(1);
        loginLog.setMsg("登录成功");
        loginLogService.saveLog(loginLog);

        // 8. 获取角色信息
        String roleCode = "USER";
        if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null) {
                roleCode = role.getRoleCode();
            }
        }

        // 9. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);
        user.setPassword(null); // 清除密码信息

        return new Object[]{token, user};
    }

    /**
     * 更新用户失败状态（独立事务，确保失败次数能够持久化）
     * 使用编程式事务确保在方法返回前事务已提交，避免主事务回滚影响
     */
    public void updateUserFailStatus(SysUser user) {
        // #region agent log
        debugLog("A", "AuthServiceImpl.updateUserFailStatus:203", "更新前状态", 
            java.util.Map.of("userId", user.getId(),
                "loginFailCount", user.getLoginFailCount(),
                "lastLoginFailTime", user.getLastLoginFailTime() != null ? user.getLastLoginFailTime().toString() : "null",
                "lockTime", user.getLockTime() != null ? user.getLockTime().toString() : "null"));
        // #endregion
        
        // 使用编程式事务，确保独立提交
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysUser::getId, user.getId())
                    .set(SysUser::getLastLoginFailTime, user.getLastLoginFailTime())
                    .set(SysUser::getLoginFailCount, user.getLoginFailCount());
            
            // 如果设置了锁定时间，也更新锁定时间
            if (user.getLockTime() != null) {
                updateWrapper.set(SysUser::getLockTime, user.getLockTime());
            } else {
                // 如果lockTime为null，也要更新为null（清空锁定）
                updateWrapper.set(SysUser::getLockTime, null);
            }
            
            // #region agent log
            debugLog("A", "AuthServiceImpl.updateUserFailStatus:217", "执行SQL更新前", 
                java.util.Map.of("userId", user.getId(), "updateWrapper", updateWrapper.toString()));
            // #endregion
            
            int updateResult = sysUserMapper.update(null, updateWrapper);
            
            // #region agent log
            debugLog("A", "AuthServiceImpl.updateUserFailStatus:220", "执行SQL更新后", 
                java.util.Map.of("userId", user.getId(), "updateResult", updateResult));
            // #endregion
            
            // 提交事务
            transactionManager.commit(status);
            
            // #region agent log
            debugLog("A", "AuthServiceImpl.updateUserFailStatus:225", "事务已提交", 
                java.util.Map.of("userId", user.getId()));
            
            // 等待一小段时间，确保事务完全提交到数据库
            try {
                Thread.sleep(50); // 等待50毫秒，确保数据库提交完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 验证更新结果：使用新的查询（清除缓存）
            // 使用新的查询条件，避免MyBatis一级缓存
            LambdaQueryWrapper<SysUser> verifyWrapper = new LambdaQueryWrapper<>();
            verifyWrapper.eq(SysUser::getId, user.getId());
            SysUser verifyUser = sysUserMapper.selectOne(verifyWrapper);
            debugLog("A", "AuthServiceImpl.updateUserFailStatus:235", "验证更新结果", 
                java.util.Map.of("userId", verifyUser != null ? verifyUser.getId() : "null",
                    "loginFailCount", verifyUser != null && verifyUser.getLoginFailCount() != null ? verifyUser.getLoginFailCount() : "null",
                    "lastLoginFailTime", verifyUser != null && verifyUser.getLastLoginFailTime() != null ? verifyUser.getLastLoginFailTime().toString() : "null",
                    "lockTime", verifyUser != null && verifyUser.getLockTime() != null ? verifyUser.getLockTime().toString() : "null"));
            // #endregion
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("更新用户失败状态失败", e);
            throw e;
        }
    }

    /**
     * 重置用户失败状态（独立事务，确保重置能够持久化）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void resetUserFailStatus(SysUser user) {
        user.setLoginFailCount(0);          // 失败次数归0
        user.setLastLoginFailTime(null);   // 清空最后失败时间
        user.setLockTime(null);            // 清空锁定时间
        sysUserMapper.updateById(user);
    }

    @Override
    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return null;
        }
        Long userId = (Long) authentication.getPrincipal();
        return sysUserMapper.selectById(userId);
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

}

