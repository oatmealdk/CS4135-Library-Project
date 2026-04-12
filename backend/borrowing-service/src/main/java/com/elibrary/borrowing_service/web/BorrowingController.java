package com.elibrary.borrowing_service.web;

import com.elibrary.borrowing_service.application.BorrowingService;
import com.elibrary.borrowing_service.application.dto.BorrowRecordDTO;
import com.elibrary.borrowing_service.application.dto.BorrowRequest;
import com.elibrary.borrowing_service.application.dto.RunOverdueCheckResult;
import com.elibrary.borrowing_service.application.dto.SetDueDateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Borrowing context's published language in DDD section 1.
 *
 * Endpoints implemented to match our API contracts defined in the first DDD stage:
 *   POST   /api/borrows                                -> borrow a book
 *   PUT    /api/borrows/{recordId}/return              -> return a book
 *   PUT    /api/borrows/{recordId}/renew               -> renew a book
 *   GET    /api/borrows/user/{userId}                  -> get all borrows for a user
 *   GET    /api/borrows/{recordId}                     -> get a single borrow record
 *   GET    /api/borrows/book/{bookId}/active           -> active borrows for a book (used by book-service, INV-C2)
 *   POST   /api/borrows/maintenance/run-overdue-check       -> manual overdue job (same as daily scheduler)
 *   PUT    /api/borrows/maintenance/{recordId}/due-date     -> QA: set due date on ACTIVE/RENEWED loan
 */
@RestController
@RequestMapping("/api/borrows")
public class BorrowingController {

    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    @PostMapping
    public ResponseEntity<BorrowRecordDTO> borrowBook(@Valid @RequestBody BorrowRequest request) {
        BorrowRecordDTO result = borrowingService.borrowBook(request.getUserId(), request.getBookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{recordId}/return")
    public ResponseEntity<BorrowRecordDTO> returnBook(@PathVariable Long recordId) {
        return ResponseEntity.ok(borrowingService.returnBook(recordId));
    }

    @PutMapping("/{recordId}/renew")
    public ResponseEntity<BorrowRecordDTO> renewBook(@PathVariable Long recordId) {
        return ResponseEntity.ok(borrowingService.renewBook(recordId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BorrowRecordDTO>> getBorrowsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(borrowingService.getBorrowsByUser(userId));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<BorrowRecordDTO> getBorrowRecord(@PathVariable Long recordId) {
        return ResponseEntity.ok(borrowingService.getBorrowRecord(recordId));
    }

    @GetMapping("/book/{bookId}/active")
    public ResponseEntity<List<BorrowRecordDTO>> getActiveBorrowsByBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(borrowingService.getActiveBorrowsByBook(bookId));
    }

    /**
     * Manual trigger for the same overdue pass as the daily 01:00 scheduler (dev / QA).
     * Does not create fines; those apply on return via {@code PUT /{recordId}/return}.
     */
    @PostMapping("/maintenance/run-overdue-check")
    public ResponseEntity<RunOverdueCheckResult> runOverdueCheck() {
        int n = borrowingService.markOverdueRecords();
        return ResponseEntity.ok(new RunOverdueCheckResult(n));
    }

    /**
     * QA / admin testing: override {@code dueDate} for an ACTIVE or RENEWED borrow (persisted).
     * Combine with {@link #runOverdueCheck()} to test overdue status without waiting on the calendar.
     */
    @PutMapping("/maintenance/{recordId}/due-date")
    public ResponseEntity<BorrowRecordDTO> adminTestingSetDueDate(
            @PathVariable Long recordId,
            @Valid @RequestBody SetDueDateRequest body) {
        return ResponseEntity.ok(borrowingService.adminTestingSetDueDate(recordId, body.dueDate()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleBusinessRuleViolation(IllegalStateException ex) {
        return ResponseEntity.status(422).body(ex.getMessage());
    }
}
