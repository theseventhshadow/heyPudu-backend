package com.heypudu.ms_rabbit_admin.dto;

import jakarta.validation.constraints.Min;

public record QueueRequest(
        @Min(value = 1000, message = "messageTtlMs debe ser mayor o igual a 1000")
        Integer messageTtlMs,
        @Min(value = 1, message = "maxLength debe ser mayor que cero")
        Integer maxLength,
        @Min(value = 1, message = "deliveryLimit debe ser mayor que cero")
        Integer deliveryLimit,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {
}
