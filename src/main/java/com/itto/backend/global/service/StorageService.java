package com.itto.backend.global.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadFile(MultipartFile file, String directory);
    void deleteFile(String fileUrl);
}
