package com.ai.comic.config;

import com.ai.comic.account.security.JwtAuthenticationFilter;
import com.ai.comic.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置（无状态 + JWT）。
 * <p>
 * - 注册 /api/v1/auth/** 与 swagger 公开<br>
 * - 其余 /api/v1/** 需登录<br>
 * - 静态资源 /storage/** 公开<br>
 * - CORS 全开（M0 内网验证）
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（无状态 JWT）
                .csrf(AbstractHttpConfigurer::disable)
                // CORS 配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态会话
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权规则
                .authorizeHttpRequests(auth -> auth
                        // 预检请求放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 认证接口放行
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // swagger / api-docs 放行
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/favicon.ico").permitAll()
                        // 静态资源放行
                        .requestMatchers("/storage/**").permitAll()
                        // 其余 /api/v1/** 需登录
                        .requestMatchers("/api/v1/**").authenticated()
                        // 其他请求放行
                        .anyRequest().permitAll()
                )
                // 异常处理：未登录返回 401 JSON
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, resp, authEx) -> {
                            resp.setStatus(401);
                            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            resp.setCharacterEncoding("UTF-8");
                            Result<Void> body = Result.fail(401, "未登录或登录已过期");
                            resp.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                        .accessDeniedHandler((req, resp, accessEx) -> {
                            resp.setStatus(403);
                            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            resp.setCharacterEncoding("UTF-8");
                            Result<Void> body = Result.fail(403, "权限不足");
                            resp.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )
                // JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
