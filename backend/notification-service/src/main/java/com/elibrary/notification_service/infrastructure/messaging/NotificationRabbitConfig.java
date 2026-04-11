package com.elibrary.notification_service.infrastructure.messaging;

import com.elibrary.notification_service.integration.messaging.EventRoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("unused")
public class NotificationRabbitConfig {

    public static final String BORROWING_EVENTS_EXCHANGE = "borrowing.events";
    public static final String QUEUE_NAME = "notification.borrowing.events";

    @Bean
    public TopicExchange externalEventsTopicExchange() {
        return new TopicExchange(BORROWING_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationInboundQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Declarables notificationBindings(TopicExchange externalEventsTopicExchange, Queue notificationInboundQueue) {
        return new Declarables(
            BindingBuilder.bind(notificationInboundQueue).to(externalEventsTopicExchange)
                .with(EventRoutingKeys.BOOK_BORROWED),
            BindingBuilder.bind(notificationInboundQueue).to(externalEventsTopicExchange)
                .with(EventRoutingKeys.BOOK_RETURNED),
            BindingBuilder.bind(notificationInboundQueue).to(externalEventsTopicExchange)
                .with(EventRoutingKeys.BOOK_RENEWED),
            BindingBuilder.bind(notificationInboundQueue).to(externalEventsTopicExchange)
                .with(EventRoutingKeys.BOOK_OVERDUE),
            BindingBuilder.bind(notificationInboundQueue).to(externalEventsTopicExchange)
                .with(EventRoutingKeys.FINE_APPLIED)
        );
    }
}
