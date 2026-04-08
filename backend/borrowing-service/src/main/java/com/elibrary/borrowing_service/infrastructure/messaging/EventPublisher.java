package com.elibrary.borrowing_service.infrastructure.messaging;

import com.elibrary.borrowing_service.infrastructure.messaging.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Borrowing domain events to the borrowing.events topic exchange.
 *
 * Each method corresponds to one routing key from the spec's API contracts.
 * The Notification context consumes these via the notification.borrowing.events queue.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishBookBorrowed(BookBorrowedEvent event) {
        log.debug("Publishing BookBorrowed for recordId={}", event.getRecordId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.KEY_BOOK_BORROWED, event);
    }

    public void publishBookReturned(BookReturnedEvent event) {
        log.debug("Publishing BookReturned for recordId={}", event.getRecordId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.KEY_BOOK_RETURNED, event);
    }

    public void publishBookRenewed(BookRenewedEvent event) {
        log.debug("Publishing BookRenewed for recordId={}", event.getRecordId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.KEY_BOOK_RENEWED, event);
    }

    public void publishBookOverdue(BookOverdueEvent event) {
        log.debug("Publishing BookOverdue for recordId={}", event.getRecordId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.KEY_BOOK_OVERDUE, event);
    }

    public void publishFineApplied(FineAppliedEvent event) {
        log.debug("Publishing FineApplied for fineId={}", event.getFineId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.KEY_FINE_APPLIED, event);
    }
}
