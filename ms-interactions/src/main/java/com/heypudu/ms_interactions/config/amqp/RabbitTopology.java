package com.heypudu.ms_interactions.config.amqp;

/**
 * Nombres de exchanges, colas y routing keys de la mensajeria de heyPudu.
 * Centralizados aqui para que ningun productor ni consumidor escriba literales.
 */
public final class RabbitTopology {

    private RabbitTopology() {
    }

    public static final String COMMAND_EXCHANGE = "ex.heypudu.cmd";
    public static final String EVENTS_EXCHANGE = "ex.heypudu.events";
    public static final String DLX_EXCHANGE = "ex.heypudu.dlx";

    public static final String COMMAND_QUEUE = "q.heypudu.command";
    public static final String COMMAND_QUEUE_DLQ = "q.heypudu.command.dlq";
    public static final String NOTIFICATION_QUEUE = "q.heypudu.notification";
    public static final String NOTIFICATION_QUEUE_DLQ = "q.heypudu.notification.dlq";
    public static final String AUDIT_QUEUE = "q.heypudu.audit";
    public static final String AUDIT_QUEUE_DLQ = "q.heypudu.audit.dlq";

    /** Comando de borrado de publicacion: lo publica ms-posts, lo consume ms-interactions. */
    public static final String RK_POST_DELETE = "post.delete";

    /** Eventos de interaccion: los publica ms-interactions. */
    public static final String RK_INTERACTION_LIKE = "interaction.liked";
    public static final String RK_INTERACTION_COMMENT = "interaction.commented";

    /** Evento de seguimiento: lo publica ms-users. */
    public static final String RK_USER_FOLLOWED = "user.followed";

    /** Patron de binding para la cola de auditoria: recibe todo lo relevante. */
    public static final String RK_AUDIT_PATTERN = "interaction.*";
    public static final String RK_AUDIT_FOLLOW_PATTERN = "user.#";
}
