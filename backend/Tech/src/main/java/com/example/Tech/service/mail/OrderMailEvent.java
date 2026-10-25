package com.example.Tech.service.mail;

/** Published inside the order transaction; the mail is built and sent only after that transaction commits. */
public record OrderMailEvent(Long orderId, MailKind kind) {
}
