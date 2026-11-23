package com.heypudu.ms_analytics.dto;

import java.time.Instant;

/**
 * Métricas agregadas de publicaciones, calculadas a partir de {@code heypudu.posts.events}.
 *
 * @param created    publicaciones creadas desde que arrancó el servicio
 * @param deleted    publicaciones eliminadas desde que arrancó el servicio
 * @param published  diferencia entre creadas y eliminadas
 * @param updatedAt  instante del último evento aplicado
 */
public record PostStatsResponse(
        long created,
        long deleted,
        long published,
        Instant updatedAt) {
}
