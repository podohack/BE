package com.example.pepperstone.chatting.websocket.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
// WebSocket 연결 전 인터셉터
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    // Authorization 헤더를 확인하여 유저 인증
    // 유저명이 채팅유저로 시작하지 않으면 401 반환
    // 유저명이 채팅유저로 시작해서 인증된 유저라면 session username을 저장

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if(request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

            String token = servletRequest.getHeader("Authorization");
            if(token != null && token.startsWith("user")) {
                attributes.put("username", token);
                return true;
            } else {
                servletResponse.setStatus(401);
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
