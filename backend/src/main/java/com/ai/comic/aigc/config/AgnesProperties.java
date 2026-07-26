package com.ai.comic.aigc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agnes AI 接入配置（文档 15.8）。
 * <p>
 * 绑定 application.yml 中的 agnes.ai.* 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agnes.ai")
public class AgnesProperties {

    /** Agnes AI Base URL，如 https://apihub.agnes-ai.com/v1 */
    private String baseUrl;

    /** 调用超时（秒） */
    private int timeoutSec = 120;

    /** Key 池配置 */
    private List<KeyConfig> keys = new ArrayList<>();

    /** 限流策略 */
    private RateLimit rateLimit = new RateLimit();

    /** 各 plan 的 RPM 上限 */
    private RpmConfig rpm = new RpmConfig();

    /** 模型名配置 */
    private Models models = new Models();

    /** 视频异步任务轮询配置 */
    private Video video = new Video();

    @Data
    public static class KeyConfig {
        /** Key 标识 */
        private String name;
        /** 明文 Key */
        private String apiKey;
        /** 订阅计划：free / token-plan / enterprise */
        private String plan = "free";
    }

    @Data
    public static class RateLimit {
        /** 429 后冷却秒数 */
        private int coolDownSec = 60;
        /** 是否在 429 时切换 Key 重试 */
        private boolean retryOn429 = true;
        /** 最大重试次数（= Key 数 - 1） */
        private int maxRetries = 3;
    }

    @Data
    public static class RpmConfig {
        private PlanRpm free = new PlanRpm(20, 30, 1);
        private PlanRpm tokenPlan = new PlanRpm(1000, 100, 5);
        private PlanRpm enterprise = new PlanRpm(1000, 100, 5);

        /**
         * 根据计划名获取 RPM 配置（不区分大小写，连字符兼容）。
         */
        public PlanRpm ofPlan(String plan) {
            if (plan == null) {
                return free;
            }
            return switch (plan.toLowerCase().replace("-", "")) {
                case "tokenplan" -> tokenPlan;
                case "enterprise" -> enterprise;
                default -> free;
            };
        }
    }

    @Data
    public static class PlanRpm {
        /** 文本模型 RPM */
        private int text;
        /** 图像模型 RPM */
        private int image;
        /** 视频模型 RPM */
        private int video;

        public PlanRpm() {}

        public PlanRpm(int text, int image, int video) {
            this.text = text;
            this.image = image;
            this.video = video;
        }
    }

    @Data
    public static class Models {
        private String text = "agnes-2.0-flash";
        private String image = "agnes-image-2.1-flash";
        private String video = "agnes-video-v2.0";
    }

    @Data
    public static class Video {
        /** 轮询间隔（毫秒） */
        private long pollIntervalMs = 10000L;
        /** 单片段最大轮询时长（毫秒），超时判失败 */
        private long maxPollDurationMs = 300000L;
        /** 查询结果路径：GET {base-url}{query-path}?video_id=xxx */
        private String queryPath = "/agnesapi";
    }
}
