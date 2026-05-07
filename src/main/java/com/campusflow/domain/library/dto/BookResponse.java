package com.campusflow.domain.library.dto;

import com.campusflow.domain.library.entity.Book;

public record BookResponse(
        Long id,
        String title,
        String author,
        String publisher,
        String isbn,
        String category,
        String publishYear,
        int totalCopies,
        int availableCopies,
        String callNumber,
        boolean available
) {
    public static BookResponse from(Book b) {
        return new BookResponse(
                b.getId(), b.getTitle(), b.getAuthor(), b.getPublisher(),
                b.getIsbn(), b.getCategory(), b.getPublishYear(),
                b.getTotalCopies(), b.getAvailableCopies(),
                b.getCallNumber(), b.getAvailableCopies() > 0
        );
    }
}
