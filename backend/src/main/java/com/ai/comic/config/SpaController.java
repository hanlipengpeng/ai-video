package com.ai.comic.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * SPA 回退控制器：将前端路由（Vue Router history 模式）的刷新/直接访问请求
 * 转发到 /index.html，由前端路由接管。
 * <p>
 * 仅匹配不含 "." 的路径（排除静态文件如 app.js、style.css、favicon.ico 等）。
 * /api/**、/storage/**、/swagger** 等由各自的 Handler 优先处理，不会进入这里。
 */
@Controller
public class SpaController {

    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping("/{path:[^\\.]*}")
    public String single(@PathVariable String path) {
        return "forward:/index.html";
    }

    @GetMapping("/{path:[^\\.]*}/**")
    public String nested(@PathVariable String path) {
        return "forward:/index.html";
    }
}
