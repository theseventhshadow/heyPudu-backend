package com.heypudu.ms_rabbit_admin.service.impl;

import com.heypudu.ms_rabbit_admin.dto.BindingRequest;
import com.heypudu.ms_rabbit_admin.dto.ExchangeRequest;
import com.heypudu.ms_rabbit_admin.dto.QueueRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitAdminServiceImplTest {

    private RabbitAdmin rabbitAdmin;
    private RabbitAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        rabbitAdmin = mock(RabbitAdmin.class);
        service = new RabbitAdminServiceImpl(rabbitAdmin);
    }

    @Test
    void createsDirectExchangeInsideHeypuduNamespace() {
        service.createExchange("ex.heypudu.test", new ExchangeRequest("direct"));

        ArgumentCaptor<Exchange> exchange = ArgumentCaptor.forClass(Exchange.class);
        verify(rabbitAdmin).declareExchange(exchange.capture());

        assertEquals("direct", exchange.getValue().getType());
        assertEquals("ex.heypudu.test", exchange.getValue().getName());
        assertEquals(true, exchange.getValue().isDurable());
    }

    @Test
    void createsQuorumQueueWithRetentionPolicies() {
        service.createQueue(
                "q.heypudu.test",
                new QueueRequest(5_000, 100, 2, "ex.heypudu.dlx", "q.heypudu.test.dlq")
        );

        ArgumentCaptor<Queue> queue = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(queue.capture());

        assertEquals("q.heypudu.test", queue.getValue().getName());
        assertEquals("quorum", queue.getValue().getArguments().get("x-queue-type"));
        assertEquals(5_000, ((Number) queue.getValue().getArguments()
                .get("x-message-ttl")).intValue());
        assertEquals(100, ((Number) queue.getValue().getArguments()
                .get("x-max-length")).intValue());
        assertEquals(2, ((Number) queue.getValue().getArguments()
                .get("x-delivery-limit")).intValue());
        assertEquals("ex.heypudu.dlx",
                queue.getValue().getArguments().get("x-dead-letter-exchange"));
        assertEquals("q.heypudu.test.dlq",
                queue.getValue().getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void createsBindingWithValidatedNamesAndRoutingKey() {
        service.createBinding(new BindingRequest(
                "ex.heypudu.events",
                "q.heypudu.audit",
                "interaction.*"
        ));

        ArgumentCaptor<Binding> binding = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareBinding(binding.capture());

        assertEquals("ex.heypudu.events", binding.getValue().getExchange());
        assertEquals("q.heypudu.audit", binding.getValue().getDestination());
        assertEquals("interaction.*", binding.getValue().getRoutingKey());
    }

    @Test
    void rejectsResourcesOutsideHeypuduNamespace() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteExchange("amq.direct")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createBinding(new BindingRequest(
                        "ex.heypudu.events",
                        "q.heypudu.audit",
                        "Interaction.Invalid"
                ))
        );
    }
}
