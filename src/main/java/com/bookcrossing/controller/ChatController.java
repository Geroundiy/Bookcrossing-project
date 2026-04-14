package com.bookcrossing.controller;

import com.bookcrossing.dto.ConversationDTO;
import com.bookcrossing.model.Message;
import com.bookcrossing.model.User;
import com.bookcrossing.service.ChatService;
import com.bookcrossing.service.NotificationService;
import com.bookcrossing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ChatController(ChatService chatService,
                          UserService userService,
                          SimpMessagingTemplate messagingTemplate,
                          NotificationService notificationService) {
        this.chatService       = chatService;
        this.userService       = userService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    @GetMapping("/messages")
    public String messagesPage(Model model, Principal principal,
                               @RequestParam(required = false) Long partnerId) {
        User currentUser = userService.findByUsername(principal.getName());

        // 1. Инициализируем флаги значениями по умолчанию (false)
        boolean isChatBlocked = false;
        boolean isMuted = false;
        boolean blockedByPartner = false;

        if (partnerId != null && !currentUser.getId().equals(partnerId)) {
            User partner = userService.findById(partnerId);
            chatService.markMessagesAsRead(currentUser, partner);

            model.addAttribute("activePartner", partner);
            model.addAttribute("history", chatService.getChatHistory(currentUser.getId(), partnerId));

            // 2. Переопределяем флаги, если выбран активный собеседник
            isChatBlocked = currentUser.hasChatBlocked(partnerId);
            isMuted = currentUser.hasMuted(partnerId);
            blockedByPartner = partner.hasChatBlocked(currentUser.getId());
        }

        // 3. Добавляем флаги в модель в ЛЮБОМ случае, чтобы Thymeleaf не падал на null
        model.addAttribute("isChatBlocked", isChatBlocked);
        model.addAttribute("isMuted", isMuted);
        model.addAttribute("blockedByPartner", blockedByPartner);

        List<ConversationDTO> conversations = chatService.getUserConversations(currentUser);
        model.addAttribute("conversations", conversations);
        model.addAttribute("currentUser", currentUser);

        return "messages";
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, String> payload, Principal principal) {
        String content     = payload.get("content");
        Long   recipientId = Long.parseLong(payload.get("recipientId"));
        User   sender      = userService.findByUsername(principal.getName());

        Message savedMsg;
        try {
            savedMsg = chatService.saveMessage(sender.getId(), recipientId, content);
        } catch (IllegalStateException e) {
            // Получатель заблокировал отправителя — молча игнорируем
            return;
        }

        messagingTemplate.convertAndSendToUser(
                savedMsg.getRecipient().getUsername(), "/queue/messages", savedMsg);
        messagingTemplate.convertAndSendToUser(
                sender.getUsername(), "/queue/messages", savedMsg);

        // Уведомление не отправляем, если получатель замьютил отправителя (#4)
        User recipient = savedMsg.getRecipient();
        if (!recipient.hasMuted(sender.getId())) {
            String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            notificationService.sendNotification(
                    recipient.getUsername(),
                    "Новое сообщение от " + sender.getUsername(),
                    preview,
                    "/messages?partnerId=" + sender.getId()
            );
        }
    }

    // ── Поиск в чате (#4) ──────────────────────────────────────────────────

    @GetMapping("/api/chat/search")
    @ResponseBody
    public ResponseEntity<List<Message>> searchChat(
            @RequestParam Long partnerId,
            @RequestParam String query,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        User partner     = userService.findById(partnerId);
        List<Message> results = chatService.searchInChat(currentUser, partner, query);
        return ResponseEntity.ok(results);
    }

    // ── Удаление переписки (#4) ─────────────────────────────────────────────

    @PostMapping("/chat/delete")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteConversation(
            @RequestParam Long partnerId,
            @RequestParam(defaultValue = "self") String mode,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        User partner     = userService.findById(partnerId);

        if ("both".equals(mode)) {
            chatService.deleteConversationBoth(currentUser, partner);
        } else {
            chatService.deleteConversationSelf(currentUser, partner);
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ── Блокировка в чате (#4) ─────────────────────────────────────────────

    @PostMapping("/chat/block")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> blockUser(
            @RequestParam Long targetId,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        chatService.blockChatUser(currentUser, targetId);
        return ResponseEntity.ok(Map.of("blocked", true, "targetId", targetId));
    }

    @PostMapping("/chat/unblock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unblockUser(
            @RequestParam Long targetId,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        chatService.unblockChatUser(currentUser, targetId);
        return ResponseEntity.ok(Map.of("blocked", false, "targetId", targetId));
    }

    // ── Мут (#4) ───────────────────────────────────────────────────────────

    @PostMapping("/chat/mute")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> muteUser(
            @RequestParam Long targetId,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        chatService.muteUser(currentUser, targetId);
        return ResponseEntity.ok(Map.of("muted", true, "targetId", targetId));
    }

    @PostMapping("/chat/unmute")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unmuteUser(
            @RequestParam Long targetId,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        chatService.unmuteUser(currentUser, targetId);
        return ResponseEntity.ok(Map.of("muted", false, "targetId", targetId));
    }
}