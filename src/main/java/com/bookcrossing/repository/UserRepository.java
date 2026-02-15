package com.bookcrossing.repository;

import com.bookcrossing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // Новый метод: искать или по нику, или по почте
    Optional<User> findByUsernameOrEmail(String username, String email);
}