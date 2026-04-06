package com.bookcrossing.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final CustomAuthFailureHandler authFailureHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
                          CustomAuthFailureHandler authFailureHandler) {
        this.userDetailsService = userDetailsService;
        this.authFailureHandler = authFailureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Кастомный точка входа для неаутентифицированных запросов.
     *
     * Стандартный Spring Security при обращении к защищённому ресурсу
     * перенаправляет сразу на страницу логина (/login).
     * Здесь мы меняем это поведение: первый раз пользователь попадает
     * на приветственный экран (/welcome), откуда сам нажимает кнопку входа.
     */
    @Bean
    public AuthenticationEntryPoint welcomeEntryPoint() {
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request,
                                 HttpServletResponse response,
                                 AuthenticationException authException) throws IOException {
                // Запросы к статике и API пропускаем без редиректа
                String uri = request.getRequestURI();
                if (uri.startsWith("/css/") || uri.startsWith("/js/")
                        || uri.startsWith("/images/") || uri.startsWith("/sounds/")
                        || uri.startsWith("/api/") || uri.startsWith("/ws/")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                // Все остальные защищённые запросы → приветственный экран
                response.sendRedirect(request.getContextPath() + "/welcome");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"))
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        // Публичные маршруты
                        .requestMatchers(
                                "/welcome",
                                "/register", "/login",
                                "/css/**", "/js/**", "/images/**", "/sounds/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/moderator/**").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers("/complaints/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureHandler(authFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/welcome")   // после выхода — тоже на welcome
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(welcomeEntryPoint())  // ← ключевое изменение
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }
}