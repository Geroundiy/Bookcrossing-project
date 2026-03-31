package com.bookcrossing.controller;

import com.bookcrossing.model.User;
import com.bookcrossing.service.AchievementService;
import com.bookcrossing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.Base64;

@Controller
public class ProfileController {

    private final UserService userService;
    private final AchievementService achievementService;

    public ProfileController(UserService userService,
                             AchievementService achievementService) {
        this.userService = userService;
        this.achievementService = achievementService;
    }

    /** Свой профиль */
    @GetMapping("/profile")
    public String myProfile(Principal principal, Model model) {
        User me = userService.findByUsername(principal.getName());
        model.addAttribute("profileUser", me);
        model.addAttribute("isOwnProfile", true);
        achievementService.checkAndAward(me);
        return "profile";
    }

    /** Профиль другого пользователя */
    @GetMapping("/users/{username}")
    public String userProfile(@PathVariable String username,
                              Principal principal, Model model) {
        User target = userService.findByUsername(username);
        model.addAttribute("profileUser", target);
        model.addAttribute("isOwnProfile",
                principal != null && principal.getName().equals(username));
        return "profile";
    }

    /** Страница редактирования профиля */
    @GetMapping("/profile/edit")
    public String editForm(Principal principal, Model model) {
        User me = userService.findByUsername(principal.getName());
        model.addAttribute("user", me);
        // Передаём константу в шаблон — фронтенд и бэкенд синхронизированы (#7)
        model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
        return "profile-edit";
    }

    /** Сохранение изменений профиля */
    @Transactional
    @PostMapping("/profile/edit")
    public String editSave(Principal principal,
                           @RequestParam(required = false) String fullName,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String city,
                           @RequestParam(required = false) String country,
                           @RequestParam(required = false) String gender,
                           @RequestParam(required = false) String birthDate,
                           @RequestParam(required = false) String aboutMe,
                           @RequestParam(required = false) String socialLinks,
                           @RequestParam(required = false) String favoriteGenres,
                           @RequestParam(required = false) String currentPassword,
                           @RequestParam(required = false) String newPassword,
                           @RequestParam(required = false) String confirmPassword,
                           @RequestParam(required = false, name = "avatarFile") MultipartFile avatarFile,
                           RedirectAttributes ra, Model model) {

        User me = userService.findByUsername(principal.getName());

        // Валидация email
        if (email != null && !email.isBlank()
                && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            model.addAttribute("user", me);
            model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
            model.addAttribute("error", "Некорректный формат email.");
            return "profile-edit";
        }

        // Валидация «О себе»
        if (aboutMe != null && aboutMe.length() > 1000) {
            model.addAttribute("user", me);
            model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
            model.addAttribute("error", "Раздел «О себе» не должен превышать 1000 символов.");
            return "profile-edit";
        }

        // Смена пароля
        if (newPassword != null && !newPassword.isBlank()) {

            // Длина — единая константа (#7, было 6 — теперь всегда MIN_PASSWORD_LENGTH)
            if (newPassword.length() < UserService.MIN_PASSWORD_LENGTH) {
                model.addAttribute("user", me);
                model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
                model.addAttribute("error",
                        "Новый пароль должен содержать минимум "
                                + UserService.MIN_PASSWORD_LENGTH + " символов.");
                return "profile-edit";
            }

            // Допустимые символы — единое регулярное выражение (#7)
            if (!newPassword.matches(UserService.PASSWORD_PATTERN)) {
                model.addAttribute("user", me);
                model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
                model.addAttribute("error",
                        "Пароль может содержать только латинские буквы, цифры и спецсимволы.");
                return "profile-edit";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("user", me);
                model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
                model.addAttribute("error", "Пароли не совпадают.");
                return "profile-edit";
            }
            if (!userService.changePassword(me, currentPassword, newPassword, model)) {
                model.addAttribute("user", me);
                model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
                return "profile-edit";
            }
        }

        // Аватар
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String ct = avatarFile.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                model.addAttribute("user", me);
                model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
                model.addAttribute("error", "Файл аватара должен быть изображением.");
                return "profile-edit";
            }
            if (avatarFile.getSize() > 3 * 1024 * 1024) {
                model.addAttribute("user", me);
                model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
                model.addAttribute("error", "Размер аватара не должен превышать 3 МБ.");
                return "profile-edit";
            }
            try {
                String b64 = Base64.getEncoder().encodeToString(avatarFile.getBytes());
                me.setAvatarUrl("data:" + ct + ";base64," + b64);
            } catch (IOException e) {
                model.addAttribute("user", me);
                model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
                model.addAttribute("error", "Ошибка загрузки аватара.");
                return "profile-edit";
            }
        }

        // Обновляем поля
        if (fullName      != null) me.setFullName(fullName.trim());
        if (email         != null) me.setEmail(email.trim());
        if (city          != null) me.setCity(city.trim());
        if (country       != null) me.setCountry(country.trim());
        if (gender        != null) me.setGender(gender);
        if (aboutMe       != null) me.setAboutMe(aboutMe.trim());
        if (socialLinks   != null) me.setSocialLinks(socialLinks.trim());
        if (favoriteGenres!= null) me.setFavoriteGenres(favoriteGenres.trim());
        if (birthDate     != null && !birthDate.isBlank()) {
            try { me.setBirthDate(java.time.LocalDate.parse(birthDate)); }
            catch (Exception ignored) {}
        }

        userService.saveUser(me);
        ra.addFlashAttribute("success", "Профиль успешно обновлён!");
        return "redirect:/profile";
    }
}