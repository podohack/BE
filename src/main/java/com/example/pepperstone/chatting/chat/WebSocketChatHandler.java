package com.example.pepperstone.chatting.chat;

import com.example.pepperstone.chatting.chat.dto.ChatDTO;
import com.example.pepperstone.chatting.chatRoom.ChatRoom;
import com.example.pepperstone.chatting.websocket.message.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final Map<Long, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        String username = (String) session.getAttributes().get("username");
        WebSocketMessage webSocketMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

        // payload를 ChatDTO로 변환
        ChatDTO chatDTO = objectMapper.convertValue(webSocketMessage.getPayload(), ChatDTO.class);

        switch (webSocketMessage.getType().getValue()) {
            case "ENTER" -> enterChatRoom(chatDTO,session);
            case "TALK" -> sendMessage(username, chatDTO);
            case "EXIT" -> exitChatRoom(username, chatDTO);
        }
    }

    /**
     * 메시지 전송
     * @param chatDTO ChatDTO
     */
    private void sendMessage(String username, ChatDTO chatDTO) {
        log.info("send chatDTO : {}", chatDTO.getMessage());
        ChatRoom chatRoom = chatRoomMap.get(chatDTO.getChatRoomId());
        chatRoom.sendMessage(username, chatDTO);
    }

    /**
     * 채팅방 입장
     * @param chatDTO ChatDTO
     * @param session WebSocket 세션
     */
    private void enterChatRoom(ChatDTO chatDTO, WebSocketSession session) {
        log.info("enter chatDTO : {}", chatDTO.getChatRoomId());
        chatDTO.setMessage(chatDTO.getUsername() + "님이 입장하셨습니다.");
        ChatRoom chatRoom = chatRoomMap.getOrDefault(chatDTO.getChatRoomId(), new ChatRoom(objectMapper));
        chatRoom.enter(chatDTO, session);
        chatRoomMap.put(chatDTO.getChatRoomId(), chatRoom);
    }

    /**
     * 채팅방 퇴정
     * @param chatDTO ChatDTO
     */
    private void exitChatRoom(String username, ChatDTO chatDTO) {
        log.info("exit chatDTO : {}", chatDTO.getChatRoomId());
        chatDTO.setMessage(chatDTO.getUsername() + "님이 퇴장하셨습니다.");
        ChatRoom chatRoom = chatRoomMap.get(chatDTO.getChatRoomId());
        chatRoom.exit(username, chatDTO);

        if(chatRoom.getActiveUserMap().isEmpty())
            chatRoomMap.remove(chatDTO.getChatRoomId());
    }
}
