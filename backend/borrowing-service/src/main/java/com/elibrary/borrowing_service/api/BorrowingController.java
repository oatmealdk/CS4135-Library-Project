package com.elibrary.borrowing_service.api;

import com.elibrary.borrowing_service.application.BorrowingService;
import com.elibrary.borrowing_service.application.dto.BorrowRecordDTO;
import com.elibrary.borrowing_service.application.dto.BorrowRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrows")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
public class BorrowingController {

    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    @PostMapping
    public BorrowRecordDTO borrow(@Valid @RequestBody BorrowRequest request) {
        return borrowingService.borrowBook(request.getUserId(), request.getBookId());
    }

    @PutMapping("/{recordId}/return")
    public BorrowRecordDTO returnBook(@PathVariable Long recordId) {
        return borrowingService.returnBook(recordId);
    }

    @PutMapping("/{recordId}/renew")
    public BorrowRecordDTO renew(@PathVariable Long recordId) {
        return borrowingService.renewBook(recordId);
    }

    @GetMapping("/user/{userId}")
    public List<BorrowRecordDTO> listForUser(@PathVariable Long userId) {
        return borrowingService.getBorrowsByUser(userId);
    }

    @GetMapping("/{recordId}")
    public BorrowRecordDTO getOne(@PathVariable Long recordId) {
        return borrowingService.getBorrowRecord(recordId);
    }

    @GetMapping("/book/{bookId}/active")
    public List<BorrowRecordDTO> activeForBook(@PathVariable Long bookId) {
        return borrowingService.getActiveBorrowsByBook(bookId);
    }
}
