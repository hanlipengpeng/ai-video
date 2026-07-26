package com.ai.comic.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 * <p>
 * 统一将异常转换为 {@link Result} 结构，前端按 code 字段判断成功/失败。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        log.warn("业务异常: code={}, msg={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /** 参数校验异常（@RequestBody + @Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(400, "参数校验失败: " + msg);
    }

    /** 参数绑定异常 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(400, "参数绑定失败: " + msg);
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return Result.fail(400, "参数类型不匹配: " + ex.getName());
    }

    /** 鉴权失败 */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuth(AuthenticationException ex, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        return Result.fail(401, "未登录或登录已过期");
    }

    /** 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException ex, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.FORBIDDEN.value());
        return Result.fail(403, "权限不足");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception ex, HttpServletResponse resp) {
        log.error("系统异常", ex);
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Result.fail(500, "系统内部错误: " + ex.getMessage());
    }
}
