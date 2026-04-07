package com.elibrary.book_service.domain.repository;

import com.elibrary.book_service.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    boolean existsByIsbn(String isbn);
    boolean existsByIsbnAndBookIdNot(String isbn, Long bookId);
}
