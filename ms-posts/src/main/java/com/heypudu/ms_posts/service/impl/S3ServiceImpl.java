package com.heypudu.ms_posts.service.impl;

import com.heypudu.ms_posts.dto.request.PresignedUrlRequest;
import com.heypudu.ms_posts.dto.response.PresignedUrlResponse;
import com.heypudu.ms_posts.exception.BusinessException;
import com.heypudu.ms_posts.service.S3Service;
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

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.audio-bucket}")
    private String audioBucket;

    @Value("${aws.s3.covers-bucket}")
    private String coversBucket;

    public S3ServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public PresignedUrlResponse generateAudioUploadUrl(PresignedUrlRequest request) {
        String objectKey = buildObjectKey("audios", request.fileName());
        return generateUploadUrl(audioBucket, objectKey, request.contentType());
    }

    @Override
    public PresignedUrlResponse generateCoverUploadUrl(PresignedUrlRequest request) {
        String objectKey = buildObjectKey("covers", request.fileName());
        return generateUploadUrl(coversBucket, objectKey, request.contentType());
    }

    @Override
    public String generateAudioDownloadUrl(String audioKey) {
        return generateDownloadUrl(audioBucket, audioKey);
    }

    @Override
    public String generateCoverDownloadUrl(String coverImageKey) {
        return generateDownloadUrl(coversBucket, coverImageKey);
    }

    @Override
    public void deleteAudio(String audioKey) {
        deleteObject(audioBucket, audioKey);
    }

    @Override
    public void deleteCover(String coverImageKey) {
        deleteObject(coversBucket, coverImageKey);
    }

    private PresignedUrlResponse generateUploadUrl(String bucket, String objectKey, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
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

    private String generateDownloadUrl(String bucket, String objectKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_TTL)
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    private void deleteObject(String bucket, String objectKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (Exception ex) {
            log.warn("No se pudo eliminar el objeto {} del bucket {}: {}",
                    objectKey, bucket, ex.getMessage());
        }
    }

    private String buildObjectKey(String prefix, String fileName) {
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return prefix + "/" + UUID.randomUUID() + "-" + sanitized;
    }
}