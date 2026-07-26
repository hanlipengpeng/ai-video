package com.ai.comic.aigc.client;

import com.ai.comic.aigc.AgnesHttpException;
import com.ai.comic.aigc.ModelType;
import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.aigc.entity.ApiKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Agnes 文本模型客户端（文档 13.2）。
 * <p>
 * 端点：POST /v1/chat/completions（同步）。
 * 模型：agnes-2.0-flash，1M 上下文。
 */
@Slf4j
@Component
public class AgnesTextClient extends AbstractAgnesClient {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 调用文本生成（单轮）。
     *
     * @param systemPrompt 系统提示（可空）
     * @param userPrompt   用户输入
     * @return 模型输出的文本内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        return executeWithKeyRetry(ModelType.TEXT, key -> doChat(key, systemPrompt, userPrompt));
    }

    /**
     * 调用文本生成并返回 JSON 字符串（要求模型输出合法 JSON）。
     */
    public String chatForJson(String systemPrompt, String userPrompt) {
        String raw = chat(systemPrompt, userPrompt);
        // 兼容模型可能包裹 ```json ... ``` 的情况
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String doChat(ApiKey key, String systemPrompt, String userPrompt) {
        String model = agnesProperties.getModels().getText();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);
        body.put("stream", false);

        JsonNode resp = agnesWebClient.post()
                .uri(agnesProperties.getBaseUrl() + "/chat/completions")
                .header("Authorization", bearer(key))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (resp == null) {
            throw new AgnesHttpException(0, "Agnes 文本调用返回空响应");
        }
        // OpenAI 兼容结构：choices[0].message.content
        JsonNode content = resp.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode()) {
            log.warn("Agnes 文本响应缺少 content: {}", resp);
            throw new AgnesHttpException(0, "Agnes 文本响应格式异常");
        }
        String text = content.asText();
        log.debug("Agnes 文本调用成功: model={}, keyId={}, 输出长度={}", model, key.getId(), text.length());
        return text;
    }
}
