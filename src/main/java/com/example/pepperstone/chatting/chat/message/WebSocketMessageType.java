package com.example.pepperstone.chatting.chat.message;

// WebSocket 메세지 타입
public enum WebSocketMessageType {
    ENTER("ENTER"),
    TALK("TALK"),
    EXIT("EXIT");

    private final String type;

    WebSocketMessageType(String type) {
        this.type = type;
    }

    public String getValue() {
        return this.type;
    }
}
