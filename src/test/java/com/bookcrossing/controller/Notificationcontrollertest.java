package com.bookcrossing.controller;

import com.bookcrossing.model.Notification;
import com.bookcrossing.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

    @Mock NotificationService notificationService;
    @InjectMocks NotificationController notificationController;

    private Principal principal;
    private Model model;

    @BeforeEach
    void setUp() {
        principal = () -> "alice";
        model     = mock(Model.class);
    }

    @Test
    @DisplayName("GET /notifications — загружает уведомления и помечает прочитанными")
    void notificationsPage_loadsAndMarksRead() {
        Notification n = new Notification();
        when(notificationService.getNotifications("alice")).thenReturn(List.of(n));

        String view = notificationController.notificationsPage(model, principal);

        assertThat(view).isEqualTo("notifications");
        verify(model).addAttribute(eq("notifications"), eq(List.of(n)));
        verify(notificationService).markAllRead("alice");
    }

    @Test
    @DisplayName("GET /notifications — пустой список уведомлений")
    void notificationsPage_empty() {
        when(notificationService.getNotifications("alice")).thenReturn(List.of());

        String view = notificationController.notificationsPage(model, principal);

        assertThat(view).isEqualTo("notifications");
        verify(notificationService).markAllRead("alice");
    }

    @Test
    @DisplayName("POST /notifications/{id}/read — помечает одно уведомление прочитанным")
    void markRead_callsService() {
        String result = notificationController.markRead(42L, principal);

        assertThat(result).isEqualTo("ok");
        verify(notificationService).markRead(42L);
    }
}