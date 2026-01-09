package com.cdiom.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdiom.backend.mapper.SysRoleMapper;
import com.cdiom.backend.mapper.SysUserMapper;
import com.cdiom.backend.model.LoginLog;
import com.cdiom.backend.model.SysRole;
import com.cdiom.backend.model.SysUser;
import com.cdiom.backend.service.AuthService;
import com.cdiom.backend.service.LoginLogService;
import com.cdiom.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 认证服务实现类
 * 
 * @author cdiom
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final LoginLogService loginLogService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object[] login(String username, String password, String ip, String browser, String os) {
        // 查询用户（支持用户名或手机号登录）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SysUser::getUsername, username)
                .or().eq(SysUser::getPhone, username));
        SysUser user = sysUserMapper.selectOne(wrapper);

        // 记录登录日志
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setIp(ip);
        loginLog.setBrowser(browser);
        loginLog.setOs(os);

        // 用户不存在或密码错误
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginLog.setStatus(0);
            loginLog.setMsg("用户名或密码错误");
            loginLogService.saveLog(loginLog);
            
            // 如果用户存在，增加失败次数
            if (user != null) {
                int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
                user.setLoginFailCount(failCount);
                
                // 连续5次失败锁定1小时
                if (failCount >= 5) {
                    user.setLockTime(LocalDateTime.now().plus(1, ChronoUnit.HOURS));
                }
                sysUserMapper.updateById(user);
            }
            
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            loginLog.setStatus(0);
            loginLog.setMsg("用户已被禁用");
            loginLog.setUserId(user.getId());
            loginLogService.saveLog(loginLog);
            throw new RuntimeException("用户已被禁用");
        }

        // 检查是否被锁定
        if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            loginLog.setStatus(0);
            loginLog.setMsg("账号已被锁定，请稍后再试");
            loginLog.setUserId(user.getId());
            loginLogService.saveLog(loginLog);
            throw new RuntimeException("账号已被锁定，请稍后再试");
        }

        // 登录成功，重置失败次数和锁定时间
        user.setLoginFailCount(0);
        user.setLockTime(null);
        sysUserMapper.updateById(user);

        // 获取角色信息
        String roleCode = "USER";
        if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null) {
                roleCode = role.getRoleCode();
            }
        }

            // 记录成功登录日志
            loginLog.setUserId(user.getId());
            loginLog.setStatus(1);
            loginLog.setMsg("登录成功");
            loginLogService.saveLog(loginLog);

            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);

            // 清除密码信息
            user.setPassword(null);
            
            // 返回token和用户信息
            return new Object[]{token, user};
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

