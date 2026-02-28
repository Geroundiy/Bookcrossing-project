package com.bookcrossing.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rating; // 1-5

    @Column(length = 1000)
    private String comment;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Автор отзыва

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public User getTargetUser() { return targetUser; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }
}