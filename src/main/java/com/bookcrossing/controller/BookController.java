package com.bookcrossing.controller;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import com.bookcrossing.service.BookService;
import com.bookcrossing.service.UserService;
import jakarta.validation.Valid; // Важно
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
public class BookController {
    private final BookService bookService;
    private final UserService userService;

    public BookController(BookService bookService, UserService userService) {
        this.bookService = bookService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String catalog(Model model, @RequestParam(required = false) String query) {
        if (query != null && !query.isEmpty()) {
            model.addAttribute("books", bookService.searchBooks(query));
        } else {
            model.addAttribute("books", bookService.getAllBooks());
        }
        return "catalog";
    }

    @GetMapping("/my-books")
    public String myBooks(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("books", bookService.getMyBooks(user));
        return "my-books";
    }

    @GetMapping("/add-book")
    public String addBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "add-book";
    }

    // Обновленный метод с валидацией
    @PostMapping("/add-book")
    public String addBook(@Valid @ModelAttribute Book book,
                          BindingResult bindingResult,
                          Principal principal) {
        if (bindingResult.hasErrors()) {
            return "add-book"; // Если есть ошибки, вернуть на форму
        }
        User user = userService.findByUsername(principal.getName());
        bookService.saveBook(book, user);
        return "redirect:/my-books";
    }

    @PostMapping("/book/{id}/status")
    public String toggleStatus(@PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        bookService.toggleStatus(id, user);
        return "redirect:/my-books";
    }
}