package com.bookcrossing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String email;

    @JsonIgnore
    private String password;

    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    private LocalDate birthDate;
    private String city;
    private String country;
    private String gender;

    @Column(columnDefinition = "TEXT")
    private String aboutMe;

    private String socialLinks;
    private String favoriteGenres;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "is_blocked")
    private Boolean blocked = false;

    @Column(columnDefinition = "TEXT")
    private String blockReason;

    private LocalDateTime blockUntil;

    private LocalDateTime registeredAt;

    @Column(name = "super_admin", columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    private boolean superAdmin = false;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Book> books;

    /**
     * Пользователи, заблокированные текущим пользователем в чате (#4).
     * Хранятся как ID, чтобы избежать циклических ссылок.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_chat_blocks",
            joinColumns = @JoinColumn(name = "blocker_id"))
    @Column(name = "blocked_user_id")
    private Set<Long> chatBlockedUsers = new HashSet<>();

    /**
     * Пользователи, которых текущий пользователь отправил в мут (#4).
     * Уведомления от них приходить не будут.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_chat_mutes",
            joinColumns = @JoinColumn(name = "muter_id"))
    @Column(name = "muted_user_id")
    private Set<Long> mutedUsers = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
        if (role       == null)  role          = UserRole.USER;
        if (blocked    == null)  blocked       = false;
        if (chatBlockedUsers == null) chatBlockedUsers = new HashSet<>();
        if (mutedUsers       == null) mutedUsers       = new HashSet<>();
    }

    // ── Вспомогательные методы ─────────────────────────────

    public Integer getAge() {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String getAvatarDisplay() {
        if (avatarUrl == null || avatarUrl.isEmpty()) return "/images/usual_avatar.png";
        return avatarUrl;
    }

    public boolean isCurrentlyBlocked() {
        if (blocked == null || !blocked) return false;
        if (blockUntil == null) return true;
        return LocalDateTime.now().isBefore(blockUntil);
    }

    public boolean isAdmin()     { return role == UserRole.ADMIN; }
    public boolean isModerator() { return role == UserRole.MODERATOR || role == UserRole.ADMIN; }
    public boolean isSuperAdmin() { return superAdmin; }

    /** Проверить, заблокировал ли текущий пользователь другого в чате */
    public boolean hasChatBlocked(Long userId) {
        return chatBlockedUsers != null && chatBlockedUsers.contains(userId);
    }

    /** Проверить, замьютил ли текущий пользователь другого */
    public boolean hasMuted(Long userId) {
        return mutedUsers != null && mutedUsers.contains(userId);
    }

    public enum UserRole { USER, MODERATOR, ADMIN }
}