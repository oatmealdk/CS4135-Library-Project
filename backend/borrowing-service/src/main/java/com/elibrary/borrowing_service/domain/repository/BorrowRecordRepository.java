package com.elibrary.borrowing_service.domain.repository;

import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import com.elibrary.borrowing_service.domain.model.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByUserId(Long userId);

    List<BorrowRecord> findByBookIdAndStatus(Long bookId, BorrowStatus status);

    List<BorrowRecord> findByStatus(BorrowStatus status);

    @Query("SELECT r FROM BorrowRecord r WHERE r.status IN ('ACTIVE', 'RENEWED') AND r.dueDate < :today")
    List<BorrowRecord> findOverdueRecords(LocalDate today);

    long countByUserIdAndStatusIn(Long userId, List<BorrowStatus> statuses);
}
