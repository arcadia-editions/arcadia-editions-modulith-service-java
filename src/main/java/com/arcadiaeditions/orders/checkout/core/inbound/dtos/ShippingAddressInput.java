package com.arcadiaeditions.orders.checkout.core.inbound.dtos;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ShippingAddressInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ShippingAddressInput implements Serializable {

    @NotNull
    private String name;

    @NotNull
    private String line1;

    private String line2;

    @NotNull
    private String city;

    private String region;

    @NotNull
    private String postalCode;

    @NotNull
    private String country;

    public String getName() {
        return name;
    }

    public ShippingAddressInput setName(String name) {
        this.name = name;
        return this;
    }

    public String getLine1() {
        return line1;
    }

    public ShippingAddressInput setLine1(String line1) {
        this.line1 = line1;
        return this;
    }

    public String getLine2() {
        return line2;
    }

    public ShippingAddressInput setLine2(String line2) {
        this.line2 = line2;
        return this;
    }

    public String getCity() {
        return city;
    }

    public ShippingAddressInput setCity(String city) {
        this.city = city;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public ShippingAddressInput setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public ShippingAddressInput setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public ShippingAddressInput setCountry(String country) {
        this.country = country;
        return this;
    }
}
