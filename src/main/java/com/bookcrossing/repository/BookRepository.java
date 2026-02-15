package com.bookcrossing.repository;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // Умный поиск: фильтр по жанру (если выбран) И поиск по тексту (если введен)
    @Query("SELECT b FROM Book b WHERE " +
            "(:genre IS NULL OR :genre = '' OR b.genre = :genre) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Book> searchDocs(@Param("query") String query, @Param("genre") String genre);

    // Поиск книг конкретного пользователя с фильтрацией
    @Query("SELECT b FROM Book b WHERE b.owner = :owner AND " +
            "(:genre IS NULL OR :genre = '' OR b.genre = :genre) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Book> searchMyBooks(@Param("owner") User owner, @Param("query") String query, @Param("genre") String genre);

    List<Book> findByOwner(User owner);
}