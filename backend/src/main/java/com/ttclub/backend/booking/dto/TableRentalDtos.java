package com.ttclub.backend.booking.dto;

public class TableRentalDtos {
    public static class PackageDto {
        public Long   id;
        public String name;
        public Double hours;
        public Double priceCad;
        public Boolean active;
        public Integer sortOrder;
    }
    public static class QuoteDto {
        public Double priceCad;
        public Double taxCad;
        public Double totalCad;
        public String currency;
    }
    public static class StartResp {
        public Long bookingId;
        public String clientSecret;
        public Double priceCad;
        public Double taxCad;
        public Double totalCad;
        public String currency;
    }
    public static class CreditSummaryDto {
        public Double hoursRemaining;
    }
    public static class AdminUserCreditRow {
        public Long id;
        public String name;
        public String email;
        public Double tableHoursRemaining;
    }
}
