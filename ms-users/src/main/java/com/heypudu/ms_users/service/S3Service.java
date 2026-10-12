package com.heypudu.ms_users.service;

import com.heypudu.ms_users.dto.request.PresignedUrlRequest;
import com.heypudu.ms_users.dto.response.PresignedUrlResponse;

public interface S3Service {

    PresignedUrlResponse generateAvatarUploadUrl(PresignedUrlRequest request);

    String generateAvatarDownloadUrl(String avatarKey);

    void deleteAvatar(String avatarKey);
}