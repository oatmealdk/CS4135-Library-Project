package com.elibrary.borrowing_service.application.dto;

import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import com.elibrary.borrowing_service.domain.model.BorrowStatus;
import com.elibrary.borrowing_service.domain.model.Fine;

import java.time.LocalDate;

/**
 * DTO for all BorrowRecord responses.
 *
 * fine is null unless the return triggered a fine (INV-B2).
 */
public class BorrowRecordDTO {

    private Long recordId;
    private Long userId;
    private Long bookId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private int renewCount;
    private BorrowStatus status;
    private FineDTO fine;

    public record FineDTO(Long fineId, double amount, int daysOverdue, boolean isPaid) {}

    public static BorrowRecordDTO from(BorrowRecord record) {
        return from(record, null);
    }

    public static BorrowRecordDTO from(BorrowRecord record, Fine fine) {
        BorrowRecordDTO dto = new BorrowRecordDTO();
        dto.recordId = record.getRecordId();
        dto.userId = record.getUserId();
        dto.bookId = record.getBookId();
        dto.borrowDate = record.getBorrowDate();
        dto.dueDate = record.getDueDate();
        dto.returnDate = record.getReturnDate();
        dto.renewCount = record.getRenewCount();
        dto.status = record.getStatus();
        if (fine != null) {
            dto.fine = new FineDTO(fine.getFineId(), fine.getAmount(),
                                   fine.getDaysOverdue(), fine.isPaid());
        }
        return dto;
    }


    public Long getRecordId()      { return recordId; }
    public Long getUserId()        { return userId; }
    public Long getBookId()        { return bookId; }
    public LocalDate getBorrowDate(){ return borrowDate; }
    public LocalDate getDueDate()  { return dueDate; }
    public LocalDate getReturnDate(){ return returnDate; }
    public int getRenewCount()     { return renewCount; }
    public BorrowStatus getStatus(){ return status; }
    public FineDTO getFine()       { return fine; }
}
