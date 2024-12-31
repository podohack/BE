package com.example.pepperstone.user.controller;

import com.example.pepperstone.user.dto.request.SignInRequestDTO;
import com.example.pepperstone.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/auth")
public class UserController {
    private final UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<Map<String, Object>> signIn(@RequestBody SignInRequestDTO signInRequestDTO) {
        try {
            String token = userService.login(signInRequestDTO.getUsername());

            final Map<String, String> resDTO = new HashMap<>();
            resDTO.put("username", signInRequestDTO.getUsername());
            resDTO.put("Authorization", token);

            return ResponseEntity.ok().body(Map.of("code", 200, "data", resDTO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "data", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("code", 500, "data", "로그인 오류. 잠시 후 다시 시도해주세요."));
        }
    }
}
