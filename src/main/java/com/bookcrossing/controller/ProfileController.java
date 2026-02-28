package com.bookcrossing.controller;

import com.bookcrossing.model.BookGenre;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.ReviewRepository; // Добавлено
import com.bookcrossing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.ArrayList;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final ReviewRepository reviewRepository; // Добавлено

    public ProfileController(UserService userService, ReviewRepository reviewRepository) {
        this.userService = userService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public String viewProfile(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("books", user.getBooks());

        // Получаем отзывы, оставленные этому пользователю (владельцу книг)
        model.addAttribute("reviews", reviewRepository.findByTargetUser(user));
        model.addAttribute("exchangeHistory", new ArrayList<String>());

        return "profile";
    }

    @GetMapping("/edit")
    public String editProfileForm(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("allGenres", BookGenre.values());
        return "profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@ModelAttribute User updatedData,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        userService.updateProfile(currentUser, updatedData, avatarFile);
        return "redirect:/profile";
    }
}