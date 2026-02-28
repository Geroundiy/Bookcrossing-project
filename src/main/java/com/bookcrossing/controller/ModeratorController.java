package com.bookcrossing.controller;

import com.bookcrossing.model.*;
import com.bookcrossing.repository.ComplaintRepository;
import com.bookcrossing.repository.ModerationLogRepository;
import com.bookcrossing.service.NotificationService;
import com.bookcrossing.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/moderator")
public class ModeratorController {

    private final ComplaintRepository complaintRepository;
    private final ModerationLogRepository logRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public ModeratorController(ComplaintRepository complaintRepository,
                               ModerationLogRepository logRepository,
                               UserService userService,
                               NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.logRepository       = logRepository;
        this.userService         = userService;
        this.notificationService = notificationService;
    }

    // ── Панель модератора ─────────────────────────────────────

    @GetMapping
    public String moderatorPanel(Model model,
                                 @RequestParam(required = false) String status,
                                 Principal principal) {
        // Жалобы с фильтром
        var complaints = (status != null && !status.isBlank())
                ? complaintRepository.findByStatusOrderByCreatedAtDesc(
                Complaint.ComplaintStatus.valueOf(status))
                : complaintRepository.findAllByOrderByCreatedAtDesc();

        model.addAttribute("complaints",      complaints);
        model.addAttribute("selectedStatus",  status);
        model.addAttribute("statuses",        Complaint.ComplaintStatus.values());

        // Статистика
        model.addAttribute("pendingCount",
                complaintRepository.countByStatus(Complaint.ComplaintStatus.PENDING));
        model.addAttribute("acceptedCount",
                complaintRepository.countByStatus(Complaint.ComplaintStatus.ACCEPTED));
        model.addAttribute("rejectedCount",
                complaintRepository.countByStatus(Complaint.ComplaintStatus.REJECTED));
        model.addAttribute("myResolved",
                complaintRepository.countResolvedByModerator(principal.getName()));

        // Лог действий текущего модератора
        model.addAttribute("myLogs",
                logRepository.findByModeratorUsernameOrderByCreatedAtDesc(principal.getName()));

        return "moderator";
    }

    // ── Подать жалобу (любой пользователь) ───────────────────

    @PostMapping("/complaints/submit")
    public String submitComplaint(@RequestParam Long targetBookId,
                                  @RequestParam String targetBookTitle,
                                  @RequestParam String type,
                                  @RequestParam String description,
                                  Principal principal,
                                  RedirectAttributes ra) {
        User author = userService.findByUsername(principal.getName());

        Complaint c = new Complaint();
        c.setAuthor(author);
        c.setTargetBookId(targetBookId);
        c.setTargetBookTitle(targetBookTitle);
        c.setType(Complaint.ComplaintType.valueOf(type));
        c.setDescription(description);
        c.setCreatedAt(LocalDateTime.now());
        complaintRepository.save(c);

        ra.addFlashAttribute("success", "Жалоба отправлена модераторам.");
        return "redirect:/";
    }

    // ── Принять жалобу ────────────────────────────────────────

    @Transactional
    @PostMapping("/complaints/{id}/accept")
    public String acceptComplaint(@PathVariable Long id,
                                  @RequestParam(required = false) String comment,
                                  Principal principal,
                                  RedirectAttributes ra) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Жалоба не найдена"));

        User moderator = userService.findByUsername(principal.getName());
        c.setStatus(Complaint.ComplaintStatus.ACCEPTED);
        c.setModeratorComment(comment);
        c.setResolvedBy(moderator);
        c.setResolvedAt(LocalDateTime.now());
        complaintRepository.save(c);

        // Уведомляем автора жалобы
        notificationService.sendNotification(
                c.getAuthor().getUsername(),
                "Ваша жалоба принята",
                "Жалоба на «" + c.getTargetBookTitle() + "» была рассмотрена и принята.",
                "/notifications"
        );

        // Лог
        saveLog(moderator, ModerationLog.ActionType.COMPLAINT_ACCEPTED,
                c.getAuthor(), comment);

        ra.addFlashAttribute("success", "Жалоба принята.");
        return "redirect:/moderator";
    }

    // ── Отклонить жалобу ──────────────────────────────────────

    @Transactional
    @PostMapping("/complaints/{id}/reject")
    public String rejectComplaint(@PathVariable Long id,
                                  @RequestParam(required = false) String comment,
                                  Principal principal,
                                  RedirectAttributes ra) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Жалоба не найдена"));

        User moderator = userService.findByUsername(principal.getName());
        c.setStatus(Complaint.ComplaintStatus.REJECTED);
        c.setModeratorComment(comment);
        c.setResolvedBy(moderator);
        c.setResolvedAt(LocalDateTime.now());
        complaintRepository.save(c);

        notificationService.sendNotification(
                c.getAuthor().getUsername(),
                "Ваша жалоба отклонена",
                "Жалоба на «" + c.getTargetBookTitle() + "» была отклонена." +
                        (comment != null ? " Причина: " + comment : ""),
                "/notifications"
        );

        saveLog(moderator, ModerationLog.ActionType.COMPLAINT_REJECTED,
                c.getAuthor(), comment);

        ra.addFlashAttribute("success", "Жалоба отклонена.");
        return "redirect:/moderator";
    }

    private void saveLog(User moderator, ModerationLog.ActionType action,
                         User target, String reason) {
        ModerationLog log = new ModerationLog();
        log.setModerator(moderator);
        log.setAction(action);
        log.setTargetUser(target);
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
    }
}