package com.bookcrossing.service;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {
    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(JavaMailSender mailSender, SimpMessagingTemplate messagingTemplate) {
        this.mailSender = mailSender;
        this.messagingTemplate = messagingTemplate;
    }

    // Отправка Email при бронировании
    public void sendBookingEmail(User owner, User reader, Book book) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(owner.getEmail());
        message.setSubject("Вашу книгу забронировали!");
        message.setText(String.format(
                "Здравствуйте, %s!\n\nПользователь %s забронировал вашу книгу '%s'.\nСвяжитесь с ним в мессенджере приложения.",
                owner.getUsername(), reader.getUsername(), book.getTitle()
        ));
        mailSender.send(message);
    }

    // Отправка Push (WebSocket) уведомления
    public void sendNotification(String username, String title, String message, String link) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                Map.of("title", title, "body", message, "link", link)
        );
    }
}