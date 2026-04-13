package com.elibrary.book_service.domain.service;

import com.elibrary.book_service.domain.repository.BookRepository;
import com.elibrary.book_service.domain.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceContractTest {

    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EntityManager entityManager;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private CatalogueManagementService service;

    @Test
    void hasActiveBorrows_returnsTrueWhenBorrowingServiceReturnsNonEmptyArray() {
        when(restTemplate.getForObject(
            "http://borrowing-service/api/borrows/book/42/active",
            Object[].class
        )).thenReturn(new Object[]{new Object()});

        boolean hasActive = service.hasActiveBorrows(42L);

        assertTrue(hasActive);
        verify(restTemplate).getForObject(
            eq("http://borrowing-service/api/borrows/book/42/active"),
            eq(Object[].class)
        );
    }

    @Test
    void hasActiveBorrows_returnsFalseWhenBorrowingServiceReturnsEmptyArray() {
        when(restTemplate.getForObject(
            "http://borrowing-service/api/borrows/book/42/active",
            Object[].class
        )).thenReturn(new Object[0]);

        boolean hasActive = service.hasActiveBorrows(42L);

        assertFalse(hasActive);
    }
}
