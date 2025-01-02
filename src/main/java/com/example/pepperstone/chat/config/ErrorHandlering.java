package com.example.pepperstone.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@Component
public class ErrorHandlering {
    public static class ChatRoomNotFoundException extends RuntimeException {
        public ChatRoomNotFoundException(String message) {
            super(message);
        }
    }

    public void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            // 에러 메시지를 클라이언트로 전송
            session.sendMessage(new TextMessage(errorMessage));
        } catch (IOException e) {
            log.error("Error sending error message", e);
        }
    }
}
