package com.campusflow.domain.library.repository;

import com.campusflow.domain.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR
                   LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:category IS NULL OR b.category = :category)
            """)
    Page<Book> search(@Param("keyword") String keyword,
                      @Param("category") String category,
                      Pageable pageable);
}
