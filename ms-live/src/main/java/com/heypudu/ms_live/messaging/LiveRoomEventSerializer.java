package com.heypudu.ms_live.messaging;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Serializa {@link LiveRoomEvent} con fechas ISO-8601 y sin cabeceras de tipo,
 * para que cada consumidor fije su propio DTO.
 */
public class LiveRoomEventSerializer extends JsonSerializer<LiveRoomEvent> {

    public LiveRoomEventSerializer() {
        super(JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build());
        setAddTypeInfo(false);
    }
}
