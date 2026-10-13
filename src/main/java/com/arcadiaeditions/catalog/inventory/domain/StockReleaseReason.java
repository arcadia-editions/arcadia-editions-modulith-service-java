package com.arcadiaeditions.catalog.inventory.domain;


/**
 * Enum for StockReleaseReason.
 */
public enum StockReleaseReason {
    PAYMENT_DECLINED,
    PAYMENT_RETRY_EXHAUSTED,
    PAYMENT_VOIDED,
    RESERVATION_EXPIRED,
    ;
}
