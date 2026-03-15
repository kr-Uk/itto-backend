package com.itto.backend.infrastructure.minio;

import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url}")
    private String publicUrl;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 버킷 생성 완료: {}", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO 버킷 초기화 실패: {}", e.getMessage(), e);
        }
    }

    private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    );

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            // 파일 타입 검증 (이미지만 허용)
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                        "허용되지 않는 파일 형식입니다. JPEG, PNG, WebP, HEIC만 업로드 가능합니다.");
            }

            String objectName = directory + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            return publicUrl + "/" + objectName;
        } catch (Exception e) {
            log.error("파일 업로드 실패: [{}] {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                    "파일 업로드 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectName(fileUrl);
            if (objectName != null) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build());
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

    private String extractObjectName(String fileUrl) {
        try {
            String prefix = publicUrl + "/";
            if (fileUrl.startsWith(prefix)) {
                return fileUrl.substring(prefix.length());
            }
        } catch (Exception e) {
            log.warn("URL 파싱 실패: {}", fileUrl);
        }
        return null;
    }
}
