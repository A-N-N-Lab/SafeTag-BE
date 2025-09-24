package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.service.TurnCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class IceConfigController {

    private final TurnCredentialService turn;

    // GET /api/ice-config?ttl=120
    @GetMapping("/ice-config")
    public ResponseEntity<Map<String, Object>> iceConfig(
            @RequestParam(name = "ttl", required = false) Integer ttl) {
        Map<String, Object> body = (ttl == null) ? turn.issue() : turn.issue(ttl);
        return ResponseEntity.ok(body);
    }
}
