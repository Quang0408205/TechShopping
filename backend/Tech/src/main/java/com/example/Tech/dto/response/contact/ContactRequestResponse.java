package com.example.Tech.dto.response.contact;

import com.example.Tech.entity.contact.ContactStatus;
import com.example.Tech.entity.contact.ContactTopic;

import java.time.LocalDateTime;

/**
 * A contact request for staff. userId / username = the sender's account when logged in (null for a guest);
 * handledByName = who changed it last.
 */
public record ContactRequestResponse(
        Long id,
        Long userId,
        String username,
        String fullName,
        String email,
        String phone,
        ContactTopic topic,
        String message,
        ContactStatus status,
        String staffNote,
        String handledByName,
        LocalDateTime handledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
