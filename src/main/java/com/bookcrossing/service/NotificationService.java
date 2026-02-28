package com.bookcrossing.service;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.Notification;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.NotificationRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public NotificationService(JavaMailSender mailSender,
                               SimpMessagingTemplate messagingTemplate,
                               NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Основной метод: сохраняет уведомление в БД и отправляет через WebSocket.
     */
    @Transactional
    public void sendNotification(String username, String title, String body, String link) {
        // 1. Сохраняем в БД
        Notification notification = new Notification();
        notification.setUsername(username);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        // 2. Отправляем WebSocket-событие (toast + обновление бейджа)
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                Map.of(
                        "id",    saved.getId(),
                        "title", title,
                        "body",  body,
                        "link",  link
                )
        );
    }

    /**
     * Получить все уведомления пользователя.
     */
    public List<Notification> getNotifications(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    /**
     * Количество непрочитанных.
     */
    public long getUnreadCount(String username) {
        return notificationRepository.countByUsernameAndReadFalse(username);
    }

    /**
     * Пометить все уведомления как прочитанные.
     */
    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllReadByUsername(username);
    }

    /**
     * Пометить одно уведомление как прочитанное.
     */
    @Transactional
    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    /**
     * Email-уведомление при бронировании книги.
     */
    public void sendBookingEmail(User owner, User reader, Book book) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(owner.getEmail());
            message.setSubject("Вашу книгу забронировали!");
            message.setText(String.format(
                    "Здравствуйте, %s!\n\nПользователь %s хочет забрать книгу «%s».\n" +
                            "Свяжитесь с ним в мессенджере приложения.",
                    owner.getUsername(), reader.getUsername(), book.getTitle()
            ));
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email не отправлен: " + e.getMessage());
        }
    }
}