package com.heypudu.ms_live.config.amqp;

/**
 * Nombres de exchanges, colas y routing keys de la mensajería de heyPudú
 * usados por {@code ms-live}.
 */
public final class RabbitTopology {

    private RabbitTopology() {
    }

    public static final String COMMAND_EXCHANGE = "ex.heypudu.cmd";
    public static final String EVENTS_EXCHANGE = "ex.heypudu.events";
    public static final String DLX_EXCHANGE = "ex.heypudu.dlx";

    /** Cola propia de comandos de salas; no reutiliza {@code q.heypudu.command}. */
    public static final String LIVE_COMMAND_QUEUE = "q.heypudu.live.command";
    public static final String LIVE_COMMAND_QUEUE_DLQ = "q.heypudu.live.command.dlq";

    /** Evento de sala abierta: lo consume {@code ms-users} para notificar seguidores. */
    public static final String RK_ROOM_CREATED = "room.created";

    /** Comando de sala cerrada: lo consume {@code ms-live} para limpiar reacciones. */
    public static final String RK_ROOM_CLOSED = "room.closed";
}
