package com.bookcrossing.repository;

import com.bookcrossing.model.Review;
import com.bookcrossing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTargetUser(User targetUser);

    List<Review> findByBookId(Long bookId);

    // Средний рейтинг книги
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Long bookId);

    // Количество отзывов
    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId")
    Long countByBookId(@Param("bookId") Long bookId);

    // Удаление всех отзывов книги (нужно перед удалением самой книги)
    @Modifying
    @Query("DELETE FROM Review r WHERE r.book.id = :bookId")
    void deleteByBookId(@Param("bookId") Long bookId);
}