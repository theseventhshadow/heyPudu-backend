package com.heypudu.ms_users.dto.response;

public record BanCandidateResponse(
        UserProfileResponse user,
        long totalReports,
        long postReports,
        long commentReports
) {
}
