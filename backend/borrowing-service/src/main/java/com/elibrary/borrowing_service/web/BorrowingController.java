package com.elibrary.borrowing_service.web;

import com.elibrary.borrowing_service.application.BorrowingService;
import com.elibrary.borrowing_service.application.dto.BorrowRecordDTO;
import com.elibrary.borrowing_service.application.dto.BorrowRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Borrowing context's published language in DDD section 1.
 *
 * Endpoints implemented to match our API contracts defined in the first DDD stage:
 *   POST   /api/borrows                       -> borrow a book
 *   PUT    /api/borrows/{recordId}/return     -> return a book
 *   PUT    /api/borrows/{recordId}/renew      -> renew a book
 *   GET    /api/borrows/user/{userId}         -> get all borrows for a user
 *   GET    /api/borrows/{recordId}            -> get a single borrow record
 *   GET    /api/borrows/book/{bookId}/active  -> active borrows for a book (used by book-service, INV-C2)
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleBusinessRuleViolation(IllegalStateException ex) {
        return ResponseEntity.status(422).body(ex.getMessage());
    }
}
