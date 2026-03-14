package com.astik.user_service.exception;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException(String msg) { super(msg); }
}