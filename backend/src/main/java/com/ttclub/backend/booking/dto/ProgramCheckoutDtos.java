package com.ttclub.backend.booking.dto;

public class ProgramCheckoutDtos {

    /** Request body for starting a program checkout. */
    public static class CheckoutReq {
        public Long programId;   // optional (derived from package), kept for parity with frontend
        public Long packageId;   // required
    }

    /** Response for hosted checkout - frontend only needs the URL */
    public static class CheckoutResp {
        public String sessionId;
        public String url;

        // useful for displaying a review/summary someday
        public Double priceCad;
        public Double taxCad;
        public Double totalCad;
        public String currency;

        public CheckoutResp() { }

        public CheckoutResp(String sessionId, String url) {
            this.sessionId = sessionId;
            this.url = url;
        }
    }
}
