package com.example.pepperstone.chat.chatting;

import com.example.pepperstone.chat.chatting.message.WebSocketMessageType;
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
        log.info("send chatDTO: {}", chatDTO.getMessage());

        ChatRoom chatRoom = chatRoomMap.get(chatDTO.getChatRoomId());
        if (chatRoom != null) {
            chatRoom.sendMessage(username, chatDTO);
        } else {
            log.warn("ChatRoom not found for ID: {}", chatDTO.getChatRoomId());
        }
    }

    // 채팅방 입장
    private void enterChatRoom(ChatDTO chatDTO, WebSocketSession session) {
        try {
            Long chatRoomId = chatDTO.getChatRoomId();
            log.info("User entering chat room: {}", chatRoomId);

            ChatRoomEntity chatRoomEntity = chatRoomRepo.findById(chatRoomId)
                    .orElseGet(() -> ChatRoomEntity.builder().build());

            chatRoomRepo.save(chatRoomEntity);

            ChatRoom chatRoom = chatRoomMap.computeIfAbsent(chatRoomId, id -> new ChatRoom(redisTemplate, objectMapper));
            chatRoom.enter(chatDTO, session);

            String chatRoomKey = CHAT_ROOM_KEY_PREFIX + chatRoomId;
            redisTemplate.opsForHash().put(chatRoomKey, chatDTO.getUsername(), session.getId());

            chatDTO.setMessage(chatDTO.getUsername() + "님이 입장하셨습니다.");

            log.info("User {} entered chat room {}", chatDTO.getUsername(), chatRoomId);
        } catch (Exception e) {
            log.error("Error entering chat room: {}", e.getMessage());
        }
    }


    // 채팅방 퇴장
    private void exitChatRoom(String username, ChatDTO chatDTO) {
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

        chatDTO.setMessage(chatDTO.getUsername() + "님이 퇴장하셨습니다.");
    }
}
