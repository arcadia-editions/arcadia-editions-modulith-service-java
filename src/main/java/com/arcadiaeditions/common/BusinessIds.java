package com.arcadiaeditions.common;

import java.util.UUID;

/**
 * Generates business ids as {@code <PREFIX>-<UUID v4 in upper case>} (research R-08).
 * Prefixes: {@code ORD} (order), {@code RES} (reservation), {@code FUL} (fulfillment),
 * {@code NOT} (notification). {@code Payment} has no business id.
 */
public final class BusinessIds {

    private BusinessIds() {}

    public static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().toUpperCase();
    }
}
