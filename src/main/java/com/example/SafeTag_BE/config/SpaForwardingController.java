package com.example.SafeTag_BE.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {
    @RequestMapping({
            "/main",
            "/login",
            "/logout",
            "/mypage",
            "/admin",
            "/sticker",
            "/auth",
            "/chatbot",
            "/scan",
            "/qr/**",
            "/call/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
