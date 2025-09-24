package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.repository.DynamicQRRepository;
import com.example.SafeTag_BE.service.CallSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class RelayController {

    private final DynamicQRRepository dynamicQRRepository;
    private final CallSessionService callSessionService;

    @GetMapping("/c/{uuid}")
    public String landing(@PathVariable String uuid, Model model, HttpServletRequest req) {
        var qr = dynamicQRRepository.findByQrValue(uuid)
                .orElseThrow(() -> new IllegalArgumentException("invalid qr uuid"));

        var cs = callSessionService.createWebRtcRequested(qr.getUser().getId(), null, uuid);

        String forwardedHost = req.getHeader("X-Forwarded-Host");
        String forwardedProto = req.getHeader("X-Forwarded-Proto");
        String scheme = (forwardedProto != null ? forwardedProto : req.isSecure() ? "https" : "http");
        String wsScheme = "https".equalsIgnoreCase(scheme) ? "wss" : "ws";
        String hostPort = (forwardedHost != null && !forwardedHost.isBlank())
                ? forwardedHost
                : req.getServerName() + (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "");

        model.addAttribute("ownerName", qr.getUser().getName());
        model.addAttribute("sessionId", cs.getSessionUuid());
        model.addAttribute("wsUrl", wsScheme + "://" + hostPort + "/ws/signaling");
        return "landing"; // templates/landing.html
    }
}
