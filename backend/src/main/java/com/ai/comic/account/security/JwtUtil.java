package com.ai.comic.account.security;

import com.ai.comic.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成与解析 token。
 * <p>
 * 使用 JJWT 0.12.x API。
 */
@Slf4j
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // 使用 HMAC-SHA256 密钥（要求 >= 256 bit = 32 字节）
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String generate(Long userId, String username) {
        long expireMs = jwtProperties.getExpireHours() * 3600_000L;
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("uid", userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 JWT，返回 Claims。失败返回 null。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Claims 中获取用户 ID。
     */
    public Long getUserId(Claims claims) {
        Object uid = claims.get("uid");
        if (uid instanceof Number) {
            return ((Number) uid).longValue();
        }
        return uid == null ? null : Long.valueOf(uid.toString());
    }

    public String getUsername(Claims claims) {
        Object username = claims.get("username");
        return username == null ? null : username.toString();
    }

    public JwtProperties getProperties() {
        return jwtProperties;
    }
}
