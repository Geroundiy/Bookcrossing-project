package com.bookcrossing.model;

import com.fasterxml.jackson.annotation.JsonIgnore; // <--- ВАЖНЫЙ ИМПОРТ
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
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

    @JsonIgnore // <--- СКРЫВАЕМ ПАРОЛЬ (Безопасность + не ломает JSON)
    private String password;

    // Поля профиля
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

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonIgnore // <--- ИСПРАВЛЕНИЕ ОШИБКИ LazyInitializationException
    private List<Book> books;

    // Метод для вычисления возраста
    public Integer getAge() {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String getAvatarDisplay() {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return "https://via.placeholder.com/150?text=User";
        }
        return avatarUrl;
    }
}