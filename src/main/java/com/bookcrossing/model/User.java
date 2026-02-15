package com.bookcrossing.model;

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
    private String password;

    // Поля профиля
    private String fullName; // Имя/Никнейм

    @Column(columnDefinition = "TEXT")
    private String avatarUrl; // Аватар (Base64 или ссылка)

    private LocalDate birthDate; // Дата рождения
    private String city;
    private String country;
    private String gender; // Пол

    @Column(columnDefinition = "TEXT")
    private String aboutMe; // О себе

    private String socialLinks; // Ссылки на соц.сети (через запятую или JSON)

    private String favoriteGenres; // Любимые жанры (строка)

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
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