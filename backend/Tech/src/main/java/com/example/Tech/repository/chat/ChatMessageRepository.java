package com.example.Tech.repository.chat;

import com.example.Tech.entity.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllBySessionIdOrderByIdAsc(Long sessionId);

    /** Newest first; the caller reverses it to get the last messages in order. */
    List<ChatMessage> findBySessionIdOrderByIdDesc(Long sessionId, Pageable pageable);

    @Query("select m.sessionId, count(m) from ChatMessage m where m.sessionId in :sessionIds group by m.sessionId")
    List<Object[]> countBySessionIds(Collection<Long> sessionIds);
}
