package com.elibrary.borrowing_service.domain.repository;

import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import com.elibrary.borrowing_service.domain.model.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByUserId(Long userId);

    List<BorrowRecord> findByBookIdAndStatus(Long bookId, BorrowStatus status);

    List<BorrowRecord> findByStatus(BorrowStatus status);

    long countByUserIdAndStatusIn(Long userId, List<BorrowStatus> statuses);
}
