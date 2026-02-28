package com.bookcrossing.repository;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // ── Все книги пользователя ──────────────────────────────
    List<Book> findByOwner(User owner);

    // ── Поиск по заголовку или автору (без фильтра жанра) ──
    @Query("""
        SELECT b FROM Book b
        WHERE LOWER(b.title)  LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(b.author) LIKE LOWER(CONCAT('%',:q,'%'))
        """)
    List<Book> searchByQuery(@Param("q") String query);

    // ── Только фильтр по жанру ──────────────────────────────
    @Query("SELECT b FROM Book b WHERE LOWER(b.genre) = LOWER(:genre)")
    List<Book> searchByGenre(@Param("genre") String genre);

    // ── Поиск + фильтр жанра ───────────────────────────────
    @Query("""
        SELECT b FROM Book b
        WHERE (LOWER(b.title)  LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(b.author) LIKE LOWER(CONCAT('%',:q,'%')))
          AND LOWER(b.genre) = LOWER(:genre)
        """)
    List<Book> searchByQueryAndGenre(@Param("q") String query,
                                     @Param("genre") String genre);

    // ── Поиск по книгам конкретного пользователя ───────────
    @Query("""
        SELECT b FROM Book b
        WHERE b.owner = :owner
          AND (LOWER(b.title)  LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(b.author) LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    List<Book> searchByOwnerAndQuery(@Param("owner") User owner,
                                     @Param("q")     String query);
}