package com.heypudu.ms_posts.dto.response;

import java.util.UUID;

public record AuthorReportCountResponse(
        UUID authorId,
        long totalReports
) {
}
