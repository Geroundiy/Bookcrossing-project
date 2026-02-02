package com.bookcrossing.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank; // Важно!
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название книги не может быть пустым")
    private String title;

    @NotBlank(message = "Автор обязателен")
    private String author;

    @Size(max = 1000, message = "Описание слишком длинное")
    private String description;

    private String imageUrl; // URL картинки

    @Enumerated(EnumType.STRING)
    private BookStatus status = BookStatus.FREE;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;

    public enum BookStatus {
        FREE("Свободна"),
        BUSY("Занята");

        private final String displayValue;
        BookStatus(String displayValue) { this.displayValue = displayValue; }
        public String getDisplayValue() { return displayValue; }
    }

    // Метод-помощник для Frontend (если нет картинки - возвращает заглушку)
    public String getImageDisplay() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "https://via.placeholder.com/150?text=No+Cover"; // Заглушка
        }
        return imageUrl;
    }
}