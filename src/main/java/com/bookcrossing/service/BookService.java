package com.bookcrossing.service;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.BookRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> searchBooks(String query) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query);
    }

    public List<Book> getMyBooks(User user) {
        return bookRepository.findByOwner(user);
    }

    public void saveBook(Book book, User owner) {
        book.setOwner(owner);
        bookRepository.save(book);
    }

    public void toggleStatus(Long bookId, User currentUser) {
        Book book = bookRepository.findById(bookId).orElseThrow();
        if (book.getOwner().getId().equals(currentUser.getId())) {
            if (book.getStatus() == Book.BookStatus.FREE) {
                book.setStatus(Book.BookStatus.BUSY);
            } else {
                book.setStatus(Book.BookStatus.FREE);
            }
            bookRepository.save(book);
        }
    }
}