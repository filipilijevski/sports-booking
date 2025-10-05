package com.ttclub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ttclub.ratelimit.auth")
public class AuthRateLimitProperties {

    public static class Bucket {
        private int limit = 10;
        private int windowSec = 1800;
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public int getWindowSec() { return windowSec; }
        public void setWindowSec(int windowSec) { this.windowSec = windowSec; }
    }

    private Bucket login = new Bucket();
    private Bucket mfaVerify = new Bucket();
    private Bucket sendCode = new Bucket();
    private Bucket passwordReset = new Bucket();

    public AuthRateLimitProperties() {
        this.login.limit = 10;           this.login.windowSec = 1800; // 10 / 30m
        this.mfaVerify.limit = 7;        this.mfaVerify.windowSec = 600;  // 7 / 10m
        this.sendCode.limit = 6;         this.sendCode.windowSec = 900;   // 6 / 15m
        this.passwordReset.limit = 6;    this.passwordReset.windowSec = 1800; // 6 / 30m
    }

    public Bucket getLogin()         { return login; }
    public void setLogin(Bucket b)   { this.login = b; }
    public Bucket getMfaVerify()     { return mfaVerify; }
    public void setMfaVerify(Bucket b){ this.mfaVerify = b; }
    public Bucket getSendCode()      { return sendCode; }
    public void setSendCode(Bucket b){ this.sendCode = b; }
    public Bucket getPasswordReset() { return passwordReset; }
    public void setPasswordReset(Bucket b){ this.passwordReset = b; }
}
