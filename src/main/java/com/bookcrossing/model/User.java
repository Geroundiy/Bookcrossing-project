package com.bookcrossing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // Логин

    private String email; // Для валидации по US 2.1

    private String password;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Book> books;
}