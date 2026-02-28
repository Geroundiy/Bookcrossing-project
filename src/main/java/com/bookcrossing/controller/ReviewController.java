package com.bookcrossing.controller;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.Review;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.BookRepository;
import com.bookcrossing.repository.ReviewRepository;
import com.bookcrossing.service.NotificationService;
import com.bookcrossing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public ReviewController(ReviewRepository reviewRepository,
                            BookRepository bookRepository,
                            UserService userService,
                            NotificationService notificationService) {
        this.reviewRepository    = reviewRepository;
        this.bookRepository      = bookRepository;
        this.userService         = userService;
        this.notificationService = notificationService;
    }

    /** Форма */
    @GetMapping("/add")
    public String showForm(@RequestParam("bookId") Long bookId, Model model) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Книга не найдена: " + bookId));
        model.addAttribute("book", book);
        return "review_add";
    }

    /** Сохранение + уведомление владельцу */
    @Transactional
    @PostMapping("/add")
    public String addReview(@RequestParam("bookId")  Long   bookId,
                            @RequestParam("rating")  int    rating,
                            @RequestParam("comment") String comment,
                            Principal principal,
                            Model model) {

        if (rating < 1 || rating > 5) {
            Book book = bookRepository.findById(bookId).orElseThrow();
            model.addAttribute("book",  book);
            model.addAttribute("error", "Выберите оценку от 1 до 5");
            return "review_add";
        }

        User reviewer = userService.findByUsername(principal.getName());
        Book book     = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Книга не найдена: " + bookId));

        // Сохраняем отзыв
        Review review = new Review();
        review.setUser(reviewer);
        review.setBook(book);
        review.setTargetUser(book.getOwner());
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        // Уведомляем владельца книги (но не самого себя)
        User owner = book.getOwner();
        if (!owner.getUsername().equals(reviewer.getUsername())) {
            String stars = "★".repeat(rating) + "☆".repeat(5 - rating);
            notificationService.sendNotification(
                    owner.getUsername(),
                    "Новый отзыв на вашу книгу",
                    reviewer.getUsername() + " оценил «" + book.getTitle() + "» на " + stars,
                    "/notifications"
            );
        }

        return "redirect:/?reviewSuccess=true";
    }
}