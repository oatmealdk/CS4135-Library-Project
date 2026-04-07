package com.elibrary.borrowing_service.domain.repository;

import com.elibrary.borrowing_service.domain.model.RenewalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RenewalRecordRepository extends JpaRepository<RenewalRecord, Long> {

    List<RenewalRecord> findByRecordId(Long recordId);
}
