package com.campusflow.domain.library.service;

import com.campusflow.domain.library.dto.BookResponse;
import com.campusflow.domain.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public Page<BookResponse> search(String keyword, String category, Pageable pageable) {
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        String cat = (category != null && category.isBlank()) ? null : category;
        return bookRepository.search(kw, cat, pageable).map(BookResponse::from);
    }
}
