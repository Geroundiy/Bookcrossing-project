package com.bookcrossing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Кто подал жалобу
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    // На кого жалоба (может быть null если жалоба на книгу)
    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    // На какую книгу (может быть null)
    private Long targetBookId;
    private String targetBookTitle;

    @Enumerated(EnumType.STRING)
    private ComplaintType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status = ComplaintStatus.PENDING;

    // Комментарий модератора при обработке
    @Column(columnDefinition = "TEXT")
    private String moderatorComment;

    @ManyToOne
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    // ── Enum-ы ────────────────────────────────────────────────

    public enum ComplaintType {
        SPAM("Спам"),
        INAPPROPRIATE_CONTENT("Неприемлемый контент"),
        FAKE("Фейковое объявление"),
        SCAM("Мошенничество"),
        OTHER("Другое");

        private final String displayName;
        ComplaintType(String n) { this.displayName = n; }
        public String getDisplayName() { return displayName; }
    }

    public enum ComplaintStatus {
        PENDING("На рассмотрении"),
        ACCEPTED("Принята"),
        REJECTED("Отклонена");

        private final String displayName;
        ComplaintStatus(String n) { this.displayName = n; }
        public String getDisplayName() { return displayName; }
    }

    // ── Getters/Setters ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public User getTargetUser() { return targetUser; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }

    public Long getTargetBookId() { return targetBookId; }
    public void setTargetBookId(Long targetBookId) { this.targetBookId = targetBookId; }

    public String getTargetBookTitle() { return targetBookTitle; }
    public void setTargetBookTitle(String targetBookTitle) { this.targetBookTitle = targetBookTitle; }

    public ComplaintType getType() { return type; }
    public void setType(ComplaintType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getModeratorComment() { return moderatorComment; }
    public void setModeratorComment(String moderatorComment) { this.moderatorComment = moderatorComment; }

    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}