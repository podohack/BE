package com.example.pepperstone.notification.service.impl;

import com.example.pepperstone.common.entity.FcmTokenEntity;
import com.example.pepperstone.common.entity.UserEntity;
import com.example.pepperstone.notification.repository.FcmTokenRepository;
import com.example.pepperstone.notification.service.FcmService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.example.pepperstone.notification.dto.FcmMessageDto;
import com.example.pepperstone.notification.dto.FcmSendDto;
import com.example.pepperstone.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {
    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public void sendPushNotification(String title, String body, UserEntity user) {
        fcmTokenRepository.findByToken(user.getId().toString()).ifPresent(fcmToken -> {
            try {
                FcmSendDto fcmSendDto = FcmSendDto.builder()
                        .token(fcmToken.getToken())
                        .title(title)
                        .body(body)
                        .build();
                sendMessageTo(fcmSendDto);
            } catch (Exception e) {
                log.error("Error sending push notification: {}", e.getMessage());
            }
        });
    }

    @Override
    public void saveFcmToken(String token, UserEntity user) {
        if (fcmTokenRepository.findByToken(token).isEmpty()) {
            FcmTokenEntity fcmToken = FcmTokenEntity.builder()
                    .token(token)
                    .user(user)
                    .build();
            fcmTokenRepository.save(fcmToken);
        }
    }

    private int sendMessageTo(FcmSendDto fcmSendDto) throws IOException {
        String message = makeMessage(fcmSendDto);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getAccessToken());

        HttpEntity<String> entity = new HttpEntity<>(message, headers);
        String API_URL = "https://fcm.googleapis.com/v1/projects/pushtest-a8210/messages:send";

        try {
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
            return response.getStatusCode() == HttpStatus.OK ? 1 : 0;
        } catch (Exception e) {
            log.error("Error sending FCM message: {}", e.getMessage());
            return 0;
        }
    }

    private String getAccessToken() throws IOException {
        String firebaseConfigPath = "firebase/pushtest-firebase-adminsdk.json";

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    private String makeMessage(FcmSendDto fcmSendDto) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        FcmMessageDto fcmMessageDto = FcmMessageDto
                .builder()
                .message(FcmMessageDto.Message.builder()
                        .token(fcmSendDto.getToken())
                        .notification(FcmMessageDto.Notification.builder()
                                .title(fcmSendDto.getTitle())
                                .body(fcmSendDto.getBody())
                                .image(null)
                                .build()
                        ).build())
                .validateOnly(false)
                .build();

        return om.writeValueAsString(fcmMessageDto);
    }
}
