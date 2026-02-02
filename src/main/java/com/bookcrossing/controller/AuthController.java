package com.bookcrossing.controller;

import com.bookcrossing.model.User;
import com.bookcrossing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

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
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user) {
        System.out.println("Попытка регистрации пользователя: " + user.getUsername());

        // Проверка: есть ли такой пользователь?
        if (userService.findByUsername(user.getUsername()) != null) {
            System.out.println("Ошибка: Пользователь " + user.getUsername() + " уже существует.");
            return "redirect:/register?error"; // Возврат на регистрацию с ошибкой
        }

        try {
            userService.register(user);
            System.out.println("Пользователь успешно сохранен!");
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/register?error";
        }

        return "redirect:/login";
    }
}