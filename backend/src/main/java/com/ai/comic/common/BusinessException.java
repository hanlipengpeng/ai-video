package com.ai.comic.common;

/**
 * 业务异常基类。携带错误码与错误信息，由 GlobalExceptionHandler 统一处理。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 1;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
