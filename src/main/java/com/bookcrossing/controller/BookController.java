package com.bookcrossing.controller;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.BookGenre;
import com.bookcrossing.model.User;
import com.bookcrossing.service.BookService;
import com.bookcrossing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public String catalog(Model model,
                          @RequestParam(required = false) String query,
                          @RequestParam(required = false) String genre) {
        model.addAttribute("books", bookService.searchBooks(query, genre));
        model.addAttribute("genres", BookGenre.values()); // Для выпадающего списка
        model.addAttribute("selectedGenre", genre);
        model.addAttribute("searchQuery", query);
        return "catalog";
    }

    @GetMapping("/my-books")
    public String myBooks(Model model, Principal principal,
                          @RequestParam(required = false) String query,
                          @RequestParam(required = false) String genre) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("books", bookService.getMyBooks(user, query, genre));
        model.addAttribute("genres", BookGenre.values());
        model.addAttribute("selectedGenre", genre);
        model.addAttribute("searchQuery", query);
        return "my-books";
    }

    @GetMapping("/add-book")
    public String addBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("genres", BookGenre.values());
        return "add-book";
    }

    @PostMapping("/add-book")
    public String addBook(@Valid @ModelAttribute Book book,
                          BindingResult bindingResult,
                          @RequestParam("coverFile") MultipartFile file,
                          Principal principal, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", BookGenre.values());
            return "add-book";
        }
        User user = userService.findByUsername(principal.getName());
        bookService.saveBook(book, user, file);
        return "redirect:/my-books";
    }

    @PostMapping("/book/{id}/status")
    public String toggleStatus(@PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        bookService.toggleStatus(id, user);
        return "redirect:/my-books";
    }
}