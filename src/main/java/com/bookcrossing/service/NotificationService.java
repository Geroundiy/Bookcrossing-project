package com.bookcrossing.service;

import com.bookcrossing.model.Book;
import com.bookcrossing.model.Notification;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

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
     * Сохраняет уведомление в БД и отправляет через WebSocket.
     *
     * ИСПРАВЛЕНИЕ (#6): catch (Exception ignored) заменён на
     * log.warn() — ошибки брокера теперь видны в логах, но не
     * откатывают транзакцию вызывающего метода.
     */
    @Transactional
    public void sendNotification(String username, String title, String body, String link) {
        // 1. Всегда сохраняем в БД — независимо от состояния WebSocket
        Notification notification = new Notification();
        notification.setUsername(username);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        // 2. Отправляем WebSocket-событие
        try {
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
        } catch (Exception ex) {
            // Уведомление уже в БД — пользователь увидит его при следующем
            // открытии /notifications. Логируем предупреждение (не error),
            // так как отключённый WebSocket — штатная ситуация.
            log.warn("Не удалось отправить WebSocket-уведомление пользователю '{}': {}",
                    username, ex.getMessage());
        }
    }

    /**
     * Отправляет email-уведомление владельцу книги о бронировании.
     * Ошибка SMTP не должна ломать основной флоу — логируем предупреждение.
     */
    public void sendBookingEmail(User owner, User reader, Book book) {
        if (owner.getEmail() == null || owner.getEmail().isBlank()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(owner.getEmail());
            message.setSubject("Вашу книгу забронировали — BookCrossing");
            message.setText(
                    "Здравствуйте, " + owner.getUsername() + "!\n\n" +
                            "Пользователь @" + reader.getUsername() +
                            " хочет забронировать вашу книгу «" + book.getTitle() + "».\n\n" +
                            "Перейдите в раздел «Мои книги», чтобы одобрить или отклонить запрос.\n\n" +
                            "— Команда BookCrossing"
            );
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Не удалось отправить email владельцу '{}': {}",
                    owner.getUsername(), ex.getMessage());
        }
    }

    public List<Notification> getNotifications(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countByUsernameAndReadFalse(username);
    }

    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllReadByUsername(username);
    }

    @Transactional
    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}