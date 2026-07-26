package com.ai.comic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，绑定 application.yml 中的 jwt.* 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT 签名密钥（Base64 或明文） */
    private String secret;

    /** token 有效期（小时） */
    private long expireHours = 72;

    /** 请求头名称 */
    private String header = "Authorization";

    /** token 前缀 */
    private String prefix = "Bearer ";
}
