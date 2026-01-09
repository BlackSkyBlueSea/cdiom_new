package com.cdiom.backend.common.exception;

/**
 * 业务异常类
 * 
 * @author cdiom
 */
public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private Integer code;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}

