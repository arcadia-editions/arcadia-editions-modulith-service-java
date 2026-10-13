package com.arcadiaeditions.orders.checkout.core.domain.events;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.OrderConfirmedShippingAddress")
public class OrderConfirmedShippingAddress implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String line1;

    private String line2;

    private String city;

    private String region;

    private String postalCode;

    private String country;

    public String getName() {
        return name;
    }

    public OrderConfirmedShippingAddress setName(String name) {
        this.name = name;
        return this;
    }

    public String getLine1() {
        return line1;
    }

    public OrderConfirmedShippingAddress setLine1(String line1) {
        this.line1 = line1;
        return this;
    }

    public String getLine2() {
        return line2;
    }

    public OrderConfirmedShippingAddress setLine2(String line2) {
        this.line2 = line2;
        return this;
    }

    public String getCity() {
        return city;
    }

    public OrderConfirmedShippingAddress setCity(String city) {
        this.city = city;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public OrderConfirmedShippingAddress setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public OrderConfirmedShippingAddress setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public OrderConfirmedShippingAddress setCountry(String country) {
        this.country = country;
        return this;
    }
}
