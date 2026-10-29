package com.heypudu.ms_users.dto.response;

import java.util.UUID;

public record AuthorReportCountResponse(
        UUID authorId,
        long totalReports
) {
}
