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
import com.elibrary.borrowing_service.infrastructure.client.BookServiceClient.BookAvailability;
import com.elibrary.borrowing_service.infrastructure.client.UserServiceClient;
import com.elibrary.borrowing_service.infrastructure.client.UserServiceClient.UserValidation;
import com.elibrary.borrowing_service.infrastructure.messaging.EventPublisher;
import com.elibrary.borrowing_service.infrastructure.messaging.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Application-service integration test - verifies the full orchestration of
 * BorrowingService with mocked repositories, ACL clients, and event publisher.
 *
 * The tests here cover the happy path and key invariant violations for borrowBook,
 * returnBook, renewBook, and markOverdueRecords.
 */
@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {

    @Mock private BorrowRecordRepository borrowRecordRepository;
    @Mock private FineRepository fineRepository;
    @Mock private RenewalRecordRepository renewalRecordRepository;
    @Mock private FineCalculationService fineCalculationService;
    @Mock private BookServiceClient bookServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private BorrowingService borrowingService;

    @BeforeEach
    void setUp() {
        // @Value fields aren't injected by Mockito, so we poke the limit in manually
        ReflectionTestUtils.setField(borrowingService, "maxConcurrentBorrows", 5);
    }

    // builds a BorrowRecord with a fake id, since JPA would normally generate it
    private BorrowRecord savedRecord(Long recordId, Long userId, Long bookId) {
        BorrowRecord record = BorrowRecord.create(userId, bookId);
        ReflectionTestUtils.setField(record, "recordId", recordId);
        return record;
    }

    @Test
    void borrowBook_happyPath_createsRecordAndPublishesEvent() {
        when(userServiceClient.validateUser(1L))
            .thenReturn(new UserValidation(1L, true, true));
        when(borrowRecordRepository.countByUserIdAndStatusIn(eq(1L), anyList()))
            .thenReturn(0L);
        when(bookServiceClient.checkAvailability(100L))
            .thenReturn(new BookAvailability(100L, true, 3));
        // simulate what JPA does: assign an id when the record is persisted
        when(borrowRecordRepository.save(any(BorrowRecord.class)))
            .thenAnswer(inv -> {
                BorrowRecord r = inv.getArgument(0);
                ReflectionTestUtils.setField(r, "recordId", 42L);
                return r;
            });

        BorrowRecordDTO dto = borrowingService.borrowBook(1L, 100L);

        assertEquals(42L, dto.getRecordId());
        assertEquals(BorrowStatus.ACTIVE, dto.getStatus());
        // make sure the book's copy count was decremented and the event fired
        verify(bookServiceClient).decrementCopies(100L);
        verify(eventPublisher).publishBookBorrowed(any(BookBorrowedEvent.class));
    }

    @Test
    void borrowBook_throwsWhenUserDoesNotExist() {
        when(userServiceClient.validateUser(99L))
            .thenReturn(new UserValidation(99L, false, false));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> borrowingService.borrowBook(99L, 100L));
        assertTrue(ex.getMessage().contains("does not exist"));
        // shouldn't have touched the db at all if the user doesn't exist
        verifyNoInteractions(borrowRecordRepository);
    }

    @Test
    void borrowBook_throwsWhenUserInactive() {
        when(userServiceClient.validateUser(1L))
            .thenReturn(new UserValidation(1L, true, false));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> borrowingService.borrowBook(1L, 100L));
        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    void borrowBook_throwsWhenMaxBorrowsReached_INV_B9() {
        when(userServiceClient.validateUser(1L))
            .thenReturn(new UserValidation(1L, true, true));
        when(borrowRecordRepository.countByUserIdAndStatusIn(eq(1L), anyList()))
            .thenReturn(5L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> borrowingService.borrowBook(1L, 100L));
        assertTrue(ex.getMessage().contains("maximum"));
        // borrow limit hit early, so we should never even ask if the book is available
        verify(bookServiceClient, never()).checkAvailability(anyLong());
    }

    @Test
    void borrowBook_throwsWhenBookUnavailable_INV_B1() {
        when(userServiceClient.validateUser(1L))
            .thenReturn(new UserValidation(1L, true, true));
        when(borrowRecordRepository.countByUserIdAndStatusIn(eq(1L), anyList()))
            .thenReturn(0L);
        when(bookServiceClient.checkAvailability(100L))
            .thenReturn(new BookAvailability(100L, false, 0));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> borrowingService.borrowBook(1L, 100L));
        assertTrue(ex.getMessage().contains("no available copies"));
    }

    @Test
    void returnBook_onTimeReturn_noFine() {
        BorrowRecord record = savedRecord(42L, 1L, 100L);
        when(borrowRecordRepository.findById(42L)).thenReturn(Optional.of(record));
        when(fineCalculationService.applyFine(any())).thenReturn(Optional.empty());
        when(borrowRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BorrowRecordDTO dto = borrowingService.returnBook(42L);

        assertEquals(BorrowStatus.RETURNED, dto.getStatus());
        assertNull(dto.getFine());
        verify(bookServiceClient).incrementCopies(100L);
        verify(eventPublisher).publishBookReturned(any(BookReturnedEvent.class));
        // on-time return means no fine event should have been published
        verify(eventPublisher, never()).publishFineApplied(any());
    }

    @Test
    void returnBook_lateReturn_appliesFineAndPublishesBothEvents() {
        BorrowRecord record = savedRecord(42L, 1L, 100L);
        when(borrowRecordRepository.findById(42L)).thenReturn(Optional.of(record));

        // 2 chargeable days overdue at €0.50/day = €1.00
        Fine fine = Fine.create(42L, 1L, 2);
        ReflectionTestUtils.setField(fine, "fineId", 7L);
        when(fineCalculationService.applyFine(any())).thenReturn(Optional.of(fine));
        when(borrowRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BorrowRecordDTO dto = borrowingService.returnBook(42L);

        assertNotNull(dto.getFine());
        assertEquals(7L, dto.getFine().fineId());
        assertEquals(1.0, dto.getFine().amount(), 0.001);
        // late return should fire both the return event and the fine event
        verify(eventPublisher).publishBookReturned(any(BookReturnedEvent.class));
        verify(eventPublisher).publishFineApplied(any(FineAppliedEvent.class));
    }

    @Test
    void returnBook_throwsWhenRecordNotFound() {
        when(borrowRecordRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> borrowingService.returnBook(999L));
    }

    @Test
    void renewBook_happyPath_extendsAndPublishes() {
        BorrowRecord record = savedRecord(42L, 1L, 100L);
        when(borrowRecordRepository.findById(42L)).thenReturn(Optional.of(record));
        when(borrowRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(renewalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BorrowRecordDTO dto = borrowingService.renewBook(42L);

        assertEquals(BorrowStatus.RENEWED, dto.getStatus());
        assertEquals(1, dto.getRenewCount());
        assertEquals(LocalDate.now().plusDays(BorrowRecord.LOAN_PERIOD_DAYS), dto.getDueDate());
        verify(eventPublisher).publishBookRenewed(any(BookRenewedEvent.class));
    }

    // uses an ArgumentCaptor to grab the RenewalRecord that was persisted
    // and verify it holds the right before/after due dates
    @Test
    void renewBook_createsRenewalRecord() {
        BorrowRecord record = savedRecord(42L, 1L, 100L);
        when(borrowRecordRepository.findById(42L)).thenReturn(Optional.of(record));
        when(borrowRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(renewalRecordRepository.save(any(RenewalRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        borrowingService.renewBook(42L);

        ArgumentCaptor<RenewalRecord> captor = ArgumentCaptor.forClass(RenewalRecord.class);
        verify(renewalRecordRepository).save(captor.capture());
        RenewalRecord saved = captor.getValue();
        assertEquals(42L, saved.getRecordId());
        assertEquals(LocalDate.now().plusDays(BorrowRecord.LOAN_PERIOD_DAYS), saved.getNewDueDate());
    }

    // simulates the daily scheduled job finding two overdue records
    @Test
    void markOverdueRecords_transitionsAndPublishesEvents() {
        BorrowRecord r1 = savedRecord(10L, 1L, 100L);
        BorrowRecord r2 = savedRecord(11L, 2L, 200L);
        when(borrowRecordRepository.findOverdueRecords(LocalDate.now()))
            .thenReturn(List.of(r1, r2));
        when(borrowRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        borrowingService.markOverdueRecords();

        // both records should have transitioned to OVERDUE
        assertEquals(BorrowStatus.OVERDUE, r1.getStatus());
        assertEquals(BorrowStatus.OVERDUE, r2.getStatus());
        // one save + one event per record
        verify(borrowRecordRepository, times(2)).save(any());
        verify(eventPublisher, times(2)).publishBookOverdue(any(BookOverdueEvent.class));
    }

    @Test
    void markOverdueRecords_doesNothingWhenNoCandidates() {
        when(borrowRecordRepository.findOverdueRecords(LocalDate.now()))
            .thenReturn(List.of());

        borrowingService.markOverdueRecords();

        verify(borrowRecordRepository, never()).save(any());
        verify(eventPublisher, never()).publishBookOverdue(any());
    }

    // checks that the query path attaches the unpaid fine to the DTO
    @Test
    void getBorrowRecord_returnsDtoWithUnpaidFine() {
        BorrowRecord record = savedRecord(42L, 1L, 100L);
        // 3 chargeable days at €0.50/day = €1.50
        Fine fine = Fine.create(42L, 1L, 3);
        ReflectionTestUtils.setField(fine, "fineId", 7L);

        when(borrowRecordRepository.findById(42L)).thenReturn(Optional.of(record));
        when(fineRepository.findByRecordIdAndIsPaidFalse(42L)).thenReturn(Optional.of(fine));

        BorrowRecordDTO dto = borrowingService.getBorrowRecord(42L);

        assertNotNull(dto.getFine());
        assertEquals(1.50, dto.getFine().amount(), 0.001);
        assertFalse(dto.getFine().isPaid());
    }

    @Test
    void getActiveBorrowsByBook_delegatesToRepository() {
        BorrowRecord record = savedRecord(42L, 1L, 100L);
        when(borrowRecordRepository.findByBookIdAndStatus(100L, BorrowStatus.ACTIVE))
            .thenReturn(List.of(record));

        List<BorrowRecordDTO> result = borrowingService.getActiveBorrowsByBook(100L);

        assertEquals(1, result.size());
        assertEquals(42L, result.getFirst().getRecordId());
    }
}
