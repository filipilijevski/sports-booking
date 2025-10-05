package com.ttclub.backend.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stateless SHA-256 hashing for refresh tokens (hex output).
 * Stored in DB as 64 lowercase hex chars.
 */
@Component
public class TokenHashingService {

    private final MessageDigest sha256;

    public TokenHashingService() {
        try {
            this.sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** @return 64-char lowercase hex digest, or null if input is null */
    public String hash(String raw) {
        if (raw == null) return null;
        byte[] bytes;
        synchronized (sha256) {
            sha256.reset();
            sha256.update(raw.getBytes(StandardCharsets.UTF_8));
            bytes = sha256.digest();
        }
        return toHex(bytes);
    }

    private static String toHex(byte[] b) {
        char[] out = new char[b.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        int i = 0;
        for (byte value : b) {
            int v = value & 0xFF;
            out[i++] = digits[v >>> 4];
            out[i++] = digits[v & 0x0F];
        }
        return new String(out);
    }
}
