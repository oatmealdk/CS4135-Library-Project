package com.elibrary.borrowing_service.application.dto;

/**
 * Response for the manual overdue job trigger (same logic as {@code OverdueScheduler} at 01:00).
 */
public record RunOverdueCheckResult(int recordsMarkedOverdue) {}
