package com.heypudu.ms_users.config.amqp;

public final class RabbitTopology {

    private RabbitTopology() {
    }

    public static final String EVENTS_EXCHANGE = "ex.heypudu.events";
    public static final String DLX_EXCHANGE = "ex.heypudu.dlx";
    public static final String NOTIFICATION_QUEUE = "q.heypudu.notification";
    public static final String NOTIFICATION_QUEUE_DLQ = "q.heypudu.notification.dlq";
    public static final String RK_INTERACTION_LIKE = "interaction.liked";
    public static final String RK_INTERACTION_COMMENT = "interaction.commented";
    public static final String RK_USER_FOLLOWED = "user.followed";
}
