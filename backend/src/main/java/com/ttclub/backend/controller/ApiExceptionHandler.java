package com.ttclub.backend.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ttclub.backend.service.exceptions.RateLimitedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static record ApiError(String code, String message, Map<String, String> fieldErrors) { }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<Map<String,Object>> rateLimited(RateLimitedException ex) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("code", "RATE_LIMITED");
        body.put("message", "Too many requests. Try again later.");
        body.put("retryAfterSec", ex.getRetryAfterSec());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(value = { com.ttclub.backend.booking.service.DuplicateEnrollmentException.class })
    public ResponseEntity<ApiError> duplicateEnrollment(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("DUPLICATE_ENROLLMENT",
                        "You already have an active enrollment for this program.",
                        null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", "One or more fields are invalid.", fields));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException ex) {
        String msg = (ex.getRootCause() != null ? ex.getRootCause().getMessage() : "").toLowerCase();
        if (msg.contains("users_email_key") || msg.contains("idx_users_email") || msg.contains("uq_users_email_normalized")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("EMAIL_EXISTS", "There is already an account registered with this email.", null));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("DATA_INTEGRITY", "Operation violates a data constraint.", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        String msg = raw.toLowerCase();

        if (msg.contains("bad credentials")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("INVALID_CREDENTIALS", "Incorrect username or password.", null));
        }
        if (msg.contains("email") && msg.contains("already") && msg.contains("exist")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("EMAIL_EXISTS", "There is already an account registered with this email.", null));
        }
        if (msg.contains("invalid refreshtoken")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("INVALID_REFRESH_TOKEN", "Invalid session. Please sign in again.", null));
        }
        if (msg.contains("refresh token expired")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("REFRESH_EXPIRED", "Your session expired. Please sign in again.", null));
        }
        if (msg.contains("verificationcode is required")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("MISSING_VERIFICATION_CODE", "Please enter the 6-digit verification code.", null));
        }
        if (msg.contains("invalid verification code") || msg.contains("verification code expired") || msg.contains("no verification code requested")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("INVALID_VERIFICATION_CODE",
                            "The verification code is invalid or expired.",
                            Map.of("verificationCode", "Invalid or expired verification code.")));
        }
        if (msg.contains("invalid registration code")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("INVALID_ROLE_CODE", "The registration code is invalid for the requested role.", null));
        }
        if (msg.contains("email cannot be blank")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("INVALID_EMAIL", "Email cannot be blank.", Map.of("email", "Email cannot be blank.")));
        }
        if (msg.contains("passwords do not match")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("PASSWORDS_DO_NOT_MATCH", "Passwords do not match.", Map.of("confirmPassword", "Passwords do not match.")));
        }
        if (msg.contains("password must be at least")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("WEAK_PASSWORD", raw, Map.of("newPassword", raw)));
        }
        if (msg.contains("user not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("NOT_FOUND", "User not found.", null));
        }
        if (msg.contains("email is managed by oauth2 provider") || (msg.contains("oauth") && msg.contains("managed"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("OAUTH_MANAGED", "Email is managed by OAuth2 provider and cannot be changed.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("BAD_REQUEST", raw.isBlank() ? "Bad request." : raw, null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> illegalState(IllegalStateException ex) {
        String msg = ex.getMessage() == null ? "Illegal state" : ex.getMessage();
        String lower = msg.toLowerCase();

        if (lower.contains("mfa already enabled")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("MFA_ALREADY_ENABLED", "Two-factor authentication is already enabled.", null));
        }
        if (lower.contains("mfa setup required")) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body(new ApiError("MFA_SETUP_REQUIRED", "MFA setup is required before enabling.", null));
        }
        if (lower.contains("mfa not enabled")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("MFA_NOT_ENABLED", "Two-factor authentication is not enabled for this account.", null));
        }
        if (lower.contains("invitation only") || lower.contains("admin enrollment")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("INVITE_ONLY", "This program is by invitation only (admin enrollment required).", null));
        }
        if (lower.contains("initial") && lower.contains("membership") && lower.contains("required")) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(new ApiError("INITIAL_MEMBERSHIP_REQUIRED", "Initial/Annual Club Membership is required.", null));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("ILLEGAL_STATE", msg, null));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> security(SecurityException ex) {
        String msg = ex.getMessage() == null ? "Authentication required." : ex.getMessage();
        String lower = msg.toLowerCase();

        if (lower.contains("mfa token invalid")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("MFA_TOKEN_INVALID", "Your MFA session has expired. Please sign in again.", null));
        }
        if (lower.contains("too many attempts")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiError("MFA_TOO_MANY_ATTEMPTS", "Too many invalid codes. Please sign in again.", null));
        }
        if (lower.contains("invalid mfa code")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("INVALID_MFA_CODE", "The code is invalid or expired.", Map.of("code", "Invalid or expired code")));
        }
        if (lower.contains("invalid password or recovery code")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("INVALID_MFA_DISABLE", "Invalid password or recovery code.", null));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("AUTH_REQUIRED", msg, null));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ApiError> mail(MailException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("MAIL_ERROR", "We could not send the email right now. Please try again shortly.", null));
    }
}
