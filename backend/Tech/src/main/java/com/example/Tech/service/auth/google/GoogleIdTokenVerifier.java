package com.example.Tech.service.auth.google;

/** Checks an ID token issued by Google. Behind an interface so tests do not call Google. */
public interface GoogleIdTokenVerifier {

    /** @throws com.example.Tech.exception.BusinessException GOOGLE_TOKEN_INVALID when the token is not acceptable */
    GoogleIdentity verify(String credential);
}
