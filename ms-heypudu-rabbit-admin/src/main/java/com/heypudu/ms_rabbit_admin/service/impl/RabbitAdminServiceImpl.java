package com.heypudu.ms_rabbit_admin.service.impl;

import com.heypudu.ms_rabbit_admin.dto.BindingRequest;
import com.heypudu.ms_rabbit_admin.dto.ExchangeRequest;
import com.heypudu.ms_rabbit_admin.dto.QueueRequest;
import com.heypudu.ms_rabbit_admin.service.RabbitAdminService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private static final Pattern EXCHANGE_NAME = Pattern.compile("^ex\\.heypudu\\.[a-z0-9.-]+$");
    private static final Pattern QUEUE_NAME = Pattern.compile("^q\\.heypudu\\.[a-z0-9.-]+$");
    private static final Pattern ROUTING_KEY = Pattern.compile("^[a-z0-9.*#-]+$");
    private static final int DEFAULT_TTL_MS = 3_600_000;
    private static final int DEFAULT_MAX_LENGTH = 10_000;
    private static final int DEFAULT_DELIVERY_LIMIT = 3;

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminServiceImpl(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    @Override
    public void createExchange(String name, ExchangeRequest request) {
        validateExchangeName(name);
        Exchange exchange = switch (request.type().trim().toLowerCase(Locale.ROOT)) {
            case "direct" -> new DirectExchange(name, true, false);
            case "topic" -> new TopicExchange(name, true, false);
            default -> throw new IllegalArgumentException(
                    "El tipo de exchange debe ser direct o topic");
        };
        rabbitAdmin.declareExchange(exchange);
    }

    @Override
    public void deleteExchange(String name) {
        validateExchangeName(name);
        rabbitAdmin.deleteExchange(name);
    }

    @Override
    public void createQueue(String name, QueueRequest request) {
        validateQueueName(name);
        QueueBuilder builder = QueueBuilder.durable(name).quorum();

        int ttl = valueOrDefault(request.messageTtlMs(), DEFAULT_TTL_MS);
        int maxLength = valueOrDefault(request.maxLength(), DEFAULT_MAX_LENGTH);
        int deliveryLimit = valueOrDefault(request.deliveryLimit(), DEFAULT_DELIVERY_LIMIT);

        builder.ttl(ttl)
                .maxLength(maxLength)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .deliveryLimit(deliveryLimit);

        if (StringUtils.hasText(request.deadLetterExchange())
                || StringUtils.hasText(request.deadLetterRoutingKey())) {
            if (!StringUtils.hasText(request.deadLetterExchange())
                    || !StringUtils.hasText(request.deadLetterRoutingKey())) {
                throw new IllegalArgumentException(
                        "deadLetterExchange y deadLetterRoutingKey deben enviarse juntos");
            }
            validateExchangeName(request.deadLetterExchange());
            builder.deadLetterExchange(request.deadLetterExchange())
                    .deadLetterRoutingKey(request.deadLetterRoutingKey().trim());
        }

        rabbitAdmin.declareQueue(builder.build());
    }

    @Override
    public void deleteQueue(String name) {
        validateQueueName(name);
        rabbitAdmin.deleteQueue(name);
    }

    @Override
    public void createBinding(BindingRequest request) {
        Binding binding = toBinding(request);
        rabbitAdmin.declareBinding(binding);
    }

    @Override
    public void deleteBinding(BindingRequest request) {
        Binding binding = toBinding(request);
        rabbitAdmin.removeBinding(binding);
    }

    private Binding toBinding(BindingRequest request) {
        validateExchangeName(request.exchange());
        validateQueueName(request.queue());
        if (!ROUTING_KEY.matcher(request.routingKey().trim()).matches()) {
            throw new IllegalArgumentException(
                    "La routing key solo puede contener letras minusculas, numeros, puntos, guiones, * o #");
        }
        return new Binding(
                request.queue().trim(),
                Binding.DestinationType.QUEUE,
                request.exchange().trim(),
                request.routingKey().trim(),
                null
        );
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private void validateExchangeName(String name) {
        if (!StringUtils.hasText(name) || !EXCHANGE_NAME.matcher(name.trim()).matches()) {
            throw new IllegalArgumentException(
                    "El exchange debe usar el formato ex.heypudu.<nombre>");
        }
    }

    private void validateQueueName(String name) {
        if (!StringUtils.hasText(name) || !QUEUE_NAME.matcher(name.trim()).matches()) {
            throw new IllegalArgumentException(
                    "La cola debe usar el formato q.heypudu.<nombre>");
        }
    }
}
