package com.example.pepperstone.chat.chatting;

import com.example.pepperstone.chat.dto.ChatDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@RequiredArgsConstructor
public class ChatRoom {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> activeUserMap = new ConcurrentHashMap<>();

    private static final String CHAT_ROOMS_KEY = "chat:rooms:";
    private static final String CHAT_USERS_KEY = "chat:users:";

    // 채팅방 입장
    public void enter(ChatDTO chatDTO, WebSocketSession session) {
        String username = chatDTO.getUsername();
        Long roomId = chatDTO.getChatRoomId();

        activeUserMap.put(username, session);
        redisTemplate.opsForHash().put(CHAT_USERS_KEY + roomId, username, session.getId());

        broadcastMessage(username, chatDTO);
    }

    // 채팅방 퇴장
    public void exit(String username, ChatDTO chatDTO) {
        Long roomId = chatDTO.getChatRoomId();

        activeUserMap.remove(username);
        redisTemplate.opsForHash().delete(CHAT_USERS_KEY + roomId, username);

        broadcastMessage(username, chatDTO);
    }

    // 메세지 전송
    public void sendMessage(String username, ChatDTO chatDTO) {
        broadcastMessage(username, chatDTO);
    }

    private void broadcastMessage(String senderUsername, ChatDTO chatDTO) {
        String message;
        try {
            message = objectMapper.writeValueAsString(chatDTO);
        } catch (JsonProcessingException e) {
            log.error("Message serialization failed", e);
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        activeUserMap.forEach((username, session) -> {
            if (!username.equals(senderUsername) && session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (Exception e) {
                    log.error("Failed to send message to: " + username, e);
                    activeUserMap.remove(username);
                }
            }
        });
    }
}
