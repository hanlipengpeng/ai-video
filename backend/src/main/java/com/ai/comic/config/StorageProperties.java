package com.ai.comic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地存储配置，绑定 application.yml 中的 comic.storage.* 与 comic.ffmpeg.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comic")
public class StorageProperties {

    /** 本地存储配置 */
    private Storage storage = new Storage();

    /** FFmpeg 配置 */
    private Ffmpeg ffmpeg = new Ffmpeg();

    @Data
    public static class Storage {
        /** 本地存储根路径 */
        private String localPath = "/data/comic-storage";
        /** 对外暴露的 URL 前缀 */
        private String urlPrefix = "/storage";
    }

    @Data
    public static class Ffmpeg {
        /** FFmpeg 可执行文件路径 */
        private String path = "ffmpeg";
    }
}
