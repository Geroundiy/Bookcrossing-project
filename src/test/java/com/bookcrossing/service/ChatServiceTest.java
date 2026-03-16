package com.bookcrossing.service;

import com.bookcrossing.dto.ConversationDTO;
import com.bookcrossing.model.Message;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.MessageRepository;
import com.bookcrossing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService")
class ChatServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock UserRepository    userRepository;
    @InjectMocks ChatService chatService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User(); alice.setId(1L); alice.setUsername("alice");
        bob   = new User(); bob.setId(2L);   bob.setUsername("bob");
    }

    // ─── saveMessage ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("saveMessage")
    class SaveMessage {

        @Test
        @DisplayName("Сохраняет сообщение с правильными полями")
        void savesMessageWithCorrectFields() {
            Message saved = new Message();
            saved.setId(10L);
            saved.setSender(alice);
            saved.setRecipient(bob);
            saved.setContent("Привет!");

            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(messageRepository.save(any())).thenReturn(saved);

            Message result = chatService.saveMessage(1L, 2L, "Привет!");

            assertThat(result.getSender()).isEqualTo(alice);
            assertThat(result.getRecipient()).isEqualTo(bob);
            assertThat(result.getContent()).isEqualTo("Привет!");
        }

        @Test
        @DisplayName("Новое сообщение — read=false")
        void newMessage_readFalse() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

            var cap = org.mockito.ArgumentCaptor.forClass(Message.class);
            when(messageRepository.save(cap.capture())).thenAnswer(i -> i.getArgument(0));

            chatService.saveMessage(1L, 2L, "Test");

            assertThat(cap.getValue().isRead()).isFalse();
        }

        @Test
        @DisplayName("Timestamp устанавливается автоматически")
        void timestampSetAutomatically() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

            var cap = org.mockito.ArgumentCaptor.forClass(Message.class);
            when(messageRepository.save(cap.capture())).thenAnswer(i -> i.getArgument(0));

            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            chatService.saveMessage(1L, 2L, "Hello");
            LocalDateTime after  = LocalDateTime.now().plusSeconds(1);

            assertThat(cap.getValue().getTimestamp())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("Отправитель не найден — бросает исключение")
        void senderNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> chatService.saveMessage(1L, 2L, "Hi"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Получатель не найден — бросает исключение")
        void recipientNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> chatService.saveMessage(1L, 2L, "Hi"))
                    .isInstanceOf(Exception.class);
        }
    }

    // ─── getChatHistory ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getChatHistory")
    class GetChatHistory {

        @Test
        @DisplayName("Делегирует в repository.findChatHistory")
        void delegatesToRepository() {
            Message m = new Message(); m.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(messageRepository.findChatHistory(alice, bob)).thenReturn(List.of(m));

            List<Message> history = chatService.getChatHistory(1L, 2L);

            assertThat(history).containsExactly(m);
            verify(messageRepository).findChatHistory(alice, bob);
        }

        @Test
        @DisplayName("Пустая история — возвращает пустой список")
        void emptyHistory_returnsEmptyList() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(messageRepository.findChatHistory(alice, bob)).thenReturn(List.of());

            assertThat(chatService.getChatHistory(1L, 2L)).isEmpty();
        }

        @Test
        @DisplayName("Возвращает сообщения в порядке, который даёт repository")
        void returnsRepositoryOrder() {
            Message m1 = new Message(); m1.setId(1L);
            Message m2 = new Message(); m2.setId(2L);
            Message m3 = new Message(); m3.setId(3L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(messageRepository.findChatHistory(alice, bob)).thenReturn(List.of(m1, m2, m3));

            List<Message> history = chatService.getChatHistory(1L, 2L);

            assertThat(history).containsExactly(m1, m2, m3);
        }
    }

    // ─── markMessagesAsRead ───────────────────────────────────────────────────

    @Nested
    @DisplayName("markMessagesAsRead")
    class MarkMessagesAsRead {

        @Test
        @DisplayName("Вызывает repository.markMessagesAsRead (bulk-UPDATE)")
        void callsBulkUpdate() {
            chatService.markMessagesAsRead(alice, bob);
            verify(messageRepository).markMessagesAsRead(alice, bob);
        }

        @Test
        @DisplayName("Не вызывает findChatHistory (нет перебора поштучно)")
        void doesNotIterateMessages() {
            chatService.markMessagesAsRead(alice, bob);
            verify(messageRepository, never()).findChatHistory(any(), any());
        }

        @Test
        @DisplayName("Не бросает исключение, даже если нет непрочитанных")
        void noException_whenNoUnreadMessages() {
            // markMessagesAsRead работает с bulk UPDATE — ноль строк не ошибка
            assertThatNoException().isThrownBy(() ->
                    chatService.markMessagesAsRead(alice, bob));
        }
    }

    // ─── getUserConversations ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserConversations")
    class GetUserConversations {

        @Test
        @DisplayName("Нет сообщений — пустой список диалогов")
        void noMessages_emptyList() {
            when(messageRepository.findAllByUser(alice)).thenReturn(List.of());
            assertThat(chatService.getUserConversations(alice)).isEmpty();
        }

        @Test
        @DisplayName("Одно входящее сообщение — один диалог с отправителем")
        void incomingMessage_oneConversationWithSender() {
            Message m = makeMessage(bob, alice, "Hi", LocalDateTime.now());
            when(messageRepository.findAllByUser(alice)).thenReturn(List.of(m));
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, bob)).thenReturn(0L);

            List<ConversationDTO> convs = chatService.getUserConversations(alice);

            assertThat(convs).hasSize(1);
            assertThat(convs.get(0).getPartner()).isEqualTo(bob);
        }

        @Test
        @DisplayName("Одно исходящее сообщение — один диалог с получателем")
        void outgoingMessage_oneConversationWithRecipient() {
            Message m = makeMessage(alice, bob, "Hello", LocalDateTime.now());
            when(messageRepository.findAllByUser(alice)).thenReturn(List.of(m));
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, bob)).thenReturn(0L);

            List<ConversationDTO> convs = chatService.getUserConversations(alice);

            assertThat(convs).hasSize(1);
            assertThat(convs.get(0).getPartner()).isEqualTo(bob);
        }

        @Test
        @DisplayName("Несколько сообщений с одним партнёром — один диалог")
        void multipleMessagesOnePartner_onlyOneConversation() {
            Message m1 = makeMessage(bob,   alice, "Hi",    LocalDateTime.now().minusMinutes(5));
            Message m2 = makeMessage(alice, bob,   "Hello", LocalDateTime.now().minusMinutes(3));
            Message m3 = makeMessage(bob,   alice, "Bye",   LocalDateTime.now());

            when(messageRepository.findAllByUser(alice)).thenReturn(List.of(m1, m2, m3));
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, bob)).thenReturn(0L);

            List<ConversationDTO> convs = chatService.getUserConversations(alice);

            assertThat(convs).hasSize(1);
        }

        @Test
        @DisplayName("Сообщения с двумя разными партнёрами — два диалога")
        void twoPartners_twoConversations() {
            User charlie = new User(); charlie.setId(3L); charlie.setUsername("charlie");

            Message m1 = makeMessage(bob,     alice, "Hello",   LocalDateTime.now().minusMinutes(5));
            Message m2 = makeMessage(charlie, alice, "Привет",  LocalDateTime.now());

            when(messageRepository.findAllByUser(alice)).thenReturn(List.of(m1, m2));
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, bob)).thenReturn(0L);
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, charlie)).thenReturn(0L);

            List<ConversationDTO> convs = chatService.getUserConversations(alice);

            assertThat(convs).hasSize(2);
        }

        @Test
        @DisplayName("unreadCount заполняется из repository")
        void unreadCountFromRepository() {
            Message m = makeMessage(bob, alice, "Read me!", LocalDateTime.now());
            when(messageRepository.findAllByUser(alice)).thenReturn(List.of(m));
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, bob)).thenReturn(3L);

            List<ConversationDTO> convs = chatService.getUserConversations(alice);

            assertThat(convs.get(0).getUnreadCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("lastMessage содержит текст последнего сообщения")
        void lastMessageIsCorrect() {
            Message m = makeMessage(bob, alice, "Last msg", LocalDateTime.now());
            when(messageRepository.findAllByUser(alice)).thenReturn(List.of(m));
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, bob)).thenReturn(0L);

            List<ConversationDTO> convs = chatService.getUserConversations(alice);

            assertThat(convs.get(0).getLastMessage()).isEqualTo("Last msg");
        }

        @Test
        @DisplayName("lastMessageTime содержит время последнего сообщения")
        void lastMessageTimeIsCorrect() {
            LocalDateTime ts = LocalDateTime.of(2024, 6, 15, 12, 0);
            Message m = makeMessage(bob, alice, "Hi", ts);
            when(messageRepository.findAllByUser(alice)).thenReturn(List.of(m));
            when(messageRepository.countByRecipientAndSenderAndReadFalse(alice, bob)).thenReturn(0L);

            List<ConversationDTO> convs = chatService.getUserConversations(alice);

            assertThat(convs.get(0).getLastMessageTime()).isEqualTo(ts);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Message makeMessage(User sender, User recipient, String content, LocalDateTime ts) {
        Message m = new Message();
        m.setSender(sender);
        m.setRecipient(recipient);
        m.setContent(content);
        m.setTimestamp(ts);
        return m;
    }
}