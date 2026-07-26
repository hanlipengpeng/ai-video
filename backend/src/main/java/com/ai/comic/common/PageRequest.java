package com.ai.comic.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通用分页请求参数（文档 7.1：page、size）。
 */
@Data
public class PageRequest {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码最小为 1")
    private int page = 1;

    /** 每页大小，1~100 */
    @Min(value = 1, message = "每页大小最小为 1")
    @Max(value = 100, message = "每页大小最大为 100")
    private int size = 20;

    /** MyBatis-Plus 的 offset 偏移量 */
    public long offset() {
        return (long) (page - 1) * size;
    }
}
