package com.example.SafeTag_BE.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QrLinkService {
    @Value("${app.base-url}")
    private String baseUrl;

    public String linkFor(String uuid){
        return baseUrl + "/c/" + uuid;
    }
}
