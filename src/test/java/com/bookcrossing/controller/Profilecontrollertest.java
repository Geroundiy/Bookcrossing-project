package com.bookcrossing.controller;

import com.bookcrossing.model.User;
import com.bookcrossing.service.AchievementService;
import com.bookcrossing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController")
class ProfileControllerTest {

    @Mock UserService userService;
    @Mock AchievementService achievementService;
    @InjectMocks ProfileController profileController;

    private User me;
    private Principal principal;
    private Model model;
    private RedirectAttributes ra;

    @BeforeEach
    void setUp() {
        me = new User(); me.setId(1L); me.setUsername("alice");
        principal = () -> "alice";
        model     = mock(Model.class);
        ra        = mock(RedirectAttributes.class);
    }

    // ─── GET /profile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /profile — загружает профиль и проверяет достижения")
    void myProfile_loadsAndAwards() {
        when(userService.findByUsername("alice")).thenReturn(me);

        String view = profileController.myProfile(principal, model);

        assertThat(view).isEqualTo("profile");
        verify(model).addAttribute("profileUser", me);
        verify(model).addAttribute("isOwnProfile", true);
        verify(achievementService).checkAndAward(me);
    }

    // ─── GET /users/{username} ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users/{username} — чужой профиль, isOwnProfile=false")
    void userProfile_otherUser_notOwn() {
        User other = new User(); other.setUsername("bob");
        when(userService.findByUsername("bob")).thenReturn(other);

        String view = profileController.userProfile("bob", principal, model);

        assertThat(view).isEqualTo("profile");
        verify(model).addAttribute("isOwnProfile", false);
    }

    @Test
    @DisplayName("GET /users/{username} — собственный профиль через URL, isOwnProfile=true")
    void userProfile_ownUsername_isOwn() {
        when(userService.findByUsername("alice")).thenReturn(me);

        profileController.userProfile("alice", principal, model);

        verify(model).addAttribute("isOwnProfile", true);
    }

    @Test
    @DisplayName("GET /users/{username} — principal=null, isOwnProfile=false")
    void userProfile_nullPrincipal_notOwn() {
        User other = new User(); other.setUsername("bob");
        when(userService.findByUsername("bob")).thenReturn(other);

        profileController.userProfile("bob", null, model);

        verify(model).addAttribute("isOwnProfile", false);
    }

    // ─── GET /profile/edit ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /profile/edit — добавляет user и minPasswordLength")
    void editForm_populatesModel() {
        when(userService.findByUsername("alice")).thenReturn(me);

        String view = profileController.editForm(principal, model);

        assertThat(view).isEqualTo("profile-edit");
        verify(model).addAttribute("user", me);
        verify(model).addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
    }

    // ─── POST /profile/edit ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /profile/edit")
    class EditSave {

        @Test
        @DisplayName("Некорректный email — возвращает profile-edit с ошибкой")
        void invalidEmail_returnsError() {
            when(userService.findByUsername("alice")).thenReturn(me);

            String view = callEditSave("bad-email", null, null, null, null, null, null);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains("email"));
        }

        @Test
        @DisplayName("О себе > 1000 символов — возвращает ошибку")
        void aboutMeTooLong_returnsError() {
            when(userService.findByUsername("alice")).thenReturn(me);
            String longText = "a".repeat(1001);

            String view = callEditSaveAbout(longText);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains("1000"));
        }

        @Test
        @DisplayName("Новый пароль < 8 символов — возвращает ошибку")
        void newPasswordTooShort_returnsError() {
            when(userService.findByUsername("alice")).thenReturn(me);

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, "oldPass", "short", "short",
                    null, ra, model);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains(String.valueOf(UserService.MIN_PASSWORD_LENGTH)));
        }

        @Test
        @DisplayName("Новый пароль содержит кириллицу — возвращает ошибку")
        void newPasswordCyrillic_returnsError() {
            when(userService.findByUsername("alice")).thenReturn(me);

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, "oldPass", "КириллицаПарольДлинный", "КириллицаПарольДлинный",
                    null, ra, model);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains("латинские"));
        }

        @Test
        @DisplayName("Пароли не совпадают — возвращает ошибку")
        void passwordsMismatch_returnsError() {
            when(userService.findByUsername("alice")).thenReturn(me);

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, "oldPass", "NewPass123", "DifferentPass",
                    null, ra, model);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains("совпадают"));
        }

        @Test
        @DisplayName("changePassword возвращает false — остаётся на форме")
        void changePasswordFails_remainsOnForm() {
            when(userService.findByUsername("alice")).thenReturn(me);
            when(userService.changePassword(eq(me), eq("wrong"), eq("NewPass123"), any()))
                    .thenReturn(false);

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, "wrong", "NewPass123", "NewPass123",
                    null, ra, model);

            assertThat(view).isEqualTo("profile-edit");
        }

        @Test
        @DisplayName("Успешная смена пароля — сохраняет профиль")
        void changePassword_success() {
            when(userService.findByUsername("alice")).thenReturn(me);
            when(userService.changePassword(eq(me), eq("oldPass"), eq("NewPass123"), any()))
                    .thenReturn(true);

            String view = profileController.editSave(
                    principal, "Алиса", null, null, null, null, null,
                    null, null, null, "oldPass", "NewPass123", "NewPass123",
                    null, ra, model);

            assertThat(view).isEqualTo("redirect:/profile");
            verify(userService).saveUser(me);
            verify(ra).addFlashAttribute(eq("success"), any());
        }

        @Test
        @DisplayName("Аватар не изображение — возвращает ошибку")
        void avatarNotImage_returnsError() throws IOException {
            when(userService.findByUsername("alice")).thenReturn(me);
            MultipartFile avatar = mock(MultipartFile.class);
            when(avatar.isEmpty()).thenReturn(false);
            when(avatar.getContentType()).thenReturn("application/pdf");

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    avatar, ra, model);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains("изображением"));
        }

        @Test
        @DisplayName("Аватар > 3 МБ — возвращает ошибку")
        void avatarTooLarge_returnsError() {
            when(userService.findByUsername("alice")).thenReturn(me);
            MultipartFile avatar = mock(MultipartFile.class);
            when(avatar.isEmpty()).thenReturn(false);
            when(avatar.getContentType()).thenReturn("image/jpeg");
            when(avatar.getSize()).thenReturn(4 * 1024 * 1024L);

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    avatar, ra, model);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains("3 МБ"));
        }

        @Test
        @DisplayName("IOException при чтении аватара — возвращает ошибку")
        void avatarIOException_returnsError() throws IOException {
            when(userService.findByUsername("alice")).thenReturn(me);
            MultipartFile avatar = mock(MultipartFile.class);
            when(avatar.isEmpty()).thenReturn(false);
            when(avatar.getContentType()).thenReturn("image/jpeg");
            when(avatar.getSize()).thenReturn(1024L);
            when(avatar.getBytes()).thenThrow(new IOException("disk error"));

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    avatar, ra, model);

            assertThat(view).isEqualTo("profile-edit");
            verify(model).addAttribute(eq("error"), contains("аватара"));
        }

        @Test
        @DisplayName("Успешный аватар — сохраняется base64 URL")
        void validAvatar_savesBase64() throws IOException {
            when(userService.findByUsername("alice")).thenReturn(me);
            MultipartFile avatar = mock(MultipartFile.class);
            when(avatar.isEmpty()).thenReturn(false);
            when(avatar.getContentType()).thenReturn("image/png");
            when(avatar.getSize()).thenReturn(1024L);
            when(avatar.getBytes()).thenReturn("imgdata".getBytes());

            String view = profileController.editSave(
                    principal, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    avatar, ra, model);

            assertThat(view).isEqualTo("redirect:/profile");
            assertThat(me.getAvatarUrl()).startsWith("data:image/png;base64,");
        }

        @Test
        @DisplayName("Успешное сохранение без изменений пароля и аватара")
        void success_noPasswordNoAvatar() {
            when(userService.findByUsername("alice")).thenReturn(me);

            String view = profileController.editSave(
                    principal, "Алиса Иванова", "alice@test.com",
                    "Москва", "Россия", "F", "1999-05-15",
                    "О себе", "vk.com/alice", "Фантастика",
                    null, null, null,
                    null, ra, model);

            assertThat(view).isEqualTo("redirect:/profile");
            assertThat(me.getFullName()).isEqualTo("Алиса Иванова");
            assertThat(me.getCity()).isEqualTo("Москва");
            verify(userService).saveUser(me);
        }

        @Test
        @DisplayName("Дата рождения — корректно парсится")
        void birthDate_parsedCorrectly() {
            when(userService.findByUsername("alice")).thenReturn(me);

            profileController.editSave(
                    principal, null, null, null, null, null, "2000-01-01",
                    null, null, null, null, null, null,
                    null, ra, model);

            assertThat(me.getBirthDate()).isNotNull();
            assertThat(me.getBirthDate().getYear()).isEqualTo(2000);
        }

        @Test
        @DisplayName("Некорректная дата рождения — игнорируется (не бросает)")
        void birthDate_invalidFormat_ignored() {
            when(userService.findByUsername("alice")).thenReturn(me);

            assertThatCode(() -> profileController.editSave(
                    principal, null, null, null, null, null, "not-a-date",
                    null, null, null, null, null, null,
                    null, ra, model))
                    .doesNotThrowAnyException();
        }

        // ── helpers ──────────────────────────────────────────────────────────

        private String callEditSave(String email, String city, String country,
                                    String gender, String birthDate, String aboutMe,
                                    String socialLinks) {
            return profileController.editSave(
                    principal, null, email, city, country, gender, birthDate,
                    aboutMe, socialLinks, null, null, null, null,
                    null, ra, model);
        }

        private String callEditSaveAbout(String aboutMe) {
            return profileController.editSave(
                    principal, null, null, null, null, null, null,
                    aboutMe, null, null, null, null, null,
                    null, ra, model);
        }
    }
}