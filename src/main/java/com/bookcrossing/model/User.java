package com.bookcrossing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Data
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

    // ── Роль пользователя ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    // ── Блокировка ───────────────────────────────────────────
    @Column(name = "is_blocked")
    private boolean blocked = false;

    @Column(columnDefinition = "TEXT")
    private String blockReason;

    private LocalDateTime blockUntil;  // null = навсегда

    // ── Книги пользователя ───────────────────────────────────
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Book> books;

    // ── Вспомогательные методы ───────────────────────────────
    public Integer getAge() {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String getAvatarDisplay() {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return "/images/usual_avatar.png";
        }
        return avatarUrl;
    }

    /** Проверяет, является ли блокировка ещё активной */
    public boolean isCurrentlyBlocked() {
        if (!blocked) return false;
        if (blockUntil == null) return true;               // бессрочная
        return LocalDateTime.now().isBefore(blockUntil);   // временная
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isModerator() {
        return role == UserRole.MODERATOR || role == UserRole.ADMIN;
    }

    // ── Enum ролей ───────────────────────────────────────────
    public enum UserRole {
        USER, MODERATOR, ADMIN
    }
}