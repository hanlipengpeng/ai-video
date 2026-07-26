package com.ai.comic.account.security;

import com.ai.comic.account.entity.User;
import com.ai.comic.account.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 * <p>
 * 从请求头 Authorization: Bearer &lt;token&gt; 解析 JWT，校验通过后将 User 注入 SecurityContext。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = jwtUtil.getProperties().getHeader();
        String prefix = jwtUtil.getProperties().getPrefix();
        String authHeader = request.getHeader(header);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(prefix)) {
            String token = authHeader.substring(prefix.length());
            Claims claims = jwtUtil.parse(token);
            if (claims != null) {
                Long userId = jwtUtil.getUserId(claims);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userService.getById(userId);
                    if (user != null) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, java.util.Collections.emptyList());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
