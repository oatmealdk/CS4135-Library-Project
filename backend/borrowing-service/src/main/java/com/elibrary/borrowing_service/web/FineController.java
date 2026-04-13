package com.elibrary.borrowing_service.web;

import com.elibrary.borrowing_service.application.BorrowingService;
import com.elibrary.borrowing_service.application.dto.FineAdminDTO;
import com.elibrary.borrowing_service.application.dto.FineDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin / desk API for viewing and settling fines.
 */
@RestController
@RequestMapping("/api/fines")
public class FineController {

    private final BorrowingService borrowingService;

    public FineController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    @GetMapping
    public ResponseEntity<List<FineAdminDTO>> listFines() {
        return ResponseEntity.ok(borrowingService.listAllFinesForAdmin());
    }

    @PutMapping("/{fineId}/pay")
    public ResponseEntity<FineDTO> markPaid(@PathVariable Long fineId) {
        return ResponseEntity.ok(borrowingService.markFinePaid(fineId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleAlreadyPaid(IllegalStateException ex) {
        return ResponseEntity.status(422).body(ex.getMessage());
    }
}
