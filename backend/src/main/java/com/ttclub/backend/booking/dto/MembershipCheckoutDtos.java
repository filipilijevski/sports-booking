package com.ttclub.backend.booking.dto;

public class MembershipCheckoutDtos {

    /** Request body: which plan to buy. */
    public static class CheckoutReq {
        public Long planId;
    }

    /** Response for the client to confirm payment. */
    public static class CheckoutResp {
        public Long   bookingId;
        public String clientSecret;

        // provide server-side price breakdown so UI never guesses tax
        public Double priceCad;
        public Double taxCad;
        public Double totalCad;
        public String currency = "CAD";

        public CheckoutResp(Long bookingId, String clientSecret) {
            this.bookingId   = bookingId;
            this.clientSecret = clientSecret;
        }

        public CheckoutResp(Long bookingId, String clientSecret,
                            Double priceCad, Double taxCad, Double totalCad, String currency) {
            this.bookingId    = bookingId;
            this.clientSecret = clientSecret;
            this.priceCad     = priceCad;
            this.taxCad       = taxCad;
            this.totalCad     = totalCad;
            if (currency != null) this.currency = currency;
        }
    }
}
