package com.bookcrossing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Имя пользователя-получателя
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    // Ссылка для перехода при клике
    private String link;

    private LocalDateTime createdAt;

    // Прочитано или нет
    @Column(name = "is_read")
    private boolean read = false;

    // ── Getters & Setters ──────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}