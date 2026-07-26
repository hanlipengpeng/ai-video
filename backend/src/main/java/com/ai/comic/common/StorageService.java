package com.ai.comic.common;

import com.ai.comic.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地存储服务（M0 用本地磁盘替代 OSS）。
 * <p>
 * - 提供下载远程 URL 到本地磁盘的方法；<br>
 * - 返回相对 URL（如 /storage/frame/2026-07-26/xxx.png），由 WebConfig 映射到本地磁盘；<br>
 * - 目录按类型 + 日期分桶，避免单目录文件过多。
 */
@Slf4j
@Service
public class StorageService {

    @Autowired
    private StorageProperties storageProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    /**
     * 下载远程 URL 到本地存储，返回相对 URL。
     *
     * @param remoteUrl 远程 URL（http/https）
     * @param category  文件分类目录，如 frame / character / video / compose
     * @param extension 文件扩展名，如 png / mp4
     * @return 相对 URL，如 /storage/frame/2026-07-26/xxx.png
     */
    public String downloadToLocalStorage(String remoteUrl, String category, String extension) {
        try {
            String dateDir = LocalDate.now().toString();
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path dir = Paths.get(storageProperties.getStorage().getLocalPath(), category, dateDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);

            // data: URI 直接 base64 解码
            if (remoteUrl.startsWith("data:")) {
                byte[] data = decodeDataUri(remoteUrl);
                Files.write(target, data);
            } else {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(remoteUrl))
                        .GET()
                        .build();
                HttpResponse<InputStream> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() / 100 != 2) {
                    throw new IOException("下载失败 HTTP " + resp.statusCode() + ": " + remoteUrl);
                }
                try (InputStream in = resp.body()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            String relPath = category + "/" + dateDir + "/" + fileName;
            String urlPrefix = storageProperties.getStorage().getUrlPrefix();
            if (!urlPrefix.endsWith("/")) {
                urlPrefix = urlPrefix + "/";
            }
            String relativeUrl = urlPrefix + relPath;
            log.debug("已下载到本地: {} -> {}", remoteUrl, relativeUrl);
            return relativeUrl;
        } catch (Exception e) {
            log.error("下载到本地存储失败: {}", remoteUrl, e);
            throw new BusinessException(500, "下载媒体文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取本地文件的绝对路径（用于 FFmpeg 处理）。
     *
     * @param relativeUrl 相对 URL（如 /storage/frame/.../x.png）
     * @return 本地绝对路径
     */
    public Path resolveLocalPath(String relativeUrl) {
        String urlPrefix = storageProperties.getStorage().getUrlPrefix();
        String relPath = relativeUrl;
        if (relPath.startsWith(urlPrefix)) {
            relPath = relPath.substring(urlPrefix.length());
        }
        // 去掉开头的斜杠
        while (relPath.startsWith("/")) {
            relPath = relPath.substring(1);
        }
        return Paths.get(storageProperties.getStorage().getLocalPath(), relPath);
    }

    /**
     * 将相对 URL 转换为可被外部访问的完整 URL（基于当前请求 host）。
     * <p>
     * M0 简化：直接返回相对 URL，前端拼接 host 即可。
     */
    public String toAbsoluteUrl(String relativeUrl, String schemeHost) {
        if (relativeUrl == null) {
            return null;
        }
        if (relativeUrl.startsWith("http") || relativeUrl.startsWith("data:")) {
            return relativeUrl;
        }
        if (schemeHost == null) {
            return relativeUrl;
        }
        if (!relativeUrl.startsWith("/")) {
            relativeUrl = "/" + relativeUrl;
        }
        return schemeHost + relativeUrl;
    }

    private byte[] decodeDataUri(String dataUri) {
        // 格式：data:image/png;base64,XXXX
        int comma = dataUri.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("非法 data URI: " + dataUri.substring(0, Math.min(50, dataUri.length())));
        }
        String meta = dataUri.substring(0, comma);
        String data = dataUri.substring(comma + 1);
        if (!meta.contains("base64")) {
            throw new IllegalArgumentException("仅支持 base64 data URI");
        }
        return java.util.Base64.getDecoder().decode(data);
    }
}
