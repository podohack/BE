package com.example.pepperstone.chat.chatting;

import com.example.pepperstone.chat.dto.ChatDTO;
import com.example.pepperstone.chat.chatting.message.WebSocketMessage;
import com.example.pepperstone.chat.chatting.message.WebSocketMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@RequiredArgsConstructor
// socket.io의 Room 개념
public class ChatRoom {
    private final Map<String, WebSocketSession> ActiveUserMap = new ConcurrentHashMap<>();
    // ConcurrentHasMap은 멀티스레드 환경세서 안정하고 성능이 뛰어난 Map이 필요할 경우 사용
    // WebSocket 세션 관리와 같은 멀티스레드 환경에서 동시 접근이 빈번한 경우 유용
    private final ObjectMapper objectMapper;

    // 채팅방 입장
    public void enter(ChatDTO chatDTO, WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        ActiveUserMap.put(username, session);

        for(Map.Entry<String, WebSocketSession> entry : ActiveUserMap.entrySet()) {
            try {
                if(!entry.getKey().equals(username))
                    entry.getValue().sendMessage(getTextMessage(WebSocketMessageType.ENTER, chatDTO));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    // 채팅방 퇴장
    public void exit(String username, ChatDTO chatDTO) {
        ActiveUserMap.remove(chatDTO.getUsername());

        for(Map.Entry<String, WebSocketSession> entry : ActiveUserMap.entrySet()) {
            try {
                if(!entry.getKey().equals(username))
                    entry.getValue().sendMessage(getTextMessage(WebSocketMessageType.EXIT, chatDTO));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    // 메세지 전송
    public void sendMessage(String username, ChatDTO chatDTO) {
        for(Map.Entry<String, WebSocketSession> entry : ActiveUserMap.entrySet()) {
            try {
                if(!entry.getKey().equals(username))
                    entry.getValue().sendMessage(getTextMessage(WebSocketMessageType.TALK, chatDTO));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    private TextMessage getTextMessage(WebSocketMessageType type, ChatDTO chatDTO) {
        try {
            return new TextMessage(objectMapper.writeValueAsString(new WebSocketMessage(type, chatDTO)));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
