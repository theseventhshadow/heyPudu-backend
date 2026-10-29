package com.heypudu.ms_posts.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreatePostReportRequest(
        @NotBlank
        @Pattern(regexp = "SPAM|HARASSMENT|HATE_SPEECH|INAPPROPRIATE_CONTENT|OTHER")
        String reason
) {
}
