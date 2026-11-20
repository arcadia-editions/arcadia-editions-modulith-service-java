package com.arcadiaeditions.fulfillment.shipping;

/**
 * markReadyToShip refused: the Fulfillment has no paymentCapturedAt yet, the warehouse ships only after capture.
 */
public class PaymentNotCapturedException extends RuntimeException {

    public PaymentNotCapturedException(String fulfillmentId) {
        super("Fulfillment " + fulfillmentId + " cannot be marked ready to ship: payment not captured yet");
    }
}
