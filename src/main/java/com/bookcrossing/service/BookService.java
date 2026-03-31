package com.bookcrossing.service;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.BookRepository;
import com.bookcrossing.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    public BookService(BookRepository bookRepository,
                       ReviewRepository reviewRepository) {
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Каталог с пагинацией.
     * Исправление #3: возвращает Page<Book> вместо List<Book>.
     */
    public Page<Book> searchBooks(String query, String genre, Pageable pageable) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasGenre = genre != null && !genre.isBlank();

        if (!hasQuery && !hasGenre) return bookRepository.findAll(pageable);
        if ( hasQuery && !hasGenre) return bookRepository.searchByQuery(query, pageable);
        if (!hasQuery             ) return bookRepository.searchByGenre(genre, pageable);
        return bookRepository.searchByQueryAndGenre(query, genre, pageable);
    }

    /** Мои книги (без пагинации — обычно их немного). */
    public List<Book> getMyBooks(User user, String query, String genre) {
        boolean hasQuery = query != null && !query.isBlank();
        if (!hasQuery) return bookRepository.findByOwner(user);
        return bookRepository.searchByOwnerAndQuery(user, query);
    }

    /** Сохранение книги. */
    @Transactional
    public Book saveBook(Book book, User owner, MultipartFile coverFile) {
        book.setOwner(owner);
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                String base64 = Base64.getEncoder().encodeToString(coverFile.getBytes());
                book.setImageUrl("data:" + coverFile.getContentType() + ";base64," + base64);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка загрузки обложки", e);
            }
        }
        return bookRepository.save(book);
    }

    /** Смена статуса. */
    @Transactional
    public Book toggleStatus(Long bookId, User user) {
        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isEmpty()) return null;
        Book book = opt.get();
        if (!book.getOwner().getId().equals(user.getId())) return null;
        book.setStatus(book.getStatus() == Book.BookStatus.FREE
                ? Book.BookStatus.BUSY : Book.BookStatus.FREE);
        return bookRepository.save(book);
    }

    /** Удаление книги. */
    @Transactional
    public boolean deleteBook(Long bookId, User user) {
        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isEmpty()) return false;
        Book book = opt.get();
        if (!book.getOwner().getId().equals(user.getId())) return false;
        reviewRepository.deleteByBookId(bookId);
        bookRepository.delete(book);
        return true;
    }
}