package com.arcadiaeditions.fulfillment.shipping.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
// @Embeddable // json embedded
@org.jmolecules.ddd.annotation.ValueObject
public class ShippingAddress implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @NotNull
    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "region")
    private String region;

    @NotNull
    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @NotNull
    @Column(name = "country", nullable = false)
    private String country;

    public String getName() {
        return name;
    }

    public ShippingAddress setName(String name) {
        this.name = name;
        return this;
    }

    public String getLine1() {
        return line1;
    }

    public ShippingAddress setLine1(String line1) {
        this.line1 = line1;
        return this;
    }

    public String getLine2() {
        return line2;
    }

    public ShippingAddress setLine2(String line2) {
        this.line2 = line2;
        return this;
    }

    public String getCity() {
        return city;
    }

    public ShippingAddress setCity(String city) {
        this.city = city;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public ShippingAddress setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public ShippingAddress setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public ShippingAddress setCountry(String country) {
        this.country = country;
        return this;
    }
}
