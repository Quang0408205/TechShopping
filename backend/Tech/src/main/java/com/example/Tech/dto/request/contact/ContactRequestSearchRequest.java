package com.example.Tech.dto.request.contact;

import com.example.Tech.entity.contact.ContactStatus;
import com.example.Tech.entity.contact.ContactTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Optional filters of the staff contact-request list.
 */
public record ContactRequestSearchRequest(

        @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
        @Schema(description = "Part of the sender's name, email, phone or the message", example = "giao hàng")
        String keyword,

        @Schema(example = "NEW")
        ContactStatus status,

        @Schema(example = "ORDER")
        ContactTopic topic
) {
}
