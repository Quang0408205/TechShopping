package com.example.Tech.entity.aftersales;

/** The three kinds of after-sales request; also the request_type column of service_requests_view. */
public enum ServiceRequestType {
    WARRANTY("BH"),
    MAINTENANCE("BT"),
    RETURN("DT");

    private final String codePrefix;

    ServiceRequestType(String codePrefix) {
        this.codePrefix = codePrefix;
    }

    /** Display code, e.g. BH000012 (no extra column, like order codes). */
    public String code(Long id) {
        return codePrefix + "%06d".formatted(id);
    }
}
