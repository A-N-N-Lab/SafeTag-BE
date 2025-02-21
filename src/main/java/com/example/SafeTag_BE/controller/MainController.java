package com.example.SafeTag_BE.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String home() {
        return "redirect:/user/signup"; // 회원가입 페이지로 리다이렉트
    }
}
