package com.example.SafeTag_BE.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    // application.yml에서 설정
    @Value("${firebase.credentials-file}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("[FCM] FirebaseApp already initialized");
                return;
            }

            InputStream in;

            // 1) classpath: 접두어 지원
            if (credentialsPath.startsWith("classpath:")) {
                String cp = credentialsPath.substring("classpath:".length());
                Resource r = new ClassPathResource(cp);
                in = r.getInputStream();

            } else {
                // 2) 절대/상대 경로 파일이 있으면 파일로
                File f = new File(credentialsPath);
                if (f.exists()) {
                    in = new FileInputStream(f);
                } else {
                    // 3) 마지막으로 classpath에서 그대로 찾아보기 (resources에 넣었을 때)
                    Resource r = new ClassPathResource(credentialsPath);
                    in = r.getInputStream();
                }
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("[FCM] FirebaseApp initialized with {}", credentialsPath);

        } catch (Exception e) {
            log.error("[FCM] Firebase init failed: {}", e.getMessage(), e);
        }
    }
}
