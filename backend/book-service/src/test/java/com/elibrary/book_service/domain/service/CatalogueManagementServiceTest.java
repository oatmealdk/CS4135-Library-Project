package com.elibrary.book_service.domain.service;

import com.elibrary.book_service.domain.model.Book;
import com.elibrary.book_service.domain.model.BookStatus;
import com.elibrary.book_service.domain.model.Category;
import com.elibrary.book_service.domain.repository.BookRepository;
import com.elibrary.book_service.domain.repository.CategoryRepository;
import com.elibrary.book_service.dto.BookDTO;
import com.elibrary.book_service.dto.CategoryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogueManagementServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EntityManager entityManager;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private CatalogueManagementService service;

    private BookDTO validBookRequest() {
        BookDTO dto = new BookDTO();
        dto.setIsbn("978-0134685991");
        dto.setTitle("Effective Java");
        dto.setAuthor("Joshua Bloch");
        dto.setPublisher("Addison-Wesley");
        dto.setPublishYear(2018);
        dto.setDescription("Best practices for Java");
        dto.setTotalCopies(3);
        return dto;
    }

    private Book createBook(Long id, String isbn, int totalCopies, int availableCopies, BookStatus status) {
        Book book = new Book();
        try {
            var field = Book.class.getDeclaredField("bookId");
            field.setAccessible(true);
            field.set(book, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        book.setIsbn(isbn);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setPublisher("Test Publisher");
        book.setPublishYear(2024);
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        book.setStatus(status);
        return book;
    }

    // ========================================================
    // INV-C3: ISBN must be unique across all books
    // ========================================================
    @Nested
    @DisplayName("INV-C3: ISBN Uniqueness")
    class IsbnUniqueness {

        @Test
        @DisplayName("Should reject book with duplicate ISBN")
        void addBook_duplicateIsbn_throwsException() {
            BookDTO request = validBookRequest();
            when(bookRepository.existsByIsbn("978-0134685991")).thenReturn(true);

            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.addBook(request)
            );
            assertTrue(ex.getMessage().contains("ISBN already exists"));
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow book with unique ISBN")
        void addBook_uniqueIsbn_succeeds() {
            BookDTO request = validBookRequest();
            Book savedBook = createBook(1L, "978-0134685991", 3, 3, BookStatus.AVAILABLE);

            when(bookRepository.existsByIsbn("978-0134685991")).thenReturn(false);
            when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

            TypedQuery<Long> mockQuery = mock(TypedQuery.class);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());
            when(mockQuery.executeUpdate()).thenReturn(0);
            when(entityManager.createQuery(anyString())).thenReturn(mockQuery);

            BookDTO result = service.addBook(request);

            assertNotNull(result);
            assertEquals("978-0134685991", result.getIsbn());
            verify(bookRepository).save(any(Book.class));
        }
    }

    // ========================================================
    // INV-C4: totalCopies >= 1 when book is added
    // ========================================================
    @Nested
    @DisplayName("INV-C4: Total Copies Minimum")
    class TotalCopiesMinimum {

        @Test
        @DisplayName("Should reject book with totalCopies = 0")
        void addBook_zeroCopies_throwsException() {
            BookDTO request = validBookRequest();
            request.setTotalCopies(0);

            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.addBook(request)
            );
            assertTrue(ex.getMessage().contains("totalCopies must be >= 1"));
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject book with negative totalCopies")
        void addBook_negativeCopies_throwsException() {
            BookDTO request = validBookRequest();
            request.setTotalCopies(-1);

            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.addBook(request)
            );
            assertTrue(ex.getMessage().contains("totalCopies must be >= 1"));
        }

        @Test
        @DisplayName("Should accept book with totalCopies = 1")
        void addBook_oneCopy_succeeds() {
            BookDTO request = validBookRequest();
            request.setTotalCopies(1);
            Book savedBook = createBook(1L, "978-0134685991", 1, 1, BookStatus.AVAILABLE);

            when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
            when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

            TypedQuery<Long> mockQuery = mock(TypedQuery.class);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());
            when(mockQuery.executeUpdate()).thenReturn(0);
            when(entityManager.createQuery(anyString())).thenReturn(mockQuery);

            BookDTO result = service.addBook(request);
            assertEquals(1, result.getTotalCopies());
        }
    }

    // ========================================================
    // INV-C1: 0 <= availableCopies <= totalCopies
    // ========================================================
    @Nested
    @DisplayName("INV-C1: Available Copies Bounds")
    class AvailableCopiesBounds {

        @Test
        @DisplayName("availableCopies should equal totalCopies on creation")
        void addBook_setsAvailableCopiesToTotal() {
            BookDTO request = validBookRequest();
            request.setTotalCopies(5);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            Book savedBook = createBook(1L, "978-0134685991", 5, 5, BookStatus.AVAILABLE);

            when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
            when(bookRepository.save(captor.capture())).thenReturn(savedBook);

            TypedQuery<Long> mockQuery = mock(TypedQuery.class);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());
            when(mockQuery.executeUpdate()).thenReturn(0);
            when(entityManager.createQuery(anyString())).thenReturn(mockQuery);

            service.addBook(request);

            Book captured = captor.getValue();
            assertEquals(5, captured.getAvailableCopies());
            assertEquals(5, captured.getTotalCopies());
        }

        @Test
        @DisplayName("Cannot decrement below zero")
        void decrementCopies_atZero_throwsException() {
            Book book = createBook(1L, "978-0134685991", 3, 0, BookStatus.BORROWED);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.decrementCopies(1L)
            );
            assertTrue(ex.getMessage().contains("No available copies"));
        }

        @Test
        @DisplayName("Cannot increment above totalCopies")
        void incrementCopies_atMax_throwsException() {
            Book book = createBook(1L, "978-0134685991", 3, 3, BookStatus.AVAILABLE);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.incrementCopies(1L)
            );
            assertTrue(ex.getMessage().contains("Cannot exceed total copies"));
        }
    }

    // ========================================================
    // INV-C5: Status derived from availability
    // ========================================================
    @Nested
    @DisplayName("INV-C5: Status Auto-Derivation")
    class StatusDerivation {

        @Test
        @DisplayName("Status becomes BORROWED when availableCopies reaches 0")
        void decrementCopies_toZero_statusBorrowed() {
            Book book = createBook(1L, "978-0134685991", 3, 1, BookStatus.AVAILABLE);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            service.decrementCopies(1L);

            assertEquals(BookStatus.BORROWED, book.getStatus());
            assertEquals(0, book.getAvailableCopies());
        }

        @Test
        @DisplayName("Status becomes AVAILABLE when copies freed from 0")
        void incrementCopies_fromZero_statusAvailable() {
            Book book = createBook(1L, "978-0134685991", 3, 0, BookStatus.BORROWED);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            service.incrementCopies(1L);

            assertEquals(BookStatus.AVAILABLE, book.getStatus());
            assertEquals(1, book.getAvailableCopies());
        }
    }

    // ========================================================
    // INV-C2: Cannot remove book while active borrows exist
    // ========================================================
    @Nested
    @DisplayName("INV-C2: Remove Book with Active Borrows")
    class RemoveBookActiveBorrows {

        @Test
        @DisplayName("Should reject removal when active borrows exist")
        void removeFromCatalogue_activeBorrows_throwsException() {
            Book book = createBook(1L, "978-0134685991", 3, 3, BookStatus.AVAILABLE);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            when(restTemplate.getForObject(
                eq("http://borrowing-service/api/borrows/book/1/active"),
                eq(Object[].class)
            )).thenReturn(new Object[]{new Object()});

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.removeFromCatalogue(1L)
            );
            assertTrue(ex.getMessage().contains("Cannot remove book"));
            verify(bookRepository, never()).save(argThat(b -> b.getStatus() == BookStatus.REMOVED));
        }
    }

    // ========================================================
    // Soft Delete (Event Storming Hotspot 4)
    // ========================================================
    @Nested
    @DisplayName("Soft Delete Behaviour")
    class SoftDelete {

        @Test
        @DisplayName("removeFromCatalogue sets status to REMOVED (soft delete)")
        void removeFromCatalogue_noActiveBorrows_softDeletes() {
            Book book = createBook(1L, "978-0134685991", 3, 3, BookStatus.AVAILABLE);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(restTemplate.getForObject(anyString(), eq(Object[].class))).thenReturn(new Object[0]);
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            service.removeFromCatalogue(1L);

            assertEquals(BookStatus.REMOVED, book.getStatus());
            verify(bookRepository).save(book);
            verify(rabbitTemplate).convertAndSend(
                eq("book.events"),
                eq("book.removed"),
                any(Object.class)
            );
        }

        @Test
        @DisplayName("Cannot decrement copies on a removed book")
        void decrementCopies_removedBook_throwsException() {
            Book book = createBook(1L, "978-0134685991", 3, 3, BookStatus.REMOVED);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.decrementCopies(1L)
            );
            assertTrue(ex.getMessage().contains("Removed books"));
        }

        @Test
        @DisplayName("Cannot update a removed book")
        void updateBook_removedBook_throwsException() {
            Book book = createBook(1L, "978-0134685991", 3, 3, BookStatus.REMOVED);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            BookDTO request = validBookRequest();

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.updateBook(1L, request)
            );
            assertTrue(ex.getMessage().contains("Cannot update a removed book"));
        }
    }

    // ========================================================
    // INV-C6: Category cannot be deleted if assigned to books
    // ========================================================
    @Nested
    @DisplayName("INV-C6: Category Deletion")
    class CategoryDeletion {

        @Test
        @DisplayName("Should reject category deletion when books are assigned")
        void deleteCategory_withBooks_throwsException() {
            Category category = new Category();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            TypedQuery<Long> mockQuery = mock(TypedQuery.class);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getSingleResult()).thenReturn(3L);

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.deleteCategory(1L)
            );
            assertTrue(ex.getMessage().contains("Category cannot be deleted"));
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should allow category deletion when no books are assigned")
        void deleteCategory_noBooks_succeeds() {
            Category category = new Category();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            TypedQuery<Long> mockQuery = mock(TypedQuery.class);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getSingleResult()).thenReturn(0L);

            service.deleteCategory(1L);

            verify(categoryRepository).delete(category);
        }
    }

    // ========================================================
    // Domain Events Published
    // ========================================================
    @Nested
    @DisplayName("Domain Events")
    class DomainEvents {

        @Test
        @DisplayName("BookAddedEvent published when book is created")
        void addBook_publishesBookAddedEvent() {
            BookDTO request = validBookRequest();
            Book savedBook = createBook(1L, "978-0134685991", 3, 3, BookStatus.AVAILABLE);

            when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
            when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

            TypedQuery<Long> mockQuery = mock(TypedQuery.class);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());
            when(mockQuery.executeUpdate()).thenReturn(0);
            when(entityManager.createQuery(anyString())).thenReturn(mockQuery);

            service.addBook(request);

            verify(rabbitTemplate).convertAndSend(
                eq("book.events"),
                eq("book.added"),
                any(Object.class)
            );
        }

        @Test
        @DisplayName("BookAvailabilityChangedEvent published when copies change")
        void decrementCopies_publishesAvailabilityEvent() {
            Book book = createBook(1L, "978-0134685991", 3, 2, BookStatus.AVAILABLE);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            service.decrementCopies(1L);

            verify(rabbitTemplate).convertAndSend(
                eq("book.events"),
                eq("book.availability.changed"),
                any(Object.class)
            );
        }
    }

    // ========================================================
    // Book Not Found
    // ========================================================
    @Nested
    @DisplayName("Book Not Found")
    class BookNotFound {

        @Test
        @DisplayName("Should throw when book doesn't exist")
        void getBook_notFound_throwsException() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(
                NoSuchElementException.class,
                () -> service.getBook(999L)
            );
        }
    }
}