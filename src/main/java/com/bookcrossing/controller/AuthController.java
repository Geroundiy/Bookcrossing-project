package com.bookcrossing.controller;

import com.bookcrossing.model.User;
import com.bookcrossing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        // Передаём константу минимальной длины пароля в шаблон
        // чтобы фронтенд и бэкенд всегда были синхронизированы (#7)
        model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user,
                               @RequestParam String confirmPassword,
                               Model model) {

        // 1. Пароли совпадают?
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Пароли не совпадают!");
            model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
            return "register";
        }

        // 2. Длина пароля — единая константа (#7)
        if (user.getPassword().length() < UserService.MIN_PASSWORD_LENGTH) {
            model.addAttribute("error",
                    "Пароль должен быть не менее " + UserService.MIN_PASSWORD_LENGTH + " символов!");
            model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
            return "register";
        }

        // 3. Только допустимые символы — единое регулярное выражение (#7)
        if (!user.getPassword().matches(UserService.PASSWORD_PATTERN)) {
            model.addAttribute("error",
                    "Пароль может содержать только латинские буквы, цифры и спецсимволы!");
            model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
            return "register";
        }

        // 4. Занят ли логин?
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Пользователь с таким именем уже существует!");
            model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
            return "register";
        }

        userService.register(user);
        return "redirect:/login?success";
    }
}