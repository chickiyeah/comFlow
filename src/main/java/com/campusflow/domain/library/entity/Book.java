package com.campusflow.domain.library.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(length = 100)
    private String publisher;

    @Column(length = 20, unique = true)
    private String isbn;

    @Column(length = 50)
    private String category;

    @Column(length = 10)
    private String publishYear;

    @Column(nullable = false)
    private int totalCopies;

    @Column(nullable = false)
    private int availableCopies;

    @Column(length = 20)
    private String callNumber; // 청구기호

    @Builder
    public Book(String title, String author, String publisher, String isbn,
                String category, String publishYear, int totalCopies, String callNumber) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
        this.category = category;
        this.publishYear = publishYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
        this.callNumber = callNumber;
    }
}
