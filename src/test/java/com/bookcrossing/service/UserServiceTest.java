package com.bookcrossing.service;

import com.bookcrossing.model.User;
import com.bookcrossing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("hashed");
        user.setEmail("test@example.com");
        user.setRole(User.UserRole.USER);
        user.setBlocked(false);
    }

    // ─── findByUsername ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsername — найден — возвращает пользователя")
    void findByUsername_found() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        assertThat(userService.findByUsername("testuser")).isEqualTo(user);
    }

    @Test
    @DisplayName("findByUsername — не найден — бросает RuntimeException")
    void findByUsername_notFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ghost");
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — найден — возвращает пользователя")
    void findById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThat(userService.findById(1L)).isEqualTo(user);
    }

    @Test
    @DisplayName("findById — не найден — бросает RuntimeException")
    void findById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── searchUsers ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchUsers — null query — возвращает всех")
    void searchUsers_nullQuery_returnsAll() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        assertThat(userService.searchUsers(null)).hasSize(1);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("searchUsers — blank query — возвращает всех")
    void searchUsers_blankQuery_returnsAll() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        assertThat(userService.searchUsers(" ")).hasSize(1);
    }

    @Test
    @DisplayName("searchUsers — с запросом — делегирует в repository")
    void searchUsers_withQuery() {
        when(userRepository.searchUsers("test")).thenReturn(List.of(user));
        assertThat(userService.searchUsers("test")).containsExactly(user);
        verify(userRepository).searchUsers("test");
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Хэширует пароль и сохраняет")
        void encodesPasswordAndSaves() {
            user.setPassword("plaintext");
            // Не первый пользователь — count > 0
            when(userRepository.count()).thenReturn(5L);
            when(passwordEncoder.encode("plaintext")).thenReturn("hashed_pw");

            userService.register(user);

            assertThat(user.getPassword()).isEqualTo("hashed_pw");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Первый зарегистрированный пользователь — получает ADMIN и superAdmin=true")
        void firstUser_getsAdminAndSuperAdmin() {
            user.setPassword("pw");
            // count() == 0 → первый пользователь
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode("pw")).thenReturn("h");

            userService.register(user);

            assertThat(user.getRole()).isEqualTo(User.UserRole.ADMIN);
            assertThat(user.isSuperAdmin()).isTrue();
        }

        @Test
        @DisplayName("Обычный пользователь (не первый) — роль USER, superAdmin=false")
        void normalUser_roleUnchanged() {
            user.setUsername("ivan");
            user.setRole(User.UserRole.USER);
            user.setPassword("pw");
            // count() > 0 → не первый пользователь
            when(userRepository.count()).thenReturn(1L);
            when(passwordEncoder.encode("pw")).thenReturn("h");

            userService.register(user);

            assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
            assertThat(user.isSuperAdmin()).isFalse();
        }

        @Test
        @DisplayName("Имя пользователя 'admin' не влияет на роль (теперь важен только count)")
        void adminUsername_noLongerGrantsRoleByName() {
            // Имя "admin" само по себе больше не делает пользователя ADMIN
            // — только если он первый (count==0). Здесь count=5, значит USER.
            user.setUsername("admin");
            user.setRole(User.UserRole.USER);
            user.setPassword("pw");
            when(userRepository.count()).thenReturn(5L);
            when(passwordEncoder.encode("pw")).thenReturn("h");

            userService.register(user);

            assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
        }
    }

    // ─── blockUser / unblockUser ──────────────────────────────────────────────

    @Test
    @DisplayName("blockUser — срочная блокировка — blockUntil задан")
    void blockUser_withDays_setsBlockUntil() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.blockUser(1L, "spam", 7);

        assertThat(user.getBlocked()).isTrue();
        assertThat(user.getBlockReason()).isEqualTo("spam");
        assertThat(user.getBlockUntil()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("blockUser — бессрочно — blockUntil null")
    void blockUser_noDays_permanentBlock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.blockUser(1L, "abuse", null);

        assertThat(user.getBlocked()).isTrue();
        assertThat(user.getBlockUntil()).isNull();
    }

    @Test
    @DisplayName("unblockUser — снимает блокировку")
    void unblockUser_clearsBlock() {
        user.setBlocked(true);
        user.setBlockReason("spam");
        user.setBlockUntil(LocalDateTime.now().plusDays(7));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.unblockUser(1L);

        assertThat(user.getBlocked()).isFalse();
        assertThat(user.getBlockReason()).isNull();
        assertThat(user.getBlockUntil()).isNull();
        verify(userRepository).save(user);
    }

    // ─── changeRole ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("changeRole — меняет роль и сохраняет")
    void changeRole_updatesAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.changeRole(1L, User.UserRole.MODERATOR);

        assertThat(user.getRole()).isEqualTo(User.UserRole.MODERATOR);
        verify(userRepository).save(user);
    }

    // ─── changePassword ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("Правильный пароль, валидный новый — возвращает true")
        void correct_returnsTrue() {
            // "ValidPass1" — длина >= 8, только латиница/цифры
            when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
            when(passwordEncoder.encode("ValidPass1")).thenReturn("new_hashed");
            Model model = mock(Model.class);

            boolean result = userService.changePassword(user, "old", "ValidPass1", model);

            assertThat(result).isTrue();
            assertThat(user.getPassword()).isEqualTo("new_hashed");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Неверный текущий пароль — возвращает false")
        void wrongCurrentPass_returnsFalse() {
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
            Model model = mock(Model.class);

            boolean result = userService.changePassword(user, "wrong", "ValidPass1", model);

            assertThat(result).isFalse();
            verify(model).addAttribute(eq("error"), anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("null currentPass — возвращает false")
        void nullCurrentPass_returnsFalse() {
            Model model = mock(Model.class);

            boolean result = userService.changePassword(user, null, "ValidPass1", model);

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Новый пароль < 8 символов — возвращает false")
        void newPasswordTooShort_returnsFalse() {
            when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
            Model model = mock(Model.class);

            boolean result = userService.changePassword(user, "old", "short", model);

            assertThat(result).isFalse();
            verify(model).addAttribute(eq("error"), contains(String.valueOf(UserService.MIN_PASSWORD_LENGTH)));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Новый пароль с кириллицей — возвращает false")
        void newPasswordCyrillic_returnsFalse() {
            when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
            Model model = mock(Model.class);

            boolean result = userService.changePassword(user, "old", "КириллицаПарольДлинный", model);

            assertThat(result).isFalse();
            verify(model).addAttribute(eq("error"), anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("null newPass — возвращает false")
        void nullNewPass_returnsFalse() {
            when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
            Model model = mock(Model.class);

            boolean result = userService.changePassword(user, "old", null, model);

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }
    }

    // ─── existsByUsername ─────────────────────────────────────────────────────

    @Test
    @DisplayName("existsByUsername — существует — возвращает true")
    void existsByUsername_exists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        assertThat(userService.existsByUsername("testuser")).isTrue();
    }

    @Test
    @DisplayName("existsByUsername — не существует — возвращает false")
    void existsByUsername_notExists() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThat(userService.existsByUsername("ghost")).isFalse();
    }

    // ─── saveUser ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveUser — сохраняет через repository")
    void saveUser_callsSave() {
        userService.saveUser(user);
        verify(userRepository).save(user);
    }
}