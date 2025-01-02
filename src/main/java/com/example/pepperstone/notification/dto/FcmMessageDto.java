package com.example.pepperstone.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// FCM 전송 Format DTO
@Getter
@Builder
public class FcmMessageDto {
    private boolean validateOnly; // 메시지 유효성 검사만 수행할지 여부
    private Message message; // 메시지 데이터

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Message {
        private Notification notification; // 알림 정보
        private String token; // 수신자 토큰
    }

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Notification {
        private String title; // 알림 제목
        private String body; // 알림 내용
        private String image; // 알림에 첨부할 이미지 URL
    }
}