package com.arcadiaeditions.payments.processing.domain;


/**
 * Enum for PaymentStatus.
 */
public enum PaymentStatus {
    AUTHORIZATION_REQUESTED,
    AUTHORIZED,
    DECLINED,
    FAILED,
    RETRY_EXHAUSTED,
    CAPTURED,
    CAPTURE_FAILED,
    VOIDED,
    ;
}
