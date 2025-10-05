package com.ttclub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ttclub.ratelimit.shop")
public class ShopRateLimitProperties {

    public static class Bucket {
        private int limit = 5;       // default 5
        private int windowSec = 900; // default 15 minutes
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public int getWindowSec() { return windowSec; }
        public void setWindowSec(int windowSec) { this.windowSec = windowSec; }
    }

    private Bucket guestPaymentIntents = new Bucket();

    public Bucket getGuestPaymentIntents() { return guestPaymentIntents; }
    public void setGuestPaymentIntents(Bucket guestPaymentIntents) {
        this.guestPaymentIntents = guestPaymentIntents;
    }
}
