package com.example.Tech.repository.mail;

import com.example.Tech.entity.mail.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findAllByOrderIdOrderByIdAsc(Long orderId);

    boolean existsByOrderIdAndKindAndStatus(Long orderId, String kind, String status);
}
