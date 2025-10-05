package com.ttclub.backend.service;

import com.ttclub.backend.service.exceptions.RateLimitedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight fixed-window rate limiter.
 * Key format is caller-defined; we recommend including IP + email/userId + deviceId.
 * In prod, we will swap this for a distributed implementation (Redis).
 */
@Service
public class RateLimitService {

    private static final class Window {
        volatile long windowEndEpochSec;
        volatile int  count;
        Window(long end, int c) { windowEndEpochSec = end; count = c; }
    }

    private final Map<String, Window> store = new ConcurrentHashMap<>();

    /**
     * Check and increment the counter for (bucketKey, windowSec).
     * Throws RateLimitedException with retryAfterSec if exceeded.
     */
    public void check(String purpose, String bucketKey, int limit, int windowSec) {
        Objects.requireNonNull(bucketKey, "bucketKey");
        final long now = Instant.now().getEpochSecond();
        final String key = purpose + "|" + bucketKey + "|" + windowSec;

        store.compute(key, (k, w) -> {
            if (w == null || now >= w.windowEndEpochSec) {
                // new window
                return new Window(now + windowSec, 1);
            }
            if (w.count >= limit) {
                long retryAfter = Math.max(1, w.windowEndEpochSec - now);
                throw new RateLimitedException(retryAfter);
            }
            w.count++;
            return w;
        });
    }
}
