package com.bookcrossing.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WelcomeController")
class WelcomeControllerTest {

    @InjectMocks WelcomeController welcomeController;

    @Test
    @DisplayName("GET /welcome — возвращает welcome")
    void welcome_returnsWelcomeView() {
        assertThat(welcomeController.welcome()).isEqualTo("welcome");
    }
}