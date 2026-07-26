package com.ai.comic.aigc.client;

import com.ai.comic.aigc.AgnesHttpException;
import com.ai.comic.aigc.ModelType;
import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.aigc.entity.ApiKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Agnes 视频模型客户端（文档 13.4、第 16 章）。
 * <p>
 * - 创建任务：POST /v1/videos（异步），返回 task_id / video_id<br>
 * - 查询结果：GET {base-url}{query-path}?video_id=xxx
 */
@Slf4j
@Component
public class AgnesVideoClient extends AbstractAgnesClient {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建视频任务的结果。
     */
    @Data
    public static class CreateResult {
        /** Agnes 任务 ID */
        private String taskId;
        /** Agnes 视频 ID（用于查询） */
        private String videoId;
        /** 初始状态 */
        private String status;
    }

    /**
     * 查询视频任务的结果。
     */
    @Data
    public static class QueryResult {
        /** 状态：processing / succeeded / failed */
        private String status;
        /** 视频下载 URL（成功时返回） */
        private String videoUrl;
        /** 进度百分比 0-100（若 Agnes 返回） */
        private Integer progress;
        /** 错误信息 */
        private String error;
    }

    /**
     * 创建图生视频任务（文档 16.3）。
     *
     * @param prompt      动作描述
     * @param imageUrl    输入图 URL（frame_image_url）
     * @return 创建结果，含 video_id
     */
    public CreateResult createImageToVideo(String prompt, String imageUrl) {
        return executeWithKeyRetry(ModelType.VIDEO, key -> doCreate(key, prompt, imageUrl));
    }

    /**
     * 创建文生视频任务（兜底，无输入图）。
     */
    public CreateResult createTextToVideo(String prompt) {
        return executeWithKeyRetry(ModelType.VIDEO, key -> doCreate(key, prompt, null));
    }

    private CreateResult doCreate(ApiKey key, String prompt, String imageUrl) {
        String model = agnesProperties.getModels().getVideo();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("prompt", prompt);
        if (imageUrl != null && !imageUrl.isBlank()) {
            body.put("image", imageUrl);
        }

        JsonNode resp = agnesWebClient.post()
                .uri(agnesProperties.getBaseUrl() + "/videos")
                .header("Authorization", bearer(key))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (resp == null) {
            throw new AgnesHttpException(0, "Agnes 视频创建返回空响应");
        }
        CreateResult result = new CreateResult();
        result.setTaskId(resp.path("task_id").asText(null));
        result.setVideoId(resp.path("video_id").asText(null));
        result.setStatus(resp.path("status").asText("processing"));
        log.info("Agnes 视频任务创建成功: keyId={}, taskId={}, videoId={}",
                key.getId(), result.getTaskId(), result.getVideoId());
        return result;
    }

    /**
     * 查询视频任务结果（文档 13.4）。
     * <p>
     * GET {base-url}{query-path}?video_id=xxx
     *
     * @param videoId Agnes 返回的 video_id
     * @return 查询结果
     */
    public QueryResult queryVideo(String videoId) {
        // 查询用任一可用 Key 即可（不消耗视频 RPM）
        ApiKey key = keyPool.select(ModelType.VIDEO);
        String queryPath = agnesProperties.getVideo().getQueryPath();
        String url = agnesProperties.getBaseUrl() + queryPath + "?video_id=" + videoId;

        JsonNode resp = agnesWebClient.get()
                .uri(url)
                .header("Authorization", bearer(key))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (resp == null) {
            throw new AgnesHttpException(0, "Agnes 视频查询返回空响应");
        }
        QueryResult result = new QueryResult();
        // 兼容多种字段命名
        String status = firstNonNull(
                resp.path("status").asText(null),
                resp.path("state").asText(null));
        result.setStatus(status == null ? "processing" : status.toLowerCase());
        // 视频地址字段兼容
        String videoUrl = firstNonNull(
                resp.path("video_url").asText(null),
                resp.path("url").asText(null),
                resp.path("output").path("video_url").asText(null));
        result.setVideoUrl(videoUrl);
        JsonNode progressNode = resp.path("progress");
        if (!progressNode.isMissingNode() && progressNode.canConvertToInt()) {
            result.setProgress(progressNode.asInt());
        }
        String error = firstNonNull(
                resp.path("error").asText(null),
                resp.path("error_msg").asText(null));
        result.setError(error);
        log.debug("Agnes 视频任务查询: videoId={}, status={}, progress={}",
                videoId, result.getStatus(), result.getProgress());
        return result;
    }

    private String firstNonNull(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank() && !"null".equals(v)) {
                return v;
            }
        }
        return null;
    }
}
