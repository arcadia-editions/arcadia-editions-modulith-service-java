package com.arcadiaeditions.common;

import java.util.UUID;

/** Business ids of the form PREFIX-UUID (e.g. ORD-3f2a...). */
public final class BusinessIds {

    private BusinessIds() {}

    public static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
