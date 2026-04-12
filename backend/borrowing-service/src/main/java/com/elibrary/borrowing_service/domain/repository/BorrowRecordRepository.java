package com.elibrary.borrowing_service.domain.repository;

import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import com.elibrary.borrowing_service.domain.model.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByUserId(Long userId);

    boolean existsByUserIdAndBookIdAndStatusIn(Long userId, Long bookId, Collection<BorrowStatus> statuses);

    List<BorrowRecord> findByBookIdAndStatus(Long bookId, BorrowStatus status);

    List<BorrowRecord> findByBookIdAndStatusIn(Long bookId, Collection<BorrowStatus> statuses);

    List<BorrowRecord> findByStatus(BorrowStatus status);

    @Query("SELECT r FROM BorrowRecord r WHERE r.status IN ('ACTIVE', 'RENEWED') AND r.dueDate < :today")
    List<BorrowRecord> findOverdueRecords(LocalDate today);

    long countByUserIdAndStatusIn(Long userId, List<BorrowStatus> statuses);
}
