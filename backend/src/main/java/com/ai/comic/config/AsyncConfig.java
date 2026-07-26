package com.ai.comic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置。
 * <p>
 * 用于 @Async 异步执行 AI 生成任务（剧本/分镜/图像/视频片段提交等）。
 * 文档 10：全局并发上限建议 ≤ 3。
 */
@Configuration
public class AsyncConfig {

    /**
     * AI 任务专用线程池：核心 3，最大 5（受 Agnes AI 限流约束，不宜过大）。
     */
    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ai-task-");
        // 队列满时由调用线程执行（背压）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 通用异步线程池：日志、下载等非 AI 任务。
     */
    @Bean("commonExecutor")
    public Executor commonExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("common-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
