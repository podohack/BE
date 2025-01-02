package com.example.pepperstone.notification.service;

import com.example.pepperstone.common.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public interface FcmService {
    void sendPushNotification(String title, String body, UserEntity user);
    void saveFcmToken(String token, UserEntity user);
}