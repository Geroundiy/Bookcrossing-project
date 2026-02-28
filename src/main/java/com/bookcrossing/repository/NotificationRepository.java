package com.bookcrossing.repository;

import com.bookcrossing.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Все уведомления пользователя (новые сверху)
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);

    // Количество непрочитанных
    long countByUsernameAndReadFalse(String username);

    // Пометить все как прочитанные
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.username = :username AND n.read = false")
    void markAllReadByUsername(@Param("username") String username);
}