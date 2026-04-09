package com.elibrary.borrowing_service.web;

import com.elibrary.borrowing_service.application.BorrowingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background scheduler that detects overdue borrow records once per day.
 *
 * For each ACTIVE or RENEWED record where today > dueDate:
 *   1. Transitions the record's status to OVERDUE
 *   2. Publishes a BookOverdue event to RabbitMQ (borrowing.book.overdue)
 *
 * The Notification context will consume those events to alert the patron.
 * It'll run at 01:00 every day - early enough to catch overnight overdues
 * before patrons start borrowing in the morning, although I'm not sure how
 * thoroughly we'll test this considering time simluation will be required.
 */
@Component
public class OverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueScheduler.class);

    private final BorrowingService borrowingService;

    public OverdueScheduler(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void detectOverdueRecords() {
        log.info("Running overdue detection...");
        borrowingService.markOverdueRecords();
        log.info("Overdue detection complete.");
    }
}
