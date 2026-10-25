package com.example.Tech.service.mail;

/** Sends one mail. Behind an interface so tests (and a later switch to another provider) replace the SMTP part. */
public interface MailTransport {

    /** false = not configured (nothing was sent); throws when the provider refuses or cannot be reached. */
    boolean send(MailMessage message) throws Exception;
}
