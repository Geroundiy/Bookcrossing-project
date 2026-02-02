package com.bookcrossing.repository;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    // Поиск по названию или автору (US 4.1)
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);

    // Книги конкретного пользователя (US 2.2)
    List<Book> findByOwner(User owner);
}