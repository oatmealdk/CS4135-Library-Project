package com.elibrary.notification_service.integration.messaging;

import com.elibrary.notification_service.application.NotificationDispatchService;
import com.elibrary.notification_service.infrastructure.messaging.NotificationRabbitConfig;
import com.elibrary.notification_service.integration.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Consumes JSON messages from borrowing-service via the {@code borrowing.events} exchange.
 * Borrowing domain logic lives in borrowing-service only; this class only deserialises and dispatches.
 */
@Component
public class IncomingEventListener {

    private static final Logger log = LoggerFactory.getLogger(IncomingEventListener.class);

    private final JsonMapper jsonMapper;
    private final NotificationDispatchService dispatchService;

    public IncomingEventListener(JsonMapper jsonMapper, NotificationDispatchService dispatchService) {
        this.jsonMapper = jsonMapper;
        this.dispatchService = dispatchService;
    }

    @RabbitListener(queues = NotificationRabbitConfig.QUEUE_NAME)
    public void onMessage(Message message) throws IOException {
        String rk = message.getMessageProperties().getReceivedRoutingKey();
        if (rk == null) {
            log.warn("Missing routing key on incoming message");
            return;
        }
        byte[] body = message.getBody();
        switch (rk) {
            case EventRoutingKeys.BOOK_BORROWED -> dispatchService.handleBorrowed(
                jsonMapper.readValue(body, BookBorrowedMessage.class));
            case EventRoutingKeys.BOOK_RENEWED -> dispatchService.handleRenewed(
                jsonMapper.readValue(body, BookRenewedMessage.class));
            case EventRoutingKeys.BOOK_OVERDUE -> dispatchService.handleOverdue(
                jsonMapper.readValue(body, BookOverdueMessage.class));
            case EventRoutingKeys.FINE_APPLIED -> dispatchService.handleFineApplied(
                jsonMapper.readValue(body, FineAppliedMessage.class));
            case EventRoutingKeys.BOOK_RETURNED -> log.debug("Return event received — no notification action");
            default -> log.warn("Unhandled routing key: {}", rk);
        }
    }
}
