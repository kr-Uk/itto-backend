package com.itto.backend.global.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class FirebaseStorageService {

    @Value("${firebase.storage-bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String directory) {
        try {
            String fileName = directory + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

            Blob blob = StorageClient.getInstance().bucket(bucketName)
                    .create(fileName, file.getInputStream(), file.getContentType());

            return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    bucketName, fileName.replace("/", "%2F"));
        } catch (Exception e) {
            log.error("파일 업로드 실패: [{}] {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                    "파일 업로드 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            if (fileName != null) {
                StorageClient.getInstance().bucket(bucketName).get(fileName).delete();
            }
        } catch (Exception e) {
            log.warn("파일 삭제 실패 (무시): {}", e.getMessage());
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private String extractFileNameFromUrl(String url) {
        try {
            int start = url.indexOf("/o/") + 3;
            int end = url.indexOf("?");
            if (start > 2 && end > start) {
                return url.substring(start, end).replace("%2F", "/");
            }
        } catch (Exception e) {
            log.warn("URL 파싱 실패: {}", url);
        }
        return null;
    }
}
