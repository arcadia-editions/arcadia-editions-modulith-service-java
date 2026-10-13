package com.arcadiaeditions.payments.processing.application.port.in;

import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Inbound Service Port for managing [Payment].
 */
@org.springframework.modulith.NamedInterface("PaymentsProcessingService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.hexagonal.PrimaryPort
public interface PaymentsProcessingService {

    /**
     *
     *
     * With Events: [[PaymentAuthorized, PaymentDeclined, PaymentFailed]].
     *
     */
    public Payment authorizePayment(AuthorizePaymentInput input);
    /**
     *
     *
     * With Events: [[PaymentRetried, PaymentRetryExhausted]].
     *
     */
    public Payment retryPayment(RetryPaymentInput input);
    /**
     *
     *
     * With Events: [[PaymentCaptured, PaymentCaptureFailed]].
     *
     */
    public Payment capturePayment(CapturePaymentInput input);
    /**
     *
     *
     * With Events: [PaymentVoided].
     *
     */
    public Payment voidPayment(VoidPaymentInput input);
}
