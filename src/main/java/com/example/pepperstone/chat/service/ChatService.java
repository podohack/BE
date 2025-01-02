package com.example.pepperstone.chat.service;

import com.example.pepperstone.chat.dto.ChatDTO;
import com.example.pepperstone.chat.repository.ChatMessageRepository;
import com.example.pepperstone.chat.repository.ChatRoomRepository;
import com.example.pepperstone.common.entity.ChatMessageEntity;
import com.example.pepperstone.common.entity.ChatRoomEntity;
import com.example.pepperstone.common.entity.UserEntity;
import com.example.pepperstone.user.respository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ChatService {
    private final ChatRoomRepository chatRoomRepo;
    private final UserRepository userRepo;
    private final ChatMessageRepository chatMessageRepo;

    @Transactional
    public void saveMessage(String username, ChatDTO chatDTO) {
        try {
            ChatRoomEntity chatRoom = chatRoomRepo.findById(chatDTO.getChatRoomId())
                    .orElseThrow(() -> new RuntimeException("Chat room not found"));

            UserEntity sender = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ChatMessageEntity message = ChatMessageEntity.builder()
                    .message(String.valueOf(chatDTO.getMessage()))
                    .chatRoom(chatRoom)
                    .sender(sender)
                    .build();

            chatMessageRepo.save(message);
        } catch (Exception e) {
            log.error("Failed to save message", e);
            throw new RuntimeException("Message save failed", e);
        }
    }
}
