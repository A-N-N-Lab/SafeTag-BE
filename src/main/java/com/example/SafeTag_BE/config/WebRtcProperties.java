package com.example.SafeTag_BE.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "webrtc")
public class WebRtcProperties {

    private List<String> stun = new ArrayList<>();
    private Turn turn = new Turn();

    @Data
    public static class Turn {
        /** coturn use-auth-secret 의 static-auth-secret 에 대응 */
        private String restSecret;
        /** coturn realm (ex: dupiona.site / safetag 등) */
        private String realm = "safetag";
        /** seconds */
        private int ttlSeconds = 120;
        /** 여러 개 URL 지원 (turn, turns 섞기 가능) */
        private List<String> urls = new ArrayList<>();
    }
}
