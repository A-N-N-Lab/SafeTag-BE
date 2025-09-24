package com.example.SafeTag_BE.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.base-dir:./uploads}")
    private String baseDir;

    public String save(MultipartFile file) {
        try {
            Files.createDirectories(Path.of(baseDir));
            String fileId = UUID.randomUUID().toString();
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String storedName = fileId + "_" + original;
            Path target = Path.of(baseDir, storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return storedName;
        } catch (Exception e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}
