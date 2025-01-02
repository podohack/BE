package com.example.pepperstone.chatting.chat;

import com.example.pepperstone.chatting.dto.ChatDTO;
import com.example.pepperstone.chatting.repository.ChatRoomRepository;
import com.example.pepperstone.chatting.service.ChatService;
import com.example.pepperstone.chatting.chat.message.WebSocketMessage;
import com.example.pepperstone.common.entity.ChatRoomEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    private final Map<Long, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatService chatService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        String username = (String) session.getAttributes().get("username");
        WebSocketMessage webSocketMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

        // payload를 ChatDTO로 변환
        ChatDTO chatDTO = objectMapper.convertValue(webSocketMessage.getPayload(), ChatDTO.class);

        switch (webSocketMessage.getType().getValue()) {
            case "ENTER" -> enterChatRoom(chatDTO,session);
            case "TALK" -> {
                sendMessage(username, chatDTO);
                chatService.saveMessage(username, chatDTO); // 메세지 DB 저장
            }
            case "EXIT" -> exitChatRoom(username, chatDTO);
        }
    }

    // 메세지 전송
    private void sendMessage(String username, ChatDTO chatDTO) {
        log.info("send chatDTO : {}", chatDTO.getMessage());

        ChatRoom chatRoom = chatRoomMap.get(chatDTO.getChatRoomId());
        chatRoom.sendMessage(username, chatDTO);
    }

    // 채팅방 입장
    private void enterChatRoom(ChatDTO chatDTO, WebSocketSession session) {
        try {
            log.info("enter chatDTO : {}", chatDTO.getChatRoomId());

            // DB에서 채팅방 확인
            Long chatRoomId = chatDTO.getChatRoomId();
            ChatRoomEntity chatRoomEntity = chatRoomRepo.findById(chatRoomId)
                    .orElseGet(() -> ChatRoomEntity.builder().build()); // id 제거

            chatRoomRepo.save(chatRoomEntity);

            // 메모리 내 채팅방 관리
            ChatRoom chatRoom = chatRoomMap.getOrDefault(chatRoomId, new ChatRoom(objectMapper));
            chatRoom.enter(chatDTO, session);
            chatRoomMap.put(chatRoomId, chatRoom);

            chatDTO.setMessage(chatDTO.getUsername() + "님이 입장하셨습니다.");

            log.info("User entered chat room: {}", chatRoomId);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking failure: {}", e.getMessage());
            // 사용자에게 재시도 안내 또는 예외 처리 로직
        }
    }

    // 채팅방 퇴장
    private void exitChatRoom(String username, ChatDTO chatDTO) {
        log.info("exit chatDTO : {}", chatDTO.getChatRoomId());

        chatDTO.setMessage(chatDTO.getUsername() + "님이 퇴장하셨습니다.");
        ChatRoom chatRoom = chatRoomMap.get(chatDTO.getChatRoomId());
        chatRoom.exit(username, chatDTO);

        if(chatRoom.getActiveUserMap().isEmpty())
            chatRoomMap.remove(chatDTO.getChatRoomId());
    }
}
