package com.ai.comic.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结构（文档 7.1）。
 *
 * <pre>
 * { "code": 0, "message": "ok", "data": { ... } }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务状态码：0 表示成功，非 0 表示失败 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(0, message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(1, message, null);
    }
}
