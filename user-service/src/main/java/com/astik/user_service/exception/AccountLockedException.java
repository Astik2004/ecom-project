package com.astik.user_service.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String msg) { super(msg); }
}
