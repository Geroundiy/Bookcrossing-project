package com.bookcrossing.controller;

import com.bookcrossing.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Страница всех уведомлений.
     * При открытии автоматически помечает все как прочитанные.
     */
    @GetMapping
    @Transactional
    public String notificationsPage(Model model, Principal principal) {
        String username = principal.getName();

        // Сначала получаем список (с флагами isRead для отображения)
        model.addAttribute("notifications",
                notificationService.getNotifications(username));

        // Затем помечаем все прочитанными
        notificationService.markAllRead(username);

        return "notifications";
    }

    /**
     * Пометить одно уведомление прочитанным (Ajax-вызов или редирект).
     */
    @PostMapping("/{id}/read")
    @ResponseBody
    public String markRead(@PathVariable Long id, Principal principal) {
        notificationService.markRead(id);
        return "ok";
    }
}