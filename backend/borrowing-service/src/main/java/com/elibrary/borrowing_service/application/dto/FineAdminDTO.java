package com.elibrary.borrowing_service.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Admin fines list: human-readable patron and book fields (IDs omitted from JSON).
 */
public record FineAdminDTO(
    Long fineId,
    double amount,
    int daysOverdue,
    double dailyRate,
    LocalDateTime issuedAt,
    LocalDateTime paidAt,
    @JsonProperty("paid") boolean paid,
    String patronName,
    String patronEmail,
    String bookTitle,
    String bookAuthor,
    String bookIsbn
) {}
