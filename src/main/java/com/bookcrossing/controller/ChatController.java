package com.bookcrossing.controller;

import com.bookcrossing.dto.ConversationDTO;
import com.bookcrossing.model.Message;
import com.bookcrossing.model.User;
import com.bookcrossing.service.ChatService;
import com.bookcrossing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    // Страница со списком сообщений
    @GetMapping("/messages")
    public String messagesPage(Model model, Principal principal,
                               @RequestParam(required = false) Long partnerId) {
        User currentUser = userService.findByUsername(principal.getName());
        List<ConversationDTO> conversations = chatService.getUserConversations(currentUser);

        model.addAttribute("conversations", conversations);
        model.addAttribute("currentUser", currentUser);

        if (partnerId != null) {
            User partner = userService.findById(partnerId); // метод нужно добавить в UserService
            model.addAttribute("activePartner", partner);
            chatService.markMessagesAsRead(currentUser, partner);
            model.addAttribute("history", chatService.getChatHistory(currentUser.getId(), partnerId));
        }

        return "messages";
    }

    // WebSocket Endpoint: Получение сообщения от клиента
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, String> payload, Principal principal) {
        String content = payload.get("content");
        Long recipientId = Long.parseLong(payload.get("recipientId"));
        User sender = userService.findByUsername(principal.getName());

        Message savedMsg = chatService.saveMessage(sender.getId(), recipientId, content);

        // Отправляем сообщение получателю в его личную очередь
        messagingTemplate.convertAndSendToUser(
                savedMsg.getRecipient().getUsername(),
                "/queue/messages",
                savedMsg
        );

        // Отправляем обратно отправителю (чтобы он увидел свое сообщение сразу)
        messagingTemplate.convertAndSendToUser(
                sender.getUsername(),
                "/queue/messages",
                savedMsg
        );
    }
}