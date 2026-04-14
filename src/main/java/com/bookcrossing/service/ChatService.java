package com.bookcrossing.service;

import com.bookcrossing.dto.ConversationDTO;
import com.bookcrossing.model.Message;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.MessageRepository;
import com.bookcrossing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository    userRepository;

    public ChatService(MessageRepository messageRepository,
                       UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository    = userRepository;
    }

    public Message saveMessage(Long senderId, Long recipientId, String content) {
        User sender    = userRepository.findById(senderId).orElseThrow();
        User recipient = userRepository.findById(recipientId).orElseThrow();

        // Проверяем, не заблокировал ли получатель отправителя (#4)
        if (recipient.hasChatBlocked(senderId)) {
            throw new IllegalStateException("Пользователь заблокировал вас в чате.");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

        return messageRepository.save(message);
    }

    public List<Message> getChatHistory(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1).orElseThrow();
        User user2 = userRepository.findById(userId2).orElseThrow();
        return messageRepository.findChatHistory(user1, user2);
    }

    public List<ConversationDTO> getUserConversations(User currentUser) {
        List<Message> allMessages = messageRepository.findAllByUser(currentUser);
        Map<Long, ConversationDTO> conversations = new HashMap<>();

        for (Message m : allMessages) {
            User partner = m.getSender().equals(currentUser) ? m.getRecipient() : m.getSender();
            if (!conversations.containsKey(partner.getId())) {
                long unread = messageRepository
                        .countByRecipientAndSenderAndReadFalse(currentUser, partner);
                conversations.put(partner.getId(), new ConversationDTO(
                        partner,
                        m.getContent(),
                        m.getTimestamp(),
                        unread
                ));
            }
        }
        return new ArrayList<>(conversations.values());
    }

    @Transactional
    public void markMessagesAsRead(User recipient, User sender) {
        messageRepository.markMessagesAsRead(recipient, sender);
    }

    // ── Удаление переписки (#4) ─────────────────────────────────────────────

    /**
     * Удалить переписку у обоих пользователей — физически удаляет все сообщения.
     */
    @Transactional
    public void deleteConversationBoth(User currentUser, User partner) {
        messageRepository.deleteConversation(currentUser, partner);
    }

    /**
     * Удалить переписку только у текущего пользователя:
     * удаляем его исходящие сообщения, входящие оставляем (партнёр их не теряет).
     */
    @Transactional
    public void deleteConversationSelf(User currentUser, User partner) {
        messageRepository.deleteSentMessages(currentUser, partner);
    }

    // ── Поиск в чате (#4) ──────────────────────────────────────────────────

    public List<Message> searchInChat(User user1, User user2, String query) {
        if (query == null || query.isBlank()) return List.of();
        return messageRepository.searchInChat(user1, user2, query.trim());
    }

    // ── Блокировка в чате (#4) ─────────────────────────────────────────────

    @Transactional
    public void blockChatUser(User blocker, Long targetId) {
        blocker.getChatBlockedUsers().add(targetId);
        userRepository.save(blocker);
    }

    @Transactional
    public void unblockChatUser(User blocker, Long targetId) {
        blocker.getChatBlockedUsers().remove(targetId);
        userRepository.save(blocker);
    }

    // ── Мут (#4) ───────────────────────────────────────────────────────────

    @Transactional
    public void muteUser(User muter, Long targetId) {
        muter.getMutedUsers().add(targetId);
        userRepository.save(muter);
    }

    @Transactional
    public void unmuteUser(User muter, Long targetId) {
        muter.getMutedUsers().remove(targetId);
        userRepository.save(muter);
    }

    /** Проверить, замьютил ли пользователь партнёра (для фильтра уведомлений) */
    public boolean isMuted(User user, Long targetId) {
        return user.hasMuted(targetId);
    }
}