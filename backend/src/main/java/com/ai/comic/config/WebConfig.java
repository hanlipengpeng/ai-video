package com.ai.comic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：将本地存储目录映射为静态资源 URL，便于前端访问生成的图片/视频。
 * <p>
 * 例如本地路径 /data/comic-storage/frame/xxx.png → URL /storage/frame/xxx.png
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        StorageProperties.Storage storage = storageProperties.getStorage();
        String localPath = storage.getLocalPath();
        // 确保路径以斜杠结尾
        if (!localPath.endsWith(File.separator) && !localPath.endsWith("/")) {
            localPath = localPath + "/";
        }
        String urlPrefix = storage.getUrlPrefix();
        if (!urlPrefix.endsWith("/")) {
            urlPrefix = urlPrefix + "/";
        }
        // file: 协议必须以 / 结尾
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations("file:" + localPath);
    }
}
