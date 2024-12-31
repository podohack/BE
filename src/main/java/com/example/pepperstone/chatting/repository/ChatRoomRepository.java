package com.example.pepperstone.chatting.repository;

import com.example.pepperstone.common.entity.ChatRoomEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {
    @Lock(LockModeType.OPTIMISTIC)
    Optional<ChatRoomEntity> findWithLockById(Long id);
}
