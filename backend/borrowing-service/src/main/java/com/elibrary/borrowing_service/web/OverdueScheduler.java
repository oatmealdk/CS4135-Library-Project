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
 * Runs at 01:00 daily. For manual testing without waiting, use
 * {@code POST /api/borrows/maintenance/run-overdue-check} (same service call).
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
        int n = borrowingService.markOverdueRecords();
        log.info("Overdue detection complete ({} record(s) marked overdue).", n);
    }
}
