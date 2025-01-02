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
// 특정 채팅방에 대한 데이터를 관리하고, 해당 채팅방의 사용자들에게 메시지를 전달하는 역할
public class ChatRoom {
    // 채팅방 상태 관리
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> activeUserMap = new ConcurrentHashMap<>();

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

    // 채팅방에 있는 모든 사용자에게 메세지 전송
    private void broadcastMessage(String senderUsername, ChatDTO chatDTO) {
        try {
            String message = objectMapper.writeValueAsString(chatDTO);
            TextMessage textMessage = new TextMessage(message);

            activeUserMap.forEach((username, session) -> {
                if (!username.equals(senderUsername) && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        // 전송 도중 문제 발생 시, 해당 사용자의 세션 제거
                        log.error("Failed to send message to {}", username, e);
                        activeUserMap.remove(username);
                    }
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message", e);
        }
    }
}
