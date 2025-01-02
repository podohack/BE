package com.example.pepperstone.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fcm_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}