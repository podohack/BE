package com.example.pepperstone.user.service;

import com.example.pepperstone.common.entity.UserEntity;
import com.example.pepperstone.user.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {
    private final UserRepository userRepo;

    public String login(String username) {
        // 데이터베이스에서 유저 조회
        Optional<UserEntity> user = userRepo.findByUsername(username);

        if (user.isPresent()) {
            // 인증 성공 - "chatUser" 접두사로 토큰 생성
            return "chatUser" + user.get().getId();
        } else {
            // 인증 실패
            throw new IllegalArgumentException("Invalid username");
        }
    }
}
