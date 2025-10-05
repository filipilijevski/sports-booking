package com.ttclub.backend.dto;

public class ShippingAddressDto {

    private String fullName;
    private String phone;
    private String email;
    private String line1;
    private String line2;
    private String city;
    private String province;
    private String postalCode;
    private String country;

    public String getFullName()            { return fullName; }
    public void setFullName(String f)      { this.fullName = f; }
    public String getPhone()               { return phone; }
    public void setPhone(String p)         { this.phone = p; }
    public String getEmail()               { return email; }
    public void setEmail(String e)         { this.email = e; }
    public String getLine1()               { return line1; }
    public void setLine1(String l)         { this.line1 = l; }
    public String getLine2()               { return line2; }
    public void setLine2(String l)         { this.line2 = l; }
    public String getCity()                { return city; }
    public void setCity(String c)          { this.city = c; }
    public String getProvince()            { return province; }
    public void setProvince(String p)      { this.province = p; }
    public String getPostalCode()          { return postalCode; }
    public void setPostalCode(String p)    { this.postalCode = p; }
    public String getCountry()             { return country; }
    public void setCountry(String c)       { this.country = c; }
}
