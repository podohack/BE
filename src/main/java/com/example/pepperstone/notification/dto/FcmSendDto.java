package com.example.pepperstone.notification.dto;

import lombok.*;

// 모바일에서 전달받은 객체
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmSendDto {
    private String token; // 수신자 토큰
    private String title; // 알림 제목
    private String body; // 알림 내용
}