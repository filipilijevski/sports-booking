package com.ttclub.backend.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

/**
 * Denormalised snapshot of the buyer's shipping address.
 * Stored in orders table via @AttributeOverrides.
 */
@Embeddable
public class ShippingAddress implements Serializable {

    private String fullName;
    private String phone;
    private String email;
    private String line1;
    private String line2;
    private String city;
    private String province;
    private String postalCode;
    private String country;

    public ShippingAddress() { }

    public String getFullName()               { return fullName; }
    public void setFullName(String fullName)  { this.fullName = fullName; }
    public String getPhone()                  { return phone; }
    public void setPhone(String phone)        { this.phone = phone; }
    public String getEmail()                  { return email; }
    public void setEmail(String email)        { this.email = email; }
    public String getLine1()                  { return line1; }
    public void setLine1(String line1)        { this.line1 = line1; }
    public String getLine2()                  { return line2; }
    public void setLine2(String line2)        { this.line2 = line2; }
    public String getCity()                   { return city; }
    public void setCity(String city)          { this.city = city; }
    public String getProvince()               { return province; }
    public void setProvince(String province)  { this.province = province; }
    public String getPostalCode()             { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry()                { return country; }
    public void setCountry(String country)    { this.country = country; }
}
