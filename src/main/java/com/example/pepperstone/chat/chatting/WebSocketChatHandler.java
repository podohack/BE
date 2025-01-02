package com.example.pepperstone.chat.chatting;

import com.example.pepperstone.chat.dto.ChatDTO;
import com.example.pepperstone.chat.repository.ChatRoomRepository;
import com.example.pepperstone.chat.service.ChatService;
import com.example.pepperstone.chat.chatting.message.WebSocketMessage;
import com.example.pepperstone.common.entity.ChatRoomEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
// WebSocket 연결에서 발생하는 다양한 이벤트(연결, 메시지 수신, 종료 등)를 처리하는 클래스
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ChatRoomRepository chatRoomRepo;
    private final ChatService chatService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Map<Long, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CHAT_ROOM_KEY_PREFIX = "chatroom:";

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        String username = (String) session.getAttributes().get("username");
        WebSocketMessage webSocketMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

        ChatDTO chatDTO = objectMapper.convertValue(webSocketMessage.getPayload(), ChatDTO.class);

        switch (webSocketMessage.getType()) {
            case ENTER -> handleEnter(chatDTO, session);
            case TALK -> handleTalk(username, chatDTO);
            case EXIT -> handleExit(username, chatDTO);
            default -> log.warn("Unsupported message type: {}", webSocketMessage.getType());
        }
    }

    private void handleEnter(ChatDTO chatDTO, WebSocketSession session) {
        try {
            Long chatRoomId = chatDTO.getChatRoomId();
            log.info("{} entered chat room {}", chatDTO.getUsername(), chatRoomId);

            ChatRoom chatRoom = chatRoomMap.computeIfAbsent(chatRoomId, id -> {
                chatRoomRepo.findById(chatRoomId).orElseGet(() -> chatRoomRepo.save(ChatRoomEntity.builder().build()));
                return new ChatRoom(redisTemplate, objectMapper);
            });

            chatRoom.enter(chatDTO, session);

            String chatRoomKey = CHAT_ROOM_KEY_PREFIX + chatRoomId;
            redisTemplate.opsForHash().put(chatRoomKey, chatDTO.getUsername(), session.getId());

            chatDTO.setMessage(chatDTO.getUsername() + "님이 입장하셨습니다.");
        } catch (Exception e) {
            log.error("Error entering chat room", e);
        }
    }

    private void handleTalk(String username, ChatDTO chatDTO) {
        sendMessage(username, chatDTO);
        chatService.saveMessage(username, chatDTO);
    }

    private void sendMessage(String username, ChatDTO chatDTO) {
        log.info("Sending message: {}", chatDTO.getMessage());

        ChatRoom chatRoom = chatRoomMap.get(chatDTO.getChatRoomId());
        if (chatRoom != null) {
            chatRoom.sendMessage(username, chatDTO);
        } else {
            log.warn("ChatRoom not found for ID: {}", chatDTO.getChatRoomId());
        }
    }

    private void handleExit(String username, ChatDTO chatDTO) {
        Long chatRoomId = chatDTO.getChatRoomId();
        log.info("User exiting chat room: {}", chatRoomId);

        ChatRoom chatRoom = chatRoomMap.get(chatRoomId);
        if (chatRoom != null) {
            chatRoom.exit(username, chatDTO);

            String chatRoomKey = CHAT_ROOM_KEY_PREFIX + chatRoomId;
            redisTemplate.opsForHash().delete(chatRoomKey, username);

            if (chatRoom.getActiveUserMap().isEmpty()) {
                chatRoomMap.remove(chatRoomId);
                log.info("Chat room {} is now empty and removed from memory.", chatRoomId);
            }
        } else {
            log.warn("ChatRoom not found for ID: {}", chatRoomId);
        }

        chatDTO.setMessage(username + "님이 퇴장하셨습니다.");
    }
}
