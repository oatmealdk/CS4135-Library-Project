package com.elibrary.borrowing_service.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the borrowing.events topic exchange on RabbitMQ.
 *
 * The Notification context binds its own queue to this exchange using routing
 * keys defined previoulsy in our sessions (borrowing.book.*, borrowing.fine.applied)
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "borrowing.events";

    public static final String KEY_BOOK_BORROWED = "borrowing.book.borrowed";
    public static final String KEY_BOOK_RETURNED = "borrowing.book.returned";
    public static final String KEY_BOOK_RENEWED  = "borrowing.book.renewed";
    public static final String KEY_BOOK_OVERDUE  = "borrowing.book.overdue";
    public static final String KEY_FINE_APPLIED  = "borrowing.fine.applied";

    @Bean
    public TopicExchange borrowingEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * Jackson converter delegating to Spring Boot's auto-configured JsonMapper,
     * which already has JavaTimeModule registered and timestamps disabled.
     */
    @Bean
    public JacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
