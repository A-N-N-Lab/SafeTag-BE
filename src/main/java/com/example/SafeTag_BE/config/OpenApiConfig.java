// src/main/java/com/example/SafeTag_BE/config/OpenApiConfig.java
package com.example.SafeTag_BE.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
// ▼ 모든 API에 기본적으로 JWT 필요하게 하려면 아래 @OpenAPIDefinition 유지
@OpenAPIDefinition(
        info = @Info(title = "SafeTag API", version = "v1"),
        security = { @SecurityRequirement(name = "bearerAuth") }
)
// ▼ JWT Bearer 스키마 정의 (Authorize 버튼 생성)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig { }
