package com.elibrary.borrowing_service.application.dto;

import com.elibrary.borrowing_service.domain.model.Fine;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Read model for admin fine list / payment confirmation.
 */
public record FineDTO(
    Long fineId,
    Long recordId,
    Long userId,
    double amount,
    int daysOverdue,
    double dailyRate,
    LocalDateTime issuedAt,
    LocalDateTime paidAt,
    @JsonProperty("paid") boolean paid
) {
    public static FineDTO from(Fine f) {
        return new FineDTO(
            f.getFineId(),
            f.getRecordId(),
            f.getUserId(),
            f.getAmount(),
            f.getDaysOverdue(),
            f.getDailyRate(),
            f.getIssuedAt(),
            f.getPaidAt(),
            f.isPaid()
        );
    }
}
