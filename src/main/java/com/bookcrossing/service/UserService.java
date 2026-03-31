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

    /**
     * Единая константа минимальной длины пароля.
     * Исправление #7: раньше было 8 на регистрации и 6 в профиле.
     * Теперь используется одна константа везде.
     */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * Регулярное выражение для проверки символов пароля.
     * Только латиница, цифры и спецсимволы — без кириллицы.
     */
    public static final String PASSWORD_PATTERN = "^[a-zA-Z0-9!@#$%^&*()_+\\-=]+$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден id=" + id));
    }

    public List<User> findAll() { return userRepository.findAll(); }

    public List<User> searchUsers(String q) {
        return (q == null || q.isBlank()) ? userRepository.findAll()
                : userRepository.searchUsers(q);
    }

    /**
     * Регистрация нового пользователя.
     * Исправление #8: суперадминистратор определяется флагом superAdmin,
     * а не магической строкой "admin".
     * Для первого запуска: если в БД ещё нет ни одного пользователя —
     * назначаем роль ADMIN и выставляем superAdmin=true.
     */
    @Transactional
    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Первый зарегистрированный пользователь становится суперадмином
        if (userRepository.count() == 0) {
            user.setRole(User.UserRole.ADMIN);
            user.setSuperAdmin(true);
        }

        userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User current, User data, MultipartFile avatar) {
        current.setFullName(data.getFullName());
        current.setEmail(data.getEmail());
        current.setCity(data.getCity());
        current.setCountry(data.getCountry());
        current.setGender(data.getGender());
        current.setBirthDate(data.getBirthDate());
        current.setAboutMe(data.getAboutMe());
        current.setSocialLinks(data.getSocialLinks());
        current.setFavoriteGenres(data.getFavoriteGenres());
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String b64 = Base64.getEncoder().encodeToString(avatar.getBytes());
                current.setAvatarUrl("data:" + avatar.getContentType() + ";base64," + b64);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка загрузки аватара", e);
            }
        }
        userRepository.save(current);
    }

    // ── Блокировка ─────────────────────────────────────────

    @Transactional
    public void blockUser(Long id, String reason, Integer days) {
        User u = findById(id);
        u.setBlocked(true);
        u.setBlockReason(reason);
        u.setBlockUntil(days != null ? LocalDateTime.now().plusDays(days) : null);
        userRepository.save(u);
    }

    @Transactional
    public void unblockUser(Long id) {
        User u = findById(id);
        u.setBlocked(false);
        u.setBlockReason(null);
        u.setBlockUntil(null);
        userRepository.save(u);
    }

    @Transactional
    public void changeRole(Long id, User.UserRole role) {
        User u = findById(id);
        u.setRole(role);
        userRepository.save(u);
    }

    // ── Удаление пользователя ──────────────────────────────

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        userRepository.deleteMessagesByUser(id);
        userRepository.deleteNotificationsByUsername(user.getUsername());
        userRepository.deleteReviewsByUser(id);
        userRepository.deleteReviewsOfUserBooks(id);
        userRepository.deleteBooksByUser(id);
        userRepository.deleteReviewsTargetingUser(id);
        userRepository.deleteById(id);
    }

    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }

    /**
     * Смена пароля.
     * Исправление #7: валидация использует единые константы MIN_PASSWORD_LENGTH
     * и PASSWORD_PATTERN — одни и те же для регистрации и профиля.
     */
    @Transactional
    public boolean changePassword(User user, String currentPass, String newPass,
                                  org.springframework.ui.Model model) {
        if (currentPass == null || !passwordEncoder.matches(currentPass, user.getPassword())) {
            model.addAttribute("error", "Текущий пароль введён неверно.");
            return false;
        }
        if (newPass == null || newPass.length() < MIN_PASSWORD_LENGTH) {
            model.addAttribute("error",
                    "Новый пароль должен содержать минимум " + MIN_PASSWORD_LENGTH + " символов.");
            return false;
        }
        if (!newPass.matches(PASSWORD_PATTERN)) {
            model.addAttribute("error",
                    "Пароль может содержать только латинские буквы, цифры и спецсимволы.");
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
        return true;
    }
}