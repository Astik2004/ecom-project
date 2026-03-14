package com.astik.user_service.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String msg) { super(msg); }
}
