package com.example.pepperstone.chatting.service;

import com.example.pepperstone.chatting.dto.ChatDTO;
import com.example.pepperstone.chatting.repository.ChatMessageRepository;
import com.example.pepperstone.chatting.repository.ChatRoomRepository;
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
        ChatRoomEntity chatRoom = chatRoomRepo.findWithLockById(chatDTO.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid chat room ID"));

        UserEntity sender = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sender username"));

        final ChatMessageEntity chatMessage = ChatMessageEntity.builder()
                .message((String) chatDTO.getMessage())
                .chatRoom(chatRoom)
                .sender(sender)
                .build();

        chatMessageRepo.save(chatMessage);
    }
}
