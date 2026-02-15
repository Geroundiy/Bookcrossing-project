package com.bookcrossing.service;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
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

    public List<Book> searchBooks(String query, String genre) {
        return bookRepository.searchDocs(query, genre);
    }

    public List<Book> getMyBooks(User user, String query, String genre) {
        return bookRepository.searchMyBooks(user, query, genre);
    }

    public void saveBook(Book book, User owner, MultipartFile file) {
        book.setOwner(owner);
        if (file != null && !file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                String base64Image = Base64.getEncoder().encodeToString(bytes);
                book.setImageUrl("data:" + file.getContentType() + ";base64," + base64Image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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