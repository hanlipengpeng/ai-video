package com.ai.comic.aigc.client;

import com.ai.comic.aigc.AgnesHttpException;
import com.ai.comic.aigc.KeyPool;
import com.ai.comic.aigc.NoAvailableKeyException;
import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.aigc.entity.ApiKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Function;

/**
 * Agnes AI 客户端基类。
 * <p>
 * 封装通用的「选 Key → 调用 → 429 重试」逻辑（文档 15.6）。
 */
@Slf4j
public abstract class AbstractAgnesClient {

    @Autowired
    protected AgnesProperties agnesProperties;

    @Autowired
    protected KeyPool keyPool;

    @Autowired
    protected WebClient agnesWebClient;

    /**
     * 执行一次带 Key 轮询重试的调用。
     * <p>
     * 流程：
     * <ol>
     *   <li>从 KeyPool 选一个可用 Key；</li>
     *   <li>用该 Key 调用 Agnes；</li>
     *   <li>若 429 → 标记 COOLED，切换下一个 Key 重试（最多 maxRetries 次）；</li>
     *   <li>若 401 → 标记 DISABLED，切换下一个 Key 重试；</li>
     *   <li>若 5xx → 抛出由调用方决定是否退避重试；</li>
     *   <li>若无可用 Key → 抛 NoAvailableKeyException。</li>
     * </ol>
     *
     * @param modelType 模型类型（用于选 Key）
     * @param caller    实际调用函数，入参为选中的 Key，返回调用结果
     * @return 调用结果
     */
    protected <T> T executeWithKeyRetry(com.ai.comic.aigc.ModelType modelType,
                                        Function<ApiKey, T> caller) {
        int maxRetries = agnesProperties.getRateLimit().isRetryOn429()
                ? agnesProperties.getRateLimit().getMaxRetries() : 0;
        AgnesHttpException lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            ApiKey key;
            try {
                key = keyPool.select(modelType);
            } catch (NoAvailableKeyException e) {
                // 无可用 Key，向上抛出
                throw e;
            }
            try {
                return caller.apply(key);
            } catch (WebClientResponseException e) {
                HttpStatusCode status = e.getStatusCode();
                int code = status.value();
                log.warn("Agnes 调用失败: keyId={}, name={}, httpStatus={}, body={}",
                        key.getId(), key.getName(), code, e.getResponseBodyAsString());
                if (code == 429) {
                    // 限流：标记 COOLED，切换 Key 重试
                    keyPool.markCooled(key.getId());
                    lastError = new AgnesHttpException(429, "Agnes 限流: " + e.getResponseBodyAsString(), e);
                    continue;
                }
                if (code == 401) {
                    // 鉴权失败：标记 DISABLED，切换 Key 重试
                    keyPool.markDisabled(key.getId());
                    lastError = new AgnesHttpException(401, "Agnes 鉴权失败: " + e.getResponseBodyAsString(), e);
                    continue;
                }
                // 4xx 业务错误（如 400 参数错误）不重试
                if (status.is4xxClientError()) {
                    throw new AgnesHttpException(code, "Agnes 业务错误: " + e.getResponseBodyAsString(), e);
                }
                // 5xx 服务端错误
                throw new AgnesHttpException(code, "Agnes 服务端错误: " + e.getResponseBodyAsString(), e);
            } catch (Exception e) {
                // 非响应异常（网络、超时等）
                if (e instanceof AgnesHttpException) {
                    throw (AgnesHttpException) e;
                }
                throw new AgnesHttpException(0, "Agnes 调用网络异常: " + e.getMessage(), e);
            }
        }
        // 重试次数用尽
        throw lastError != null ? lastError
                : new AgnesHttpException(429, "Agnes 调用重试次数用尽，无可用 Key");
    }

    /**
     * 构建鉴权 header 值。
     */
    protected String bearer(ApiKey key) {
        return "Bearer " + key.getApiKey();
    }
}
