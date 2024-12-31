package com.example.pepperstone.chatting.repository;

import com.example.pepperstone.common.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
}
