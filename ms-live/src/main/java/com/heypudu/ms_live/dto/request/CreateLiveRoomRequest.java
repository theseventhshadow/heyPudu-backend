package com.heypudu.ms_live.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLiveRoomRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 500) String description
) {
}
