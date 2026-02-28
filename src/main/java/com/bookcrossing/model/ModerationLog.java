package com.bookcrossing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_logs")
public class ModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Кто совершил действие (модератор/админ)
    @ManyToOne
    @JoinColumn(name = "moderator_id")
    private User moderator;

    // Тип действия
    @Enumerated(EnumType.STRING)
    private ActionType action;

    // Описание / причина
    @Column(columnDefinition = "TEXT")
    private String reason;

    // На кого направлено действие (может быть null для действий с книгами)
    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    // Какая книга была затронута (может быть null)
    private Long bookId;
    private String bookTitle;

    private LocalDateTime createdAt;

    public enum ActionType {
        BOOK_DELETED("Удаление книги"),
        USER_BLOCKED("Блокировка пользователя"),
        USER_UNBLOCKED("Разблокировка пользователя"),
        ROLE_CHANGED("Изменение роли"),
        USER_DELETED("Удаление аккаунта"),
        COMPLAINT_ACCEPTED("Жалоба принята"),
        COMPLAINT_REJECTED("Жалоба отклонена");

        private final String displayName;
        ActionType(String n) { this.displayName = n; }
        public String getDisplayName() { return displayName; }
    }

    // ── Getters/Setters ──────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getModerator() { return moderator; }
    public void setModerator(User moderator) { this.moderator = moderator; }

    public ActionType getAction() { return action; }
    public void setAction(ActionType action) { this.action = action; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public User getTargetUser() { return targetUser; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}