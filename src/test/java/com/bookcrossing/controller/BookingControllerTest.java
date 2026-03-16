package com.bookcrossing.controller;

import com.bookcrossing.model.*;
import com.bookcrossing.model.Booking.BookingStatus;
import com.bookcrossing.repository.BookRepository;
import com.bookcrossing.repository.BookingRepository;
import com.bookcrossing.service.NotificationService;
import com.bookcrossing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingController")
class BookingControllerTest {

    @Mock BookingRepository   bookingRepository;
    @Mock BookRepository      bookRepository;
    @Mock UserService         userService;
    @Mock NotificationService notificationService;
    @InjectMocks BookingController bookingController;

    private User  owner;
    private User  requester;
    private Book  book;
    private Principal ownerPrincipal;
    private Principal requesterPrincipal;
    private RedirectAttributes ra;

    @BeforeEach
    void setUp() {
        owner = new User(); owner.setId(1L); owner.setUsername("owner");
        requester = new User(); requester.setId(2L); requester.setUsername("requester");

        book = new Book(); book.setId(10L); book.setTitle("Test Book");
        book.setOwner(owner); book.setStatus(Book.BookStatus.FREE);

        ownerPrincipal     = () -> "owner";
        requesterPrincipal = () -> "requester";
        ra = mock(RedirectAttributes.class);
    }

    // ─── requestBooking ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("requestBooking")
    class RequestBooking {

        @Test
        @DisplayName("Успешный запрос — бронь сохранена, уведомление отправлено")
        void success_saveBookingAndNotify() {
            when(userService.findByUsername("requester")).thenReturn(requester);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.existsByBookAndRequesterAndStatus(book, requester, BookingStatus.PENDING))
                    .thenReturn(false);

            String redirect = bookingController.requestBooking(10L, "Хочу почитать", 7,
                    requesterPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/");
            ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(cap.capture());
            Booking saved = cap.getValue();
            assertThat(saved.getRequester()).isEqualTo(requester);
            assertThat(saved.getBook()).isEqualTo(book);
            assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(saved.getMessage()).isEqualTo("Хочу почитать");
            verify(notificationService).sendNotification(eq("owner"), contains("бронирование"), any(), any());
        }

        @Test
        @DisplayName("Бронирование своей книги — ошибка, redirect:/")
        void ownBook_errorRedirect() {
            when(userService.findByUsername("owner")).thenReturn(owner);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            String redirect = bookingController.requestBooking(10L, null, null, ownerPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/");
            verify(ra).addFlashAttribute(eq("error"), contains("собственную"));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Книга не FREE — ошибка")
        void bookNotFree_errorRedirect() {
            book.setStatus(Book.BookStatus.BUSY);
            when(userService.findByUsername("requester")).thenReturn(requester);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            String redirect = bookingController.requestBooking(10L, null, null, requesterPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/");
            verify(ra).addFlashAttribute(eq("error"), any());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Уже есть PENDING заявка — ошибка")
        void duplicatePending_errorRedirect() {
            when(userService.findByUsername("requester")).thenReturn(requester);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.existsByBookAndRequesterAndStatus(book, requester, BookingStatus.PENDING))
                    .thenReturn(true);

            String redirect = bookingController.requestBooking(10L, null, null, requesterPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/");
            verify(ra).addFlashAttribute(eq("error"), contains("уже"));
        }

        @Test
        @DisplayName("Без сообщения — message не устанавливается")
        void noMessage_messageNull() {
            when(userService.findByUsername("requester")).thenReturn(requester);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.existsByBookAndRequesterAndStatus(any(), any(), any())).thenReturn(false);

            bookingController.requestBooking(10L, "  ", null, requesterPrincipal, ra);

            ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(cap.capture());
            assertThat(cap.getValue().getMessage()).isNull();
        }

        @Test
        @DisplayName("С указанным сроком — bookedUntil заполняется")
        void withDays_bookedUntilSet() {
            when(userService.findByUsername("requester")).thenReturn(requester);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.existsByBookAndRequesterAndStatus(any(), any(), any())).thenReturn(false);

            bookingController.requestBooking(10L, "msg", 14, requesterPrincipal, ra);

            ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(cap.capture());
            assertThat(cap.getValue().getBookedUntil()).isNotNull();
        }

        @Test
        @DisplayName("Без срока — bookedUntil остаётся null")
        void noDays_bookedUntilNull() {
            when(userService.findByUsername("requester")).thenReturn(requester);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.existsByBookAndRequesterAndStatus(any(), any(), any())).thenReturn(false);

            bookingController.requestBooking(10L, null, null, requesterPrincipal, ra);

            ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(cap.capture());
            assertThat(cap.getValue().getBookedUntil()).isNull();
        }
    }

    // ─── acceptBooking ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptBooking")
    class AcceptBooking {

        @Test
        @DisplayName("Успешное одобрение — статус ACCEPTED, книга BOOKED")
        void success_acceptsAndSetsBookBooked() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            String redirect = bookingController.acceptBooking(1L, "Жду вас в среду", ownerPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/my-books");
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
            assertThat(book.getStatus()).isEqualTo(Book.BookStatus.BOOKED);
            verify(bookRepository).save(book);
            verify(notificationService).sendNotification(eq("requester"), contains("одобрено"), any(), any());
        }

        @Test
        @DisplayName("Заявка не найдена — ошибка")
        void notFound_error() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
            String redirect = bookingController.acceptBooking(99L, null, ownerPrincipal, ra);
            assertThat(redirect).isEqualTo("redirect:/my-books");
            verify(ra).addFlashAttribute(eq("error"), any());
        }

        @Test
        @DisplayName("Чужой владелец — возвращает ошибку")
        void wrongOwner_error() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            String redirect = bookingController.acceptBooking(1L, null, requesterPrincipal, ra);
            assertThat(redirect).isEqualTo("redirect:/my-books");
            verify(ra).addFlashAttribute(eq("error"), any());
        }

        @Test
        @DisplayName("Заявка не PENDING — возвращает ошибку")
        void alreadyAccepted_error() {
            Booking booking = acceptedBooking();
            // acceptedBooking уже ACCEPTED, а метод ищет PENDING
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            // Владелец совпадает, но статус не PENDING — findPendingForOwner вернёт null
            String redirect = bookingController.acceptBooking(1L, null, ownerPrincipal, ra);
            assertThat(redirect).isEqualTo("redirect:/my-books");
            verify(ra).addFlashAttribute(eq("error"), any());
        }
    }

    // ─── rejectBooking ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectBooking")
    class RejectBooking {

        @Test
        @DisplayName("Успешное отклонение — статус REJECTED, уведомление отправлено")
        void success_rejectsBooking() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            String redirect = bookingController.rejectBooking(1L, "Уже нашёл читателя", ownerPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/my-books");
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);
            verify(notificationService).sendNotification(eq("requester"), contains("отклонено"), any(), any());
        }

        @Test
        @DisplayName("Не найдена — ошибка")
        void notFound_error() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
            String redirect = bookingController.rejectBooking(99L, null, ownerPrincipal, ra);
            verify(ra).addFlashAttribute(eq("error"), any());
        }

        @Test
        @DisplayName("Причина отклонения сохраняется в ownerResponse")
        void responseSetCorrectly() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            bookingController.rejectBooking(1L, "Причина", ownerPrincipal, ra);

            assertThat(booking.getOwnerResponse()).isEqualTo("Причина");
        }

        @Test
        @DisplayName("Null response — ownerResponse=null")
        void nullResponse_ownerResponseNull() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            bookingController.rejectBooking(1L, null, ownerPrincipal, ra);

            assertThat(booking.getOwnerResponse()).isNull();
        }
    }

    // ─── completeBooking ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeBooking")
    class CompleteBooking {

        @Test
        @DisplayName("Успешное завершение — статус COMPLETED, книга BUSY")
        void success_completesAndSetsBusy() {
            Booking booking = acceptedBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            String redirect = bookingController.completeBooking(1L, ownerPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/my-books");
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
            assertThat(book.getStatus()).isEqualTo(Book.BookStatus.BUSY);
            verify(notificationService).sendNotification(eq("requester"), any(), contains("Приятного чтения"), any());
        }

        @Test
        @DisplayName("Бронь не ACCEPTED — ошибка")
        void notAccepted_error() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            String redirect = bookingController.completeBooking(1L, ownerPrincipal, ra);
            verify(ra).addFlashAttribute(eq("error"), any());
        }

        @Test
        @DisplayName("Бронь не найдена — ошибка")
        void notFound_error() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
            bookingController.completeBooking(99L, ownerPrincipal, ra);
            verify(ra).addFlashAttribute(eq("error"), any());
        }

        @Test
        @DisplayName("Чужой владелец — ошибка")
        void wrongOwner_error() {
            Booking booking = acceptedBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            // requesterPrincipal != owner
            bookingController.completeBooking(1L, requesterPrincipal, ra);

            verify(ra).addFlashAttribute(eq("error"), any());
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED); // не изменился
        }
    }

    // ─── releaseBooking ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("releaseBooking")
    class ReleaseBooking {

        @Test
        @DisplayName("Успешная отмена ACCEPTED брони — статус CANCELLED, книга FREE")
        void success_releasesAndSetsFree() {
            Booking booking = acceptedBooking();
            book.setStatus(Book.BookStatus.BOOKED);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            String redirect = bookingController.releaseBooking(1L, ownerPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/my-books");
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(book.getStatus()).isEqualTo(Book.BookStatus.FREE);
            verify(bookRepository).save(book);
            verify(notificationService).sendNotification(eq("requester"), contains("отменена"), any(), any());
            verify(ra).addFlashAttribute(eq("successMessage"), any());
        }

        @Test
        @DisplayName("Бронь не найдена — ошибка")
        void notFound_error() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
            bookingController.releaseBooking(99L, ownerPrincipal, ra);
            verify(ra).addFlashAttribute(eq("errorMessage"), any());
        }

        @Test
        @DisplayName("Чужой владелец — ошибка")
        void wrongOwner_error() {
            Booking booking = acceptedBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            bookingController.releaseBooking(1L, requesterPrincipal, ra);

            verify(ra).addFlashAttribute(eq("errorMessage"), any());
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED); // не изменился
        }

        @Test
        @DisplayName("Бронь не ACCEPTED (PENDING) — ошибка")
        void notAccepted_error() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            bookingController.releaseBooking(1L, ownerPrincipal, ra);

            verify(ra).addFlashAttribute(eq("errorMessage"), any());
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Бронь не ACCEPTED (COMPLETED) — ошибка")
        void completedBooking_error() {
            Booking booking = pendingBooking();
            booking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            bookingController.releaseBooking(1L, ownerPrincipal, ra);

            verify(ra).addFlashAttribute(eq("errorMessage"), any());
        }
    }

    // ─── cancelBooking ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("Отмена PENDING заявки — статус CANCELLED")
        void cancelPending_success() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            String redirect = bookingController.cancelBooking(1L, requesterPrincipal, ra);

            assertThat(redirect).isEqualTo("redirect:/");
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Отмена ACCEPTED брони — книга возвращается в FREE")
        void cancelAccepted_bookBecomeFree() {
            Booking booking = acceptedBooking();
            book.setStatus(Book.BookStatus.BOOKED);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            bookingController.cancelBooking(1L, requesterPrincipal, ra);

            assertThat(book.getStatus()).isEqualTo(Book.BookStatus.FREE);
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("Чужая заявка — ошибка")
        void wrongUser_error() {
            Booking booking = pendingBooking();
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            bookingController.cancelBooking(1L, ownerPrincipal, ra);
            verify(ra).addFlashAttribute(eq("error"), any());
        }

        @Test
        @DisplayName("Заявка не найдена — ошибка")
        void notFound_error() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
            bookingController.cancelBooking(99L, requesterPrincipal, ra);
            verify(ra).addFlashAttribute(eq("error"), any());
        }

        @Test
        @DisplayName("Отмена ACCEPTED — книга уже не BOOKED — статус книги не меняется")
        void cancelAccepted_bookAlreadyFree_noChange() {
            Booking booking = acceptedBooking();
            book.setStatus(Book.BookStatus.FREE); // уже свободна
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            bookingController.cancelBooking(1L, requesterPrincipal, ra);

            // save не должен вызываться, если статус уже FREE
            verify(bookRepository, never()).save(any());
        }
    }

    // ─── getActiveBooking ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getActiveBooking (REST)")
    class GetActiveBooking {

        @Test
        @DisplayName("Книга не найдена — 404")
        void bookNotFound_404() {
            when(bookRepository.findById(99L)).thenReturn(Optional.empty());

            ResponseEntity<Map<String, Object>> resp =
                    bookingController.getActiveBooking(99L);

            assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Активная бронь найдена — 200 с данными")
        void activeBookingFound_200WithData() {
            Booking booking = acceptedBooking();
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.findActiveBookingForBook(book, BookingStatus.ACCEPTED))
                    .thenReturn(Optional.of(booking));

            ResponseEntity<Map<String, Object>> resp =
                    bookingController.getActiveBooking(10L);

            assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody()).containsKey("requester");
            assertThat(resp.getBody().get("requester")).isEqualTo("requester");
        }

        @Test
        @DisplayName("Нет активной брони — 200 с null телом")
        void noActiveBooking_200WithNull() {
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.findActiveBookingForBook(book, BookingStatus.ACCEPTED))
                    .thenReturn(Optional.empty());

            ResponseEntity<Map<String, Object>> resp =
                    bookingController.getActiveBooking(10L);

            assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(resp.getBody()).isNull();
        }

        @Test
        @DisplayName("bookedUntil присутствует в ответе если задан")
        void bookedUntilPresent() {
            Booking booking = acceptedBooking();
            booking.setBookedUntil(java.time.LocalDateTime.of(2025, 12, 31, 23, 59));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.findActiveBookingForBook(book, BookingStatus.ACCEPTED))
                    .thenReturn(Optional.of(booking));

            ResponseEntity<Map<String, Object>> resp =
                    bookingController.getActiveBooking(10L);

            assertThat(resp.getBody()).containsKey("bookedUntil");
            assertThat(resp.getBody().get("bookedUntil")).isNotNull();
        }

        @Test
        @DisplayName("bookedUntil = null в ответе если не задан")
        void bookedUntilNull_inResponse() {
            Booking booking = acceptedBooking();
            booking.setBookedUntil(null);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookingRepository.findActiveBookingForBook(book, BookingStatus.ACCEPTED))
                    .thenReturn(Optional.of(booking));

            ResponseEntity<Map<String, Object>> resp =
                    bookingController.getActiveBooking(10L);

            assertThat(resp.getBody()).containsEntry("bookedUntil", null);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Booking pendingBooking() {
        Booking b = new Booking();
        b.setId(1L);
        b.setBook(book);
        b.setOwner(owner);
        b.setRequester(requester);
        b.setStatus(BookingStatus.PENDING);
        b.setRequestedAt(java.time.LocalDateTime.now());
        return b;
    }

    private Booking acceptedBooking() {
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.ACCEPTED);
        return b;
    }
}