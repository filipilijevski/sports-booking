package com.ttclub.backend.booking.dto;

/** DTOs for inline PaymentIntent flow for program enrollments */
public class ProgramInlineDtos {

    /** Lightweight quote for a program package */
    public static class QuoteResp {
        public Double priceCad;
        public Double taxCad;
        public Double totalCad;
        public String currency;
    }

    /** Response for client to confirm a PaymentIntent inline */
    public static class PaymentIntentResp {
        public Long   bookingId;
        public String clientSecret;

        // include server-side price breakdown so the UI never guesses tax
        public Double priceCad;
        public Double taxCad;
        public Double totalCad;
        public String currency = "CAD";

        public PaymentIntentResp(Long bookingId, String clientSecret) {
            this.bookingId   = bookingId;
            this.clientSecret = clientSecret;
        }
    }
}
