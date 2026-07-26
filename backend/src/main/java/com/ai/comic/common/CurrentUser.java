package com.ai.comic.common;

import com.ai.comic.account.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文工具。
 * <p>
 * 从 SecurityContext 中获取 JwtAuthenticationFilter 注入的当前用户。
 */
public final class CurrentUser {

    private CurrentUser() {}

    /**
     * 获取当前登录用户实体（由 JwtAuthenticationFilter 注入 principal）。
     *
     * @return 当前用户；未登录返回 null
     */
    public static User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return principal instanceof User ? (User) principal : null;
    }

    /**
     * 获取当前登录用户 ID。未登录抛业务异常。
     */
    public static Long requireId() {
        User u = get();
        if (u == null) {
            throw new BusinessException(401, "未登录");
        }
        return u.getId();
    }
}
