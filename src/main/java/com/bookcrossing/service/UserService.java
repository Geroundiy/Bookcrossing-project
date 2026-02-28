package com.bookcrossing.service;

import com.bookcrossing.model.User;
import com.bookcrossing.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: id=" + id));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) return userRepository.findAll();
        return userRepository.searchUsers(query);
    }

    @Transactional
    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Первый зарегистрированный с ником "admin" сразу получает роль ADMIN
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            user.setRole(User.UserRole.ADMIN);
        }
        userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User currentUser, User updatedData, MultipartFile avatarFile) {
        currentUser.setFullName(updatedData.getFullName());
        currentUser.setEmail(updatedData.getEmail());
        currentUser.setCity(updatedData.getCity());
        currentUser.setCountry(updatedData.getCountry());
        currentUser.setGender(updatedData.getGender());
        currentUser.setBirthDate(updatedData.getBirthDate());
        currentUser.setAboutMe(updatedData.getAboutMe());
        currentUser.setSocialLinks(updatedData.getSocialLinks());
        currentUser.setFavoriteGenres(updatedData.getFavoriteGenres());

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String base64  = Base64.getEncoder().encodeToString(avatarFile.getBytes());
                currentUser.setAvatarUrl("data:" + avatarFile.getContentType() + ";base64," + base64);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка загрузки аватара", e);
            }
        }
        userRepository.save(currentUser);
    }

    // ── Блокировка ───────────────────────────────────────────

    @Transactional
    public void blockUser(Long userId, String reason, Integer daysOrNull) {
        User user = findById(userId);
        user.setBlocked(true);
        user.setBlockReason(reason);
        user.setBlockUntil(daysOrNull != null
                ? LocalDateTime.now().plusDays(daysOrNull)
                : null);   // null = навсегда
        userRepository.save(user);
    }

    @Transactional
    public void unblockUser(Long userId) {
        User user = findById(userId);
        user.setBlocked(false);
        user.setBlockReason(null);
        user.setBlockUntil(null);
        userRepository.save(user);
    }

    // ── Изменение роли ───────────────────────────────────────

    @Transactional
    public void changeRole(Long userId, User.UserRole newRole) {
        User user = findById(userId);
        user.setRole(newRole);
        userRepository.save(user);
    }

    // ── Удаление аккаунта ────────────────────────────────────

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}