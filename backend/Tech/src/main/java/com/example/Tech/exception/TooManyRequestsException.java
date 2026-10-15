package com.example.Tech.exception;

import lombok.Getter;

/** 429 that also tells the client when to retry (Retry-After header). */
@Getter
public class TooManyRequestsException extends BusinessException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(ErrorCode errorCode, String message, long retryAfterSeconds) {
        super(errorCode, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
