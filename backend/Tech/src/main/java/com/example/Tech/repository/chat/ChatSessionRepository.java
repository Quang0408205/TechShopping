package com.example.Tech.repository.chat;

import com.example.Tech.entity.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findByToken(String token);
}
