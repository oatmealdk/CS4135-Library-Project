package com.elibrary.borrowing_service.domain.repository;

import com.elibrary.borrowing_service.domain.model.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByUserId(Long userId);

    List<Fine> findByRecordId(Long recordId);

    /** Used to enforce INV-B7: only one unpaid fine per borrow record at a time. */
    Optional<Fine> findByRecordIdAndIsPaidFalse(Long recordId);
}
