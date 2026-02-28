package com.bookcrossing.service;

import com.bookcrossing.dto.ConversationDTO;
import com.bookcrossing.model.Message;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.MessageRepository;
import com.bookcrossing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository; // [cite: 171]

    @Mock
    private UserRepository userRepository; // [cite: 172]

    @InjectMocks
    private ChatService chatService;

    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("user1");

        recipient = new User();
        recipient.setId(2L);
        recipient.setUsername("user2");
    }

    @Test
    void testSaveMessage() {
        // Настраиваем поведение моков
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Message savedMessage = new Message();
        savedMessage.setContent("Привет!");
        savedMessage.setSender(sender);
        savedMessage.setRecipient(recipient);

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        // Вызываем метод
        Message result = chatService.saveMessage(1L, 2L, "Привет!"); // [cite: 173]

        // Проверяем результат
        assertNotNull(result);
        assertEquals("Привет!", result.getContent());
        assertEquals(sender, result.getSender());
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void testGetChatHistory() {
        Message msg1 = new Message();
        msg1.setContent("Msg 1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(messageRepository.findChatHistory(sender, recipient)).thenReturn(List.of(msg1));

        List<Message> history = chatService.getChatHistory(1L, 2L);

        assertEquals(1, history.size());
        assertEquals("Msg 1", history.get(0).getContent());
    }
}