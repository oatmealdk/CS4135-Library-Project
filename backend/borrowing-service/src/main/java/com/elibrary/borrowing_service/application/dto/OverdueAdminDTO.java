package com.elibrary.borrowing_service.application.dto;

import com.elibrary.borrowing_service.domain.model.BorrowStatus;

import java.time.LocalDate;

/**
 * Admin overdue view: patron and book labels, no internal IDs.
 */
public record OverdueAdminDTO(
    String patronName,
    String patronEmail,
    String bookTitle,
    String bookAuthor,
    String bookIsbn,
    LocalDate borrowDate,
    LocalDate dueDate,
    int renewCount,
    BorrowStatus status
) {}
