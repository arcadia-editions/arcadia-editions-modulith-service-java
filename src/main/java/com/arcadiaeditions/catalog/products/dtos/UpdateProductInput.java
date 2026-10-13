package com.arcadiaeditions.catalog.products.dtos;

import com.arcadiaeditions.catalog.products.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * UpdateProductInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class UpdateProductInput implements Serializable {

    private String name;

    private String description;

    private BigDecimal price;

    private String currency;

    private Instant launchAt;

    public String getName() {
        return name;
    }

    public UpdateProductInput setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public UpdateProductInput setDescription(String description) {
        this.description = description;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public UpdateProductInput setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public UpdateProductInput setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Instant getLaunchAt() {
        return launchAt;
    }

    public UpdateProductInput setLaunchAt(Instant launchAt) {
        this.launchAt = launchAt;
        return this;
    }
}
