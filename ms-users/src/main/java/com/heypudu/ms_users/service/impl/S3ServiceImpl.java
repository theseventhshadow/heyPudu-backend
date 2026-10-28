package com.heypudu.ms_users.service.impl;

import com.heypudu.ms_users.dto.request.PresignedUrlRequest;
import com.heypudu.ms_users.dto.response.PresignedUrlResponse;
import com.heypudu.ms_users.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class S3ServiceImpl implements S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3ServiceImpl.class);

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(60);
    private static final String AVATARS_PREFIX = "avatars";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.avatars-bucket}")
    private String avatarsBucket;

    public S3ServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public PresignedUrlResponse generateAvatarUploadUrl(PresignedUrlRequest request) {
        String objectKey = buildObjectKey(AVATARS_PREFIX, request.fileName());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(avatarsBucket)
                .key(objectKey)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_TTL)
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(
                presigned.url().toString(),
                objectKey,
                Instant.now().plus(UPLOAD_URL_TTL)
        );
    }

    @Override
    public String generateAvatarDownloadUrl(String avatarKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(avatarsBucket)
                .key(avatarKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_TTL)
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    @Override
    public void deleteAvatar(String avatarKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(avatarsBucket)
                    .key(avatarKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (Exception ex) {
            log.warn("No se pudo eliminar el avatar {} del bucket {}: {}",
                    avatarKey, avatarsBucket, ex.getMessage());
        }
    }

    private String buildObjectKey(String prefix, String fileName) {
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return prefix + "/" + UUID.randomUUID() + "-" + sanitized;
    }
}