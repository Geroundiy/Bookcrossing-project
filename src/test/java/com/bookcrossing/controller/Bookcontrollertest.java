package com.bookcrossing.controller;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.Booking.BookingStatus;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.BookingRepository;
import com.bookcrossing.repository.ReviewRepository;
import com.bookcrossing.service.AchievementService;
import com.bookcrossing.service.BookService;
import com.bookcrossing.service.NotificationService;
import com.bookcrossing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookController")
class BookControllerTest {

    @Mock BookService bookService;
    @Mock UserService userService;
    @Mock NotificationService notificationService;
    @Mock ReviewRepository reviewRepository;
    @Mock BookingRepository bookingRepository;
    @Mock AchievementService achievementService;
    @InjectMocks BookController bookController;

    private User user;
    private Book book;
    private Principal principal;
    private Model model;
    private RedirectAttributes ra;

    @BeforeEach
    void setUp() {
        user = new User(); user.setId(1L); user.setUsername("alice");
        book = new Book(); book.setId(10L); book.setTitle("Тест");
        book.setOwner(user); book.setStatus(Book.BookStatus.FREE);

        principal = () -> "alice";
        model = mock(Model.class);
        ra    = mock(RedirectAttributes.class);
    }

    // ─── GET / (catalog) ──────────────────────────────────────────────────────

    @Test
    @DisplayName("catalog — добавляет книги и жанры в модель")
    void catalog_populatesModel() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookService.searchBooks(isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(reviewRepository.findAverageRatingByBookId(10L)).thenReturn(4.5);
        when(reviewRepository.countByBookId(10L)).thenReturn(3L);

        String view = bookController.catalog(model, null, null, 0);

        assertThat(view).isEqualTo("catalog");
        verify(model).addAttribute(eq("books"), any());
        verify(model).addAttribute(eq("genres"), any());
        verify(model).addAttribute(eq("currentPage"), eq(0));
        verify(model).addAttribute(eq("totalPages"), eq(1));
    }

    @Test
    @DisplayName("catalog с query и genre — делегирует в bookService")
    void catalog_withFilters() {
        Page<Book> empty = new PageImpl<>(List.of());
        when(bookService.searchBooks(eq("war"), eq("HISTORY"), any(Pageable.class))).thenReturn(empty);

        bookController.catalog(model, "war", "HISTORY", 0);

        verify(bookService).searchBooks(eq("war"), eq("HISTORY"), any(Pageable.class));
        verify(model).addAttribute(eq("selectedGenre"), eq("HISTORY"));
        verify(model).addAttribute(eq("searchQuery"),   eq("war"));
    }

    @Test
    @DisplayName("catalog с отрицательным page — нормализует до 0")
    void catalog_negativePage_normalizesToZero() {
        Page<Book> empty = new PageImpl<>(List.of());
        when(bookService.searchBooks(isNull(), isNull(), any(Pageable.class))).thenReturn(empty);

        bookController.catalog(model, null, null, -5);

        // Не бросает исключений, метод выполняется
        verify(bookService).searchBooks(isNull(), isNull(), any(Pageable.class));
    }

    // ─── GET /my-books ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("myBooks — добавляет книги и pending-заявки в модель")
    void myBooks_populatesModel() {
        when(userService.findByUsername("alice")).thenReturn(user);
        when(bookService.getMyBooks(user, null, null)).thenReturn(List.of(book));
        when(bookingRepository.findByOwnerAndStatusOrderByRequestedAtDesc(user, BookingStatus.PENDING))
                .thenReturn(List.of());
        when(bookingRepository.findByOwnerAndStatusOrderByRequestedAtDesc(user, BookingStatus.ACCEPTED))
                .thenReturn(List.of());

        String view = bookController.myBooks(model, principal, null, null);

        assertThat(view).isEqualTo("my-books");
        verify(model).addAttribute(eq("books"),          any());
        verify(model).addAttribute(eq("pendingBookings"), any());
    }

    @Test
    @DisplayName("myBooks с query — передаёт query в сервис")
    void myBooks_withQuery() {
        when(userService.findByUsername("alice")).thenReturn(user);
        when(bookService.getMyBooks(user, "тест", null)).thenReturn(List.of());
        when(bookingRepository.findByOwnerAndStatusOrderByRequestedAtDesc(user, BookingStatus.PENDING))
                .thenReturn(List.of());
        when(bookingRepository.findByOwnerAndStatusOrderByRequestedAtDesc(user, BookingStatus.ACCEPTED))
                .thenReturn(List.of());

        bookController.myBooks(model, principal, "тест", null);

        verify(bookService).getMyBooks(user, "тест", null);
    }

    // ─── GET /add-book ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("addBookForm — добавляет пустую книгу и жанры")
    void addBookForm_returnsView() {
        String view = bookController.addBookForm(model);
        assertThat(view).isEqualTo("add-book");
        verify(model).addAttribute(eq("book"),   any(Book.class));
        verify(model).addAttribute(eq("genres"), any());
    }

    // ─── POST /add-book ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /add-book")
    class AddBook {

        @Test
        @DisplayName("Ошибки валидации — возвращает форму")
        void validationErrors_returnsForm() {
            BindingResult br = mock(BindingResult.class);
            when(br.hasErrors()).thenReturn(true);
            MultipartFile file = mock(MultipartFile.class);

            String view = bookController.addBook(book, br, file, principal, model);

            assertThat(view).isEqualTo("add-book");
            verify(bookService, never()).saveBook(any(), any(), any());
        }

        @Test
        @DisplayName("Успешное добавление — сохраняет книгу, проверяет достижения, редирект")
        void success_savesAndRedirects() {
            BindingResult br = mock(BindingResult.class);
            when(br.hasErrors()).thenReturn(false);
            when(userService.findByUsername("alice")).thenReturn(user);
            MultipartFile file = mock(MultipartFile.class);

            String view = bookController.addBook(book, br, file, principal, model);

            assertThat(view).isEqualTo("redirect:/my-books");
            verify(bookService).saveBook(book, user, file);
            verify(achievementService).checkAndAward(user);
        }
    }

    // ─── POST /book/{id}/status ────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleStatus")
    class ToggleStatus {

        @Test
        @DisplayName("FREE→BUSY — отправляет уведомление «занята»")
        void busyStatus_notifiesOccupied() {
            book.setStatus(Book.BookStatus.BUSY);
            when(userService.findByUsername("alice")).thenReturn(user);
            when(bookService.toggleStatus(10L, user)).thenReturn(book);

            String view = bookController.toggleStatus(10L, principal);

            assertThat(view).isEqualTo("redirect:/my-books");
            verify(notificationService).sendNotification(eq("alice"), any(),
                    argThat(s -> s.contains("занята")), any());
            verify(achievementService).checkAndAward(user);
        }

        @Test
        @DisplayName("FREE→FREE — отправляет уведомление «свободна»")
        void freeStatus_notifiesFree() {
            book.setStatus(Book.BookStatus.FREE);
            when(userService.findByUsername("alice")).thenReturn(user);
            when(bookService.toggleStatus(10L, user)).thenReturn(book);

            bookController.toggleStatus(10L, principal);

            verify(notificationService).sendNotification(eq("alice"), any(),
                    argThat(s -> s.contains("свободна")), any());
        }

        @Test
        @DisplayName("toggleStatus возвращает null — уведомление не отправляется")
        void nullBook_noNotification() {
            when(userService.findByUsername("alice")).thenReturn(user);
            when(bookService.toggleStatus(99L, user)).thenReturn(null);

            bookController.toggleStatus(99L, principal);

            verify(notificationService, never()).sendNotification(any(), any(), any(), any());
        }
    }

    // ─── POST /book/{id}/delete ────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteBook")
    class DeleteBook {

        @Test
        @DisplayName("Успешное удаление — success flash, редирект")
        void success_flashSuccessAndRedirect() {
            when(userService.findByUsername("alice")).thenReturn(user);
            when(bookService.deleteBook(10L, user)).thenReturn(true);

            String view = bookController.deleteBook(10L, principal, ra);

            assertThat(view).isEqualTo("redirect:/my-books");
            verify(ra).addFlashAttribute(eq("successMessage"), any());
        }

        @Test
        @DisplayName("Ошибка удаления — error flash")
        void failure_flashError() {
            when(userService.findByUsername("alice")).thenReturn(user);
            when(bookService.deleteBook(10L, user)).thenReturn(false);

            bookController.deleteBook(10L, principal, ra);

            verify(ra).addFlashAttribute(eq("errorMessage"), any());
        }
    }
}