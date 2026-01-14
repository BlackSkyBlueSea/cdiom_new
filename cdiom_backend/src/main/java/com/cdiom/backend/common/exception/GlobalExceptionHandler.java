package com.cdiom.backend.common.exception;

import com.cdiom.backend.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 * 
 * @author cdiom
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常：{}", e.getMessage());
        String message = "参数校验失败";
        var fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        } else if (!e.getBindingResult().getAllErrors().isEmpty()) {
            var firstError = e.getBindingResult().getAllErrors().get(0);
            if (firstError.getDefaultMessage() != null) {
                message = firstError.getDefaultMessage();
            }
        }
        return Result.error(400, message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        log.error("参数绑定异常：{}", e.getMessage());
        String message = "参数绑定失败";
        var fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        } else if (!e.getBindingResult().getAllErrors().isEmpty()) {
            var firstError = e.getBindingResult().getAllErrors().get(0);
            if (firstError.getDefaultMessage() != null) {
                message = firstError.getDefaultMessage();
            }
        }
        return Result.error(400, message);
    }

    /**
     * 认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.error("认证异常：{}", e.getMessage());
        return Result.error(401, "未登录或登录已过期");
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.error("权限不足：{}", e.getMessage());
        return Result.error(403, "权限不足");
    }

    /**
     * 用户名或密码错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<?> handleBadCredentialsException(BadCredentialsException e) {
        log.error("用户名或密码错误：{}", e.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 数据库完整性约束违反异常
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<?> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        log.error("数据库约束违反异常：{}", e.getMessage());
        String message = e.getMessage();
        
        // 解析错误消息，提供友好的错误提示
        if (message != null) {
            // 检查是否是手机号唯一性约束
            if (message.contains("uk_phone") || (message.contains("Duplicate entry") && message.contains("phone"))) {
                String phone = extractDuplicateValue(message);
                if (!phone.isEmpty()) {
                    return Result.error(400, "手机号 " + phone + " 已被使用，请使用其他手机号");
                }
                return Result.error(400, "手机号已被使用，请使用其他手机号");
            }
            // 检查是否是用户名唯一性约束
            else if (message.contains("uk_username") || (message.contains("Duplicate entry") && message.contains("username"))) {
                String username = extractDuplicateValue(message);
                if (!username.isEmpty()) {
                    return Result.error(400, "用户名 " + username + " 已存在，请使用其他用户名");
                }
                return Result.error(400, "用户名已存在，请使用其他用户名");
            }
            // 检查是否是邮箱唯一性约束
            else if (message.contains("uk_email") || (message.contains("Duplicate entry") && message.contains("email"))) {
                String email = extractDuplicateValue(message);
                if (!email.isEmpty()) {
                    return Result.error(400, "邮箱 " + email + " 已被使用，请使用其他邮箱");
                }
                return Result.error(400, "邮箱已被使用，请使用其他邮箱");
            }
            // 其他重复键错误
            else if (message.contains("Duplicate entry")) {
                String value = extractDuplicateValue(message);
                if (!value.isEmpty()) {
                    return Result.error(400, "数据 " + value + " 已存在，请检查输入信息");
                }
                return Result.error(400, "数据已存在，请检查输入信息");
            }
        }
        
        return Result.error(400, "数据完整性约束违反，请检查输入信息");
    }
    
    /**
     * 从错误消息中提取重复的值
     * 支持格式：Duplicate entry '15788803638' for key 'sys_user.uk_phone'
     */
    private String extractDuplicateValue(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        // 尝试从 "Duplicate entry 'value' for key" 格式中提取值
        // 例如：Duplicate entry '15788803638' for key 'sys_user.uk_phone'
        if (message.contains("'")) {
            int start = message.indexOf("'");
            int end = message.indexOf("'", start + 1);
            if (end > start) {
                return message.substring(start + 1, end);
            }
        }
        return "";
    }

    /**
     * RuntimeException异常（业务异常）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // 如果是业务相关的RuntimeException，返回原始消息
        // 这些异常通常包含用户友好的错误信息
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(500, "系统内部错误，请联系管理员");
    }
}

