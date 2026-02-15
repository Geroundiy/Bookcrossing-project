package com.bookcrossing.dto;

import com.bookcrossing.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConversationDTO {
    private User partner; // С кем диалог
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
}