package com.ai.comic.account.controller;

import com.ai.comic.account.entity.User;
import com.ai.comic.account.security.JwtUtil;
import com.ai.comic.account.service.UserService;
import com.ai.comic.common.CurrentUser;
import com.ai.comic.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器（文档 7.2.1）。
 * <p>
 * - POST /api/v1/auth/register 注册<br>
 * - POST /api/v1/auth/login 登录返回 JWT<br>
 * - GET /api/v1/users/me 当前用户信息
 */
@Tag(name = "认证", description = "注册 / 登录 / 当前用户")
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 注册请求体 */
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /** 登录请求体 */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Operation(summary = "注册（用户名+密码）")
    @PostMapping("/auth/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.register(req.getUsername(), req.getPassword());
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return Result.ok(data);
    }

    @Operation(summary = "登录，返回 JWT")
    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return Result.ok(data);
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/users/me")
    public Result<Map<String, Object>> me() {
        User user = CurrentUser.get();
        if (user == null) {
            return Result.fail(401, "未登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return Result.ok(data);
    }
}
