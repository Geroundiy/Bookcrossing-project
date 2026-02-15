package com.bookcrossing.service;

import com.bookcrossing.model.User;
import com.bookcrossing.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public void updateProfile(User user, User updatedData, MultipartFile avatarFile) {
        user.setFullName(updatedData.getFullName());
        user.setCity(updatedData.getCity());
        user.setCountry(updatedData.getCountry());
        user.setBirthDate(updatedData.getBirthDate());
        user.setGender(updatedData.getGender());
        user.setAboutMe(updatedData.getAboutMe());
        user.setSocialLinks(updatedData.getSocialLinks());
        user.setFavoriteGenres(updatedData.getFavoriteGenres());

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                byte[] bytes = avatarFile.getBytes();
                String base64Image = Base64.getEncoder().encodeToString(bytes);
                user.setAvatarUrl("data:" + avatarFile.getContentType() + ";base64," + base64Image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (updatedData.getAvatarUrl() != null && !updatedData.getAvatarUrl().isEmpty()) {
            // Если пользователь вставил ссылку текстом
            user.setAvatarUrl(updatedData.getAvatarUrl());
        }

        userRepository.save(user);
    }
}