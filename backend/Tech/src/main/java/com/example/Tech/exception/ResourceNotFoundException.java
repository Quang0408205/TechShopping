package com.example.Tech.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode, Object id) {
        super(errorCode, "%s with id %s".formatted(errorCode.getDefaultMessage(), id));
    }
}
