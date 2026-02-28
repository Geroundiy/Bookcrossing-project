package com.bookcrossing.controller;

import com.bookcrossing.model.*;
import com.bookcrossing.repository.BookRepository;
import com.bookcrossing.repository.ModerationLogRepository;
import com.bookcrossing.repository.ReviewRepository;
import com.bookcrossing.service.NotificationService;
import com.bookcrossing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final ModerationLogRepository logRepository;
    private final NotificationService notificationService;

    public AdminController(UserService userService,
                           BookRepository bookRepository,
                           ReviewRepository reviewRepository,
                           ModerationLogRepository logRepository,
                           NotificationService notificationService) {
        this.userService         = userService;
        this.bookRepository      = bookRepository;
        this.reviewRepository    = reviewRepository;
        this.logRepository       = logRepository;
        this.notificationService = notificationService;
    }

    // ── Главная страница панели ───────────────────────────────

    @GetMapping
    public String adminPanel(Model model,
                             @RequestParam(required = false) String query) {
        List<Book> books = (query != null && !query.isBlank())
                ? bookRepository.searchByQuery(query)
                : bookRepository.findAll();

        model.addAttribute("books",    books);
        model.addAttribute("query",    query);
        model.addAttribute("users",    userService.searchUsers(query));
        model.addAttribute("logs",     logRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("allRoles", User.UserRole.values());
        return "admin";
    }

    // ── Удаление книги из каталога ───────────────────────────

    @Transactional
    @PostMapping("/books/{id}/delete")
    public String deleteBook(@PathVariable Long id,
                             @RequestParam(required = false) String reason,
                             Principal principal,
                             RedirectAttributes ra) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            ra.addFlashAttribute("error", "Книга не найдена");
            return "redirect:/admin";
        }

        User moderator = userService.findByUsername(principal.getName());
        String bookTitle = book.getTitle();
        User   owner     = book.getOwner();

        // Удаляем отзывы, потом книгу
        reviewRepository.deleteByBookId(id);
        bookRepository.delete(book);

        // Логируем действие
        saveLog(moderator, ModerationLog.ActionType.BOOK_DELETED, owner,
                id, bookTitle, reason);

        // Уведомляем автора книги
        notificationService.sendNotification(
                owner.getUsername(),
                "Ваша книга удалена модератором",
                "«" + bookTitle + "» была удалена." +
                        (reason != null && !reason.isBlank() ? " Причина: " + reason : ""),
                "/"
        );

        ra.addFlashAttribute("success", "Книга «" + bookTitle + "» удалена.");
        return "redirect:/admin";
    }

    // ── Блокировка пользователя ──────────────────────────────

    @PostMapping("/users/{id}/block")
    public String blockUser(@PathVariable Long id,
                            @RequestParam String reason,
                            @RequestParam(required = false) Integer days,
                            Principal principal,
                            RedirectAttributes ra) {
        User moderator = userService.findByUsername(principal.getName());
        User target    = userService.findById(id);

        // Нельзя заблокировать другого админа
        if (target.getRole() == User.UserRole.ADMIN) {
            ra.addFlashAttribute("error", "Нельзя заблокировать администратора.");
            return "redirect:/admin";
        }

        userService.blockUser(id, reason, days);
        saveLog(moderator, ModerationLog.ActionType.USER_BLOCKED, target,
                null, null, reason);

        notificationService.sendNotification(
                target.getUsername(),
                "Ваш аккаунт заблокирован",
                "Причина: " + reason + (days != null ? ". Срок: " + days + " дней." : ". Бессрочно."),
                "/"
        );

        ra.addFlashAttribute("success", "Пользователь @" + target.getUsername() + " заблокирован.");
        return "redirect:/admin";
    }

    // ── Разблокировка ────────────────────────────────────────

    @PostMapping("/users/{id}/unblock")
    public String unblockUser(@PathVariable Long id,
                              Principal principal,
                              RedirectAttributes ra) {
        User moderator = userService.findByUsername(principal.getName());
        User target    = userService.findById(id);

        userService.unblockUser(id);
        saveLog(moderator, ModerationLog.ActionType.USER_UNBLOCKED, target,
                null, null, "Разблокирован администратором");

        notificationService.sendNotification(
                target.getUsername(),
                "Ваш аккаунт разблокирован",
                "Ограничения с вашего аккаунта сняты.",
                "/"
        );

        ra.addFlashAttribute("success", "Пользователь @" + target.getUsername() + " разблокирован.");
        return "redirect:/admin";
    }

    // ── Изменение роли ───────────────────────────────────────

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam("role") User.UserRole role,
                             Principal principal,
                             RedirectAttributes ra) {
        User moderator = userService.findByUsername(principal.getName());
        User target    = userService.findById(id);

        // Нельзя понизить самого себя
        if (target.getUsername().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Нельзя изменить собственную роль.");
            return "redirect:/admin";
        }

        userService.changeRole(id, role);
        saveLog(moderator, ModerationLog.ActionType.ROLE_CHANGED, target,
                null, null, "Роль изменена на " + role.name());

        notificationService.sendNotification(
                target.getUsername(),
                "Ваша роль изменена",
                "Вам назначена роль: " + role.name(),
                "/"
        );

        ra.addFlashAttribute("success",
                "Роль @" + target.getUsername() + " изменена на " + role.name() + ".");
        return "redirect:/admin";
    }

    // ── Удаление аккаунта ────────────────────────────────────

    @Transactional
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @RequestParam(required = false) String reason,
                             Principal principal,
                             RedirectAttributes ra) {
        User moderator = userService.findByUsername(principal.getName());
        User target    = userService.findById(id);

        if (target.getUsername().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Нельзя удалить собственный аккаунт.");
            return "redirect:/admin";
        }
        if (target.getRole() == User.UserRole.ADMIN) {
            ra.addFlashAttribute("error", "Нельзя удалить аккаунт администратора.");
            return "redirect:/admin";
        }

        String username = target.getUsername();
        saveLog(moderator, ModerationLog.ActionType.USER_DELETED, target,
                null, null, reason);
        userService.deleteUser(id);

        ra.addFlashAttribute("success", "Аккаунт @" + username + " удалён.");
        return "redirect:/admin";
    }

    // ── Вспомогательный метод логирования ───────────────────

    private void saveLog(User moderator, ModerationLog.ActionType action,
                         User target, Long bookId, String bookTitle, String reason) {
        ModerationLog log = new ModerationLog();
        log.setModerator(moderator);
        log.setAction(action);
        log.setTargetUser(target);
        log.setBookId(bookId);
        log.setBookTitle(bookTitle);
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
    }
}