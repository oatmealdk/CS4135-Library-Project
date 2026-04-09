package com.elibrary.book_service.domain.service;

import com.elibrary.book_service.domain.model.*;
import com.elibrary.book_service.domain.repository.BookRepository;
import com.elibrary.book_service.domain.repository.CategoryRepository;
import com.elibrary.book_service.dto.BookDTO;
import com.elibrary.book_service.dto.CategoryDTO;
import com.elibrary.book_service.dto.PagedResult;
import com.elibrary.book_service.infrastructure.messaging.event.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CatalogueManagementService {

    private static final String BOOK_EVENTS_EXCHANGE = "book.events";
    private static final String BORROWING_SERVICE_BASE_URL = "http://borrowing-service";

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    public CatalogueManagementService(
        BookRepository bookRepository,
        CategoryRepository categoryRepository,
        EntityManager entityManager,
        RabbitTemplate rabbitTemplate,
        RestTemplate restTemplate
    ) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
    }

    @Transactional(readOnly = true)
    public BookDTO getBook(Long bookId) {
        Book book = getBookOrThrow(bookId);
        return toBookDTO(book);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailability(Long bookId) {
        Book book = getBookOrThrow(bookId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookId", book.getBookId());
        payload.put("available", book.getAvailableCopies() > 0 && book.getStatus() != BookStatus.REMOVED);
        payload.put("availableCopies", book.getAvailableCopies());
        return payload;
    }

    public BookDTO addBook(BookDTO request) {
        validateCopies(request.getTotalCopies(), request.getTotalCopies());
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("ISBN already exists: " + request.getIsbn());
        }

        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublishYear(request.getPublishYear());
        book.setDescription(request.getDescription());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());
        book.setStatus(deriveAvailabilityStatus(request.getTotalCopies()));

        Book saved = bookRepository.save(book);
        replaceBookCategories(saved.getBookId(), request.getCategoryIds());

        rabbitTemplate.convertAndSend(
            BOOK_EVENTS_EXCHANGE,
            "book.added",
            new BookAddedEvent(
                saved.getBookId(),
                saved.getTitle(),
                saved.getAuthor(),
                saved.getIsbn(),
                saved.getTotalCopies(),
                LocalDateTime.now()
            )
        );
        return toBookDTO(saved);
    }

    public BookDTO updateBook(Long bookId, BookDTO request) {
        Book existing = getBookOrThrow(bookId);
        if (existing.getStatus() == BookStatus.REMOVED) {
            throw new IllegalStateException("Cannot update a removed book.");
        }

        validateCopies(request.getTotalCopies(), request.getAvailableCopies());
        if (bookRepository.existsByIsbnAndBookIdNot(request.getIsbn(), bookId)) {
            throw new IllegalArgumentException("ISBN already exists: " + request.getIsbn());
        }

        Map<String, Object> changedFields = new HashMap<>();
        trackChange(changedFields, "isbn", existing.getIsbn(), request.getIsbn());
        trackChange(changedFields, "title", existing.getTitle(), request.getTitle());
        trackChange(changedFields, "author", existing.getAuthor(), request.getAuthor());
        trackChange(changedFields, "publisher", existing.getPublisher(), request.getPublisher());
        trackChange(changedFields, "publishYear", existing.getPublishYear(), request.getPublishYear());
        trackChange(changedFields, "description", existing.getDescription(), request.getDescription());
        trackChange(changedFields, "totalCopies", existing.getTotalCopies(), request.getTotalCopies());
        trackChange(changedFields, "availableCopies", existing.getAvailableCopies(), request.getAvailableCopies());

        existing.setIsbn(request.getIsbn());
        existing.setTitle(request.getTitle());
        existing.setAuthor(request.getAuthor());
        existing.setPublisher(request.getPublisher());
        existing.setPublishYear(request.getPublishYear());
        existing.setDescription(request.getDescription());
        existing.setTotalCopies(request.getTotalCopies());
        existing.setAvailableCopies(request.getAvailableCopies());
        existing.setStatus(deriveAvailabilityStatus(request.getAvailableCopies()));

        replaceBookCategories(bookId, request.getCategoryIds());
        Book saved = bookRepository.save(existing);

        rabbitTemplate.convertAndSend(
            BOOK_EVENTS_EXCHANGE,
            "book.updated",
            new BookUpdatedEvent(bookId, changedFields, LocalDateTime.now())
        );
        return toBookDTO(saved);
    }

    public void removeFromCatalogue(Long bookId) {
        Book book = getBookOrThrow(bookId);
        if (book.getStatus() == BookStatus.REMOVED) {
            return;
        }

        if (hasActiveBorrows(bookId)) {
            throw new IllegalStateException("Cannot remove book from catalogue while active borrows exist.");
        }

        book.setStatus(BookStatus.REMOVED);
        bookRepository.save(book);

        rabbitTemplate.convertAndSend(
            BOOK_EVENTS_EXCHANGE,
            "book.removed",
            new BookRemovedEvent(book.getBookId(), book.getTitle(), "SOFT_DELETE", LocalDateTime.now())
        );
    }

    public Map<String, Object> decrementCopies(Long bookId) {
        Book book = getBookOrThrow(bookId);
        if (book.getStatus() == BookStatus.REMOVED) {
            throw new IllegalStateException("Removed books cannot be borrowed.");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No available copies to decrement.");
        }
        int previous = book.getAvailableCopies();
        int updated = previous - 1;
        book.setAvailableCopies(updated);
        book.setStatus(deriveAvailabilityStatus(updated));
        bookRepository.save(book);
        publishAvailabilityChanged(book.getBookId(), previous, updated);
        return Map.of("bookId", book.getBookId(), "newAvailableCopies", updated);
    }

    public Map<String, Object> incrementCopies(Long bookId) {
        Book book = getBookOrThrow(bookId);
        if (book.getStatus() == BookStatus.REMOVED) {
            throw new IllegalStateException("Removed books cannot be updated.");
        }
        int previous = book.getAvailableCopies();
        int updated = previous + 1;
        if (updated > book.getTotalCopies()) {
            throw new IllegalStateException("Cannot exceed total copies.");
        }
        book.setAvailableCopies(updated);
        book.setStatus(deriveAvailabilityStatus(updated));
        bookRepository.save(book);
        publishAvailabilityChanged(book.getBookId(), previous, updated);
        return Map.of("bookId", book.getBookId(), "newAvailableCopies", updated);
    }

    @Transactional(readOnly = true)
    public PagedResult<BookDTO> searchBooks(
        String title,
        String author,
        Long categoryId,
        BookStatus status,
        Integer publishYearFrom,
        Integer publishYearTo,
        int page,
        int size
    ) {
        Specification<Book> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (title != null && !title.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
        }
        if (author != null && !author.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        } else {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("status"), BookStatus.REMOVED));
        }
        if (publishYearFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("publishYear"), publishYearFrom));
        }
        if (publishYearTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("publishYear"), publishYearTo));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> {
                var sub = query.subquery(Long.class);
                var bc = sub.from(BookCategory.class);
                sub.select(bc.get("bookId"))
                    .where(
                        cb.equal(bc.get("bookId"), root.get("bookId")),
                        cb.equal(bc.get("categoryId"), categoryId)
                    );
                return cb.exists(sub);
            });
        }

        Page<Book> pageResult = bookRepository.findAll(spec, PageRequest.of(page, size));
        List<BookDTO> content = pageResult.getContent().stream().map(this::toBookDTO).toList();
        return new PagedResult<>(content, page, size, pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toCategoryDTO).toList();
    }

    public CategoryDTO createCategory(CategoryDTO request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return toCategoryDTO(categoryRepository.save(category));
    }

    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("Category not found: " + categoryId));
        if (countBooksForCategory(categoryId) > 0) {
            throw new IllegalStateException("Category cannot be deleted while assigned to one or more books.");
        }
        categoryRepository.delete(category);
    }

    @CircuitBreaker(name = "borrowingService", fallbackMethod = "activeBorrowCheckFallback")
    public boolean hasActiveBorrows(Long bookId) {
        String url = BORROWING_SERVICE_BASE_URL + "/api/borrows/book/" + bookId + "/active";
        Boolean hasActive = restTemplate.getForObject(url, Boolean.class);
        return Boolean.TRUE.equals(hasActive);
    }

    public boolean activeBorrowCheckFallback(Long bookId, Throwable throwable) {
        throw new IllegalStateException("Unable to verify borrow status, please try again later.");
    }

    private void publishAvailabilityChanged(Long bookId, int previous, int updated) {
        rabbitTemplate.convertAndSend(
            BOOK_EVENTS_EXCHANGE,
            "book.availability.changed",
            new BookAvailabilityChangedEvent(bookId, previous, updated, LocalDateTime.now())
        );
    }

    private void validateCopies(Integer totalCopies, Integer availableCopies) {
        if (totalCopies == null || totalCopies < 1) {
            throw new IllegalArgumentException("totalCopies must be >= 1");
        }
        if (availableCopies == null || availableCopies < 0 || availableCopies > totalCopies) {
            throw new IllegalArgumentException("availableCopies must satisfy 0 <= availableCopies <= totalCopies");
        }
    }

    private BookStatus deriveAvailabilityStatus(int availableCopies) {
        return availableCopies == 0 ? BookStatus.BORROWED : BookStatus.AVAILABLE;
    }

    private Book getBookOrThrow(Long bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new NoSuchElementException("Book not found: " + bookId));
    }

    private void trackChange(Map<String, Object> changed, String field, Object oldVal, Object newVal) {
        if (!Objects.equals(oldVal, newVal)) {
            changed.put(field, newVal);
        }
    }

    private BookDTO toBookDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setBookId(book.getBookId());
        dto.setIsbn(book.getIsbn());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPublisher(book.getPublisher());
        dto.setPublishYear(book.getPublishYear());
        dto.setDescription(book.getDescription());
        dto.setTotalCopies(book.getTotalCopies());
        dto.setAvailableCopies(book.getAvailableCopies());
        dto.setStatus(book.getStatus());
        dto.setVersion(book.getVersion());
        dto.setCreatedAt(book.getCreatedAt());
        dto.setUpdatedAt(book.getUpdatedAt());
        dto.setCategoryIds(getCategoryIdsForBook(book.getBookId()));
        return dto;
    }

    private CategoryDTO toCategoryDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }

    private List<Long> getCategoryIdsForBook(Long bookId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "select bc.categoryId from BookCategory bc where bc.bookId = :bookId",
            Long.class
        );
        query.setParameter("bookId", bookId);
        return query.getResultList();
    }

    private long countBooksForCategory(Long categoryId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "select count(bc.id) from BookCategory bc where bc.categoryId = :categoryId",
            Long.class
        );
        query.setParameter("categoryId", categoryId);
        return query.getSingleResult();
    }

    private void replaceBookCategories(Long bookId, List<Long> categoryIds) {
        entityManager.createQuery("delete from BookCategory bc where bc.bookId = :bookId")
            .setParameter("bookId", bookId)
            .executeUpdate();

        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        List<Long> distinctIds = categoryIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long categoryId : distinctIds) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new NoSuchElementException("Category not found: " + categoryId);
            }
            entityManager.persist(new BookCategory(bookId, categoryId));
        }
    }
}
