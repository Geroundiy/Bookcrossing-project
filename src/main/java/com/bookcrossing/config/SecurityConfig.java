package com.bookcrossing.config;

import com.bookcrossing.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        // Разрешаем регистрацию, CSS, JS и картинки (для фона)
                        .requestMatchers("/register", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated() // Все остальное только для вошедших
                )
                .formLogin((form) -> form
                        .loginPage("/login") // URL нашей страницы входа
                        .loginProcessingUrl("/login") // Куда отправлять POST форму
                        .defaultSuccessUrl("/", true) // Куда перекинуть после успеха
                        .failureUrl("/login?error=true") // Куда кидать при ошибке
                        .permitAll()
                )
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") // Куда кидать после выхода
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepo) {
        return input -> {
            // Ищем пользователя: либо логин совпадает с input, либо email совпадает с input
            return userRepo.findByUsernameOrEmail(input, input)
                    .map(u -> User.builder()
                            .username(u.getUsername())
                            .password(u.getPassword())
                            .roles("USER")
                            .build())
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        };
    }
}
// <-- Вот эта скобка, скорее всего, потерялась