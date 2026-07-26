package com.ai.comic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 漫剧制作网站后端启动类。
 * <p>
 * 启用异步任务（@Async）和定时任务（@Scheduled，用于视频轮询）。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.ai.comic.**.mapper")
public class ComicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComicApplication.class, args);
    }
}
