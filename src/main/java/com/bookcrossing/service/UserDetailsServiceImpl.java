package com.bookcrossing.service;

import com.bookcrossing.model.User;
import com.bookcrossing.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        // ── Проверка блокировки ──────────────────────────────────
        if (user.isCurrentlyBlocked()) {
            // Spring Security интерпретирует enabled=false как "аккаунт заблокирован"
            String reason = user.getBlockReason() != null ? user.getBlockReason() : "без указания причины";
            throw new UsernameNotFoundException(
                    "Ваш аккаунт заблокирован. Причина: " + reason);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}