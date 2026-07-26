package com.ai.comic.aigc.client;

import com.ai.comic.aigc.AgnesHttpException;
import com.ai.comic.aigc.ModelType;
import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.aigc.entity.ApiKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agnes 图像模型客户端（文档 13.3、第 14 章）。
 * <p>
 * 端点：POST /v1/images/generations（同步）。
 * 模型：agnes-image-2.1-flash。
 * 支持文生图与图生图（传 image 数组）。
 */
@Slf4j
@Component
public class AgnesImageClient extends AbstractAgnesClient {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 图像生成结果。
     */
    @Data
    public static class ImageResult {
        /** 图片 URL */
        private String url;
        /** 随机种子（若 Agnes 返回） */
        private Long seed;
    }

    /**
     * 文生图。
     *
     * @param prompt 提示词
     * @param size   尺寸：1K / 2K / 3K / 4K
     * @param ratio  宽高比：1:1 / 9:16 / 16:9 ...
     * @return 图片 URL
     */
    public ImageResult textToImage(String prompt, String size, String ratio) {
        return executeWithKeyRetry(ModelType.IMAGE, key -> doGenerate(key, prompt, size, ratio, null));
    }

    /**
     * 图生图（角色一致性方案，文档 14.4）。
     *
     * @param prompt         提示词
     * @param size           尺寸
     * @param ratio          宽高比
     * @param referenceImageUrls 参考图 URL 数组（角色参考图）
     * @return 图片 URL
     */
    public ImageResult imageToImage(String prompt, String size, String ratio, List<String> referenceImageUrls) {
        return executeWithKeyRetry(ModelType.IMAGE, key -> doGenerate(key, prompt, size, ratio, referenceImageUrls));
    }

    private ImageResult doGenerate(ApiKey key, String prompt, String size, String ratio, List<String> refImages) {
        String model = agnesProperties.getModels().getImage();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("prompt", prompt);
        if (size != null && !size.isBlank()) {
            body.put("size", size);
        }
        if (ratio != null && !ratio.isBlank()) {
            body.put("ratio", ratio);
        }
        if (refImages != null && !refImages.isEmpty()) {
            ArrayNode arr = body.putArray("image");
            for (String url : refImages) {
                arr.add(url);
            }
        }
        // 返回 URL 格式
        ObjectNode extra = body.putObject("extra_body");
        extra.put("response_format", "url");

        JsonNode resp = agnesWebClient.post()
                .uri(agnesProperties.getBaseUrl() + "/images/generations")
                .header("Authorization", bearer(key))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (resp == null) {
            throw new AgnesHttpException(0, "Agnes 图像调用返回空响应");
        }
        // OpenAI 兼容结构：data[0].url
        JsonNode data = resp.path("data");
        if (data.isMissingNode() || !data.isArray() || data.isEmpty()) {
            log.warn("Agnes 图像响应缺少 data: {}", resp);
            throw new AgnesHttpException(0, "Agnes 图像响应格式异常");
        }
        JsonNode first = data.get(0);
        String url = first.path("url").asText(null);
        if (url == null) {
            // 兼容 b64_json
            String b64 = first.path("b64_json").asText(null);
            if (b64 != null) {
                url = "data:image/png;base64," + b64;
            }
        }
        ImageResult result = new ImageResult();
        result.setUrl(url);
        JsonNode seedNode = resp.path("seed");
        if (!seedNode.isMissingNode() && seedNode.canConvertToLong()) {
            result.setSeed(seedNode.asLong());
        }
        log.debug("Agnes 图像调用成功: model={}, keyId={}, url={}", model, key.getId(), url);
        return result;
    }
}
