package com.heypudu.ms_interactions.dto.response;

import java.util.UUID;

public record AuthorReportCountResponse(
        UUID authorId,
        long totalReports
) {
}
