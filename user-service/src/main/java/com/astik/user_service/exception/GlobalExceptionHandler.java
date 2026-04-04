package com.astik.user_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserExists(UserAlreadyExistsException ex, HttpServletRequest req) {
        return createProblemDetail(ex, HttpStatus.CONFLICT, "User Already Exists", req.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest req) {
        return createProblemDetail(ex, HttpStatus.NOT_FOUND, "User Not Found", req.getRequestURI());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        return createProblemDetail(ex, HttpStatus.UNAUTHORIZED, "Invalid Credentials", req.getRequestURI());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex, HttpServletRequest req) {
        return createProblemDetail(ex, HttpStatus.LOCKED, "Account Locked", req.getRequestURI());
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ProblemDetail handleAccountInactive(AccountInactiveException ex, HttpServletRequest req) {
        return createProblemDetail(ex, HttpStatus.FORBIDDEN, "Account Inactive", req.getRequestURI());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex, HttpServletRequest req) {
        return createProblemDetail(ex, HttpStatus.UNAUTHORIZED, "Invalid Token", req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail problemDetail = createProblemDetail(ex, HttpStatus.BAD_REQUEST, "Validation Failed", req.getRequestURI());
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));
        problemDetail.setProperty("invalid_params", errors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("about:blank"));
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    private ProblemDetail createProblemDetail(Exception ex, HttpStatus status, String title, String uri) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        pd.setInstance(URI.create(uri));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}