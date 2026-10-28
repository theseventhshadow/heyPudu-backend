package com.heypudu.ms_posts.service;

import com.heypudu.ms_posts.dto.request.PresignedUrlRequest;
import com.heypudu.ms_posts.dto.response.PresignedUrlResponse;

public interface S3Service {

    PresignedUrlResponse generateAudioUploadUrl(PresignedUrlRequest request);

    PresignedUrlResponse generateCoverUploadUrl(PresignedUrlRequest request);

    String generateAudioDownloadUrl(String audioKey);

    String generateCoverDownloadUrl(String coverImageKey);

    void deleteAudio(String audioKey);

    void deleteCover(String coverImageKey);
}