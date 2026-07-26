package com.ai.comic.aigc;

/**
 * 无可用 Key 异常：所有 Key 都冷却/禁用/达 RPM 上限时抛出。
 * <p>
 * 调用方应捕获此异常，让任务保持 RUNNING 状态（前端提示「排队中」，文档 15.6）。
 */
public class NoAvailableKeyException extends RuntimeException {

    public NoAvailableKeyException(String message) {
        super(message);
    }
}
