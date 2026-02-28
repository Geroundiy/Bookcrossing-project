package com.bookcrossing.repository;

import com.bookcrossing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    // Поиск по имени пользователя (для поиска в панели)
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%'))" +
            " OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<User> searchUsers(@Param("q") String query);

    // Все пользователи с конкретной ролью
    List<User> findByRole(User.UserRole role);
}