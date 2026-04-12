package com.elibrary.borrowing_service.application;

import com.elibrary.borrowing_service.application.dto.BorrowRecordDTO;
import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import com.elibrary.borrowing_service.domain.model.BorrowStatus;
import com.elibrary.borrowing_service.domain.model.Fine;
import com.elibrary.borrowing_service.domain.model.RenewalRecord;
import com.elibrary.borrowing_service.domain.repository.BorrowRecordRepository;
import com.elibrary.borrowing_service.domain.repository.FineRepository;
import com.elibrary.borrowing_service.domain.repository.RenewalRecordRepository;
import com.elibrary.borrowing_service.domain.service.FineCalculationService;
import com.elibrary.borrowing_service.infrastructure.client.BookServiceClient;
import com.elibrary.borrowing_service.infrastructure.client.UserServiceClient;
import com.elibrary.borrowing_service.infrastructure.messaging.EventPublisher;
import com.elibrary.borrowing_service.infrastructure.messaging.event.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BorrowingService {

    /** important values obtained from config-server: borrowing.max-concurrent-borrows (we chose to default to 5). */
    @Value("${borrowing.max-concurrent-borrows:5}")
    private int maxConcurrentBorrows;

    private final BorrowRecordRepository borrowRecordRepository;
    private final FineRepository fineRepository;
    private final RenewalRecordRepository renewalRecordRepository;
    private final FineCalculationService fineCalculationService;
    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;
    private final EventPublisher eventPublisher;

    public BorrowingService(BorrowRecordRepository borrowRecordRepository,
                            FineRepository fineRepository,
                            RenewalRecordRepository renewalRecordRepository,
                            FineCalculationService fineCalculationService,
                            BookServiceClient bookServiceClient,
                            UserServiceClient userServiceClient,
                            EventPublisher eventPublisher) {
        this.borrowRecordRepository  = borrowRecordRepository;
        this.fineRepository = fineRepository;
        this.renewalRecordRepository = renewalRecordRepository;
        this.fineCalculationService = fineCalculationService;
        this.bookServiceClient = bookServiceClient;
        this.userServiceClient = userServiceClient;
        this.eventPublisher = eventPublisher;
    }

    public BorrowRecordDTO borrowBook(Long userId, Long bookId) {
        // validate user exists and is active (ACL on UserServiceClient)
        UserServiceClient.UserValidation user = userServiceClient.validateUser(userId);
        if (!user.exists() || !user.isActive()) {
            throw new IllegalArgumentException("User " + userId + " does not exist or is not active.");
        }

        // check concurrent borrow limit, satisfies our new invariant INV-B9
        long activeBorrows = borrowRecordRepository.countByUserIdAndStatusIn(
            userId, List.of(BorrowStatus.ACTIVE, BorrowStatus.RENEWED, BorrowStatus.OVERDUE));
        if (activeBorrows >= maxConcurrentBorrows) {
            throw new IllegalStateException(
                "User has reached the maximum of " + maxConcurrentBorrows + " concurrent borrows.");
        }

        // one active loan per book per user (second copy of same title not allowed)
        if (borrowRecordRepository.existsByUserIdAndBookIdAndStatusIn(userId, bookId,
                List.of(BorrowStatus.ACTIVE, BorrowStatus.RENEWED, BorrowStatus.OVERDUE))) {
            throw new IllegalStateException("You already have a copy of this book on loan.");
        }

        // check book availability before borrow can happen (ACL on BookServiceClient)
        BookServiceClient.BookAvailability availability = bookServiceClient.checkAvailability(bookId);
        if (!availability.available()) {
            throw new IllegalStateException("Book " + bookId + " has no available copies.");
        }

        BorrowRecord record = BorrowRecord.create(userId, bookId);
        borrowRecordRepository.save(record);

        bookServiceClient.decrementCopies(bookId);

        eventPublisher.publishBookBorrowed(new BookBorrowedEvent(
            record.getRecordId(), userId, bookId, record.getDueDate(), LocalDateTime.now()));

        return BorrowRecordDTO.from(record);
    }

    public BorrowRecordDTO returnBook(Long recordId) {
        BorrowRecord record = findRecord(recordId);
        boolean wasOverdue  = record.getStatus() == BorrowStatus.OVERDUE;

        record.returnBook();
        borrowRecordRepository.save(record);

        // calculate fine if applicable (enforcing INV-B2 here, grace period from config server)
        Fine fine = fineCalculationService.applyFine(record).orElse(null);

        bookServiceClient.incrementCopies(record.getBookId());

        eventPublisher.publishBookReturned(new BookReturnedEvent(
            recordId, record.getUserId(), record.getBookId(),
            record.getReturnDate(), wasOverdue, LocalDateTime.now()));

        if (fine != null) {
            eventPublisher.publishFineApplied(new FineAppliedEvent(
                fine.getFineId(), recordId, record.getUserId(),
                fine.getAmount(), fine.getDaysOverdue(), LocalDateTime.now()));
        }

        return BorrowRecordDTO.from(record, fine);
    }

    public BorrowRecordDTO renewBook(Long recordId) {
        BorrowRecord record = findRecord(recordId);
        LocalDate previousDueDate = record.getDueDate();

        record.renewBook();
        borrowRecordRepository.save(record);

        RenewalRecord renewal = RenewalRecord.create(recordId, previousDueDate, record.getDueDate());
        renewalRecordRepository.save(renewal);

        eventPublisher.publishBookRenewed(new BookRenewedEvent(
            recordId, record.getUserId(), record.getBookId(),
            record.getDueDate(), record.getRenewCount(), LocalDateTime.now()));

        return BorrowRecordDTO.from(record);
    }

    // query methods
    @Transactional(readOnly = true)
    public List<BorrowRecordDTO> getBorrowsByUser(Long userId) {
        return borrowRecordRepository.findByUserId(userId)
            .stream()
            .map(r -> BorrowRecordDTO.from(r, getUnpaidFine(r)))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BorrowRecordDTO getBorrowRecord(Long recordId) {
        BorrowRecord record = findRecord(recordId);
        return BorrowRecordDTO.from(record, getUnpaidFine(record));
    }

    @Transactional(readOnly = true)
    public List<BorrowRecordDTO> getActiveBorrowsByBook(Long bookId) {
        return borrowRecordRepository
            .findByBookIdAndStatusIn(bookId,
                List.of(BorrowStatus.ACTIVE, BorrowStatus.RENEWED, BorrowStatus.OVERDUE))
            .stream()
            .map(BorrowRecordDTO::from)
            .collect(Collectors.toList());
    }

    /**
     * Marks ACTIVE/RENEWED borrows as OVERDUE when {@code dueDate} is before today.
     * Used by {@link com.elibrary.borrowing_service.web.OverdueScheduler} and the manual maintenance endpoint.
     *
     * @return number of records transitioned to OVERDUE
     */
    public int markOverdueRecords() {
        List<BorrowRecord> candidates = borrowRecordRepository.findOverdueRecords(LocalDate.now());

        candidates.forEach(record -> {
            record.markOverdue();
            borrowRecordRepository.save(record);
            eventPublisher.publishBookOverdue(new BookOverdueEvent(
                record.getRecordId(), record.getUserId(), record.getBookId(),
                record.getDueDate(), record.getDaysOverdue(), LocalDateTime.now()));
        });
        return candidates.size();
    }

    // putting som ehelper methodss here that use the repositories to get the data we need

    private BorrowRecord findRecord(Long recordId) {
        return borrowRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("BorrowRecord not found: " + recordId));
    }

    private Fine getUnpaidFine(BorrowRecord record) {
        return fineRepository.findByRecordIdAndIsPaidFalse(record.getRecordId()).orElse(null);
    }
}
