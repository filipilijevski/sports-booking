package com.ttclub.backend.service.exceptions;

public class RateLimitedException extends RuntimeException {
    private final long retryAfterSec;

    public RateLimitedException(long retryAfterSec) {
        super("Too many requests");
        this.retryAfterSec = retryAfterSec;
    }

    public long getRetryAfterSec() {
        return retryAfterSec;
    }
}
