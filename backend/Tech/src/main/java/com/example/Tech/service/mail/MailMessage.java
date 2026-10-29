package com.example.Tech.service.mail;

/** A mail ready to send: both the HTML and the plain-text body (the text part helps against spam filters). */
public record MailMessage(String to, String subject, String html, String text) {
}
