package com.elibrary.borrowing_service.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Body for {@code PUT /api/borrows/maintenance/{recordId}/due-date} (QA / admin testing only).
 */
public record SetDueDateRequest(@NotNull LocalDate dueDate) {}
