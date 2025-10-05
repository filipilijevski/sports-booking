package com.ttclub.backend.model;

/**
 * Internal abstraction of the available checkout options.<br>
 * {@code canadaPostCode} is the exact service-code we pass to the
 * Canada Post API when requesting a quote
 * (e.g. DOM.RP = Regular Parcel, DOM.EP = Expedited Parcel).
 */
public enum ShippingMethod {

    /** maps to Regular Parcel*/
    REGULAR("DOM.RP"),

    /** maps to Expedited/Xpresspost */
    EXPRESS("DOM.EP");

    private final String canadaPostCode;
    ShippingMethod(String code) { this.canadaPostCode = code; }

    public String getCanadaPostCode() { return canadaPostCode; }
}
