package com.ai.comic.common;

import lombok.Data;

/**
 * 分页响应数据。
 */
@Data
public class PageResult<T> {

    /** 当前页码（从 1 开始） */
    private long page;
    /** 每页大小 */
    private long size;
    /** 总记录数 */
    private long total;
    /** 当前页数据 */
    private java.util.List<T> list;

    public PageResult(long page, long size, long total, java.util.List<T> list) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.list = list;
    }
}
