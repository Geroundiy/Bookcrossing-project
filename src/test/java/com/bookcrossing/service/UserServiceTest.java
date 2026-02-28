package com.bookcrossing.service;

import com.bookcrossing.model.User;
import com.bookcrossing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void testFindById_UserExists() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testFindById_UserNotFound() {
        when(userRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.findById(100L));
    }

    @Test
    void testSaveUser_ShouldHashPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("rawPassword");

        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");

        userService.save(user);

        assertEquals("hashedPassword", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }
}