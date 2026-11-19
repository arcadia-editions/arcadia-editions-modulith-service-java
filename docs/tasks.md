# PlaceOrderFlow: tasks

flow: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl
baseline: baseline/place-order-flow-4
branch: afk/place-order-flow

<!-- Built by zenwave-tasks-from-code-and-models on 2026-11-17: the delta from baseline/place-order-flow-2 (board archived
     in docs/tasks/place-order-flow-baseline-2.md, T-01..T-47; T-35 reservation timer was out of scope there) to models
     service-all 0.10.3 (0.10.1 + enum ids, stored as integers by converters; P-11 A; P-12 A). Code regenerated with zenwave-regenerate (.zenwave/handoff/place-order-flow/regen-reference-0.10.1.patch):
     compile stubs are marked "baseline/place-order-flow-3 compile stub"; listener left out: PaymentRetried -> reauthorizePayment
     (T-54); there is no ReservationExpired scheduler yet (T-63). Test data: data.sql header, chains ORD-001..ORD-028,
     stand-in triggers pm_* / postal 00000 / @fail.invalid, pm_void_fail new, fixed now 2026-10-14T10:00:00Z.
     Known failing at the baseline (overlay): voidPaymentFrom{Failed,AuthorizationRequested}Test (T-56). Limits from the
     ZFL header: N = 3 attempts, T = 30 s processor timeout, backoff 5 s before attempt 2 and 15 s before attempt 3.
     Flow tests are methods of PlaceOrderFlowTest. Paths unchanged from baseline 2 (stockGone, declined) have no tasks.
     2026-11-19: learnings of the Kotlin AFK rounds 3 and 4 added to the tasks they concern, marked (Kotlin T-nn): P-10 A
     (a throwing processor is FAILED processor_error), the JPA stale merge in reauthorizePayment, test-quality fixes.
     Kotlin T-84 (backoff and processor call hold a listener thread): accepted for the demo, no task. Flow tests that
     need @DirtiesContext go to a class of their own: Spring Modulith binds the Scenario listener to the first context
     of a class only (Kotlin T-82). -->

## S1 completed (tracer bullet)
path: StartOrderCheckout > startOrderCheckout > reserveStock[StockReservedResult] > OrderCreated > authorizePayment[authorized]
      > PaymentAuthorized > confirmOrder[confirmed] > confirmStockReservation > OrderConfirmed > scheduleFulfillment[FulfillmentScheduled]
      > capturePayment[PaymentCaptured] > recordPaymentCaptured, sendOrderConfirmation[sent] > end completed
review: [x] (review-S1 PASS 2026-11-20, claude-opus fresh context; P-03 opened)

- [x] T-48 advanced AFK authorizePayment: commit AUTHORIZATION_REQUESTED (attemptCount 1) in its own transaction before calling the provider, so a call in flight counts as started for getPaymentStatus; a provider call longer than 30 s ends as FAILED with PaymentFailed (processor_timeout); a provider call that throws ends as FAILED with PaymentFailed (processor_error, P-10 A of the Kotlin service, data.sql assumption) instead of leaving the Payment AUTHORIZATION_REQUESTED for good; one constant per reason shared with the stand-in processor; shut the processor-call executor down (@PreDestroy) (Kotlin T-75)
      done: claude-opus opus
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:42-69, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:107-122, ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:119-128
      test data: data.sql ORD-002, data/jpa/Payment/2.json
      tests: PaymentsProcessingServiceTest
- [x] T-49 bounded AFK recordPaymentCaptured: on PaymentCaptured set Fulfillment.paymentCapturedAt (SCHEDULED, no state change, no event); no-op when no Fulfillment exists or it is already set (replace the no-op compile stub)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:215-221, ../arcadia-editions-modulith-api/fulfillment-shipping-domain-model.zdl:105-110
      test data: data.sql ORD-023, data.sql ORD-001
      tests: FulfillmentShippingServiceTest
- [x] T-50 bounded AFK markReadyToShip: refused while Fulfillment.paymentCapturedAt is null (the warehouse ships only after capture); unchanged otherwise. The refusal names its cause (payment not captured yet), not an InvalidStateTransitionException from SCHEDULED with allowedStates [SCHEDULED]; FulfillmentTransitions is generated, do not edit it (Kotlin T-76)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:51-56, ../arcadia-editions-modulith-api/fulfillment-shipping-domain-model.zdl:112-114
      test data: data.sql ORD-006, data.sql ORD-023, data.sql ORD-001
      tests: FulfillmentShippingServiceTest
      blocked by: T-49
- [x] T-51 advanced AFK Flow test completed: the Payment passes through AUTHORIZATION_REQUESTED before AUTHORIZED, the Fulfillment records paymentCapturedAt, markReadyToShip works only after it; ends at OrderConfirmationSent
      done: claude-opus opus
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:336-345, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:202-221
      test data: data.sql ORD-001
      tests: PlaceOrderFlowTest
      blocked by: T-48, T-50

## S2 retry loop (retried, retry-exhausted)
path: OrderCreated > authorizePayment[failed] > PaymentFailed > retryPayment[retried] > PaymentRetried > reauthorizePayment[authorized|declined|failed]
      > ... > retryPayment[retry-exhausted] > PaymentRetryExhausted > releaseStock > StockReleased > cancelOrder > OrderCancelled
      > sendOrderCancelledNotification > end orderCancelled
review: [ ]

- [x] T-52 bounded AFK retryPayment: Optional.empty(), no event, when there is no Payment (P-12 A); no-op, no event, unless the Payment is FAILED and input.attemptCount == Payment.attemptCount (redelivery); never increments attemptCount; retried below 3 attempts, retry-exhausted at 3
      done: claude-sonnet sonnet (2 fix rounds)
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:124-136, ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:138-145
      test data: data.sql ORD-008, data.sql ORD-009
      tests: PaymentsProcessingServiceTest
- [x] T-53 advanced AFK reauthorizePayment: from FAILED with input.attemptCount == Payment.attemptCount, wait the backoff (5 s before attempt 2, 15 s before attempt 3), increment attemptCount and commit AUTHORIZATION_REQUESTED before the provider call (as T-48; ZDL transitions FAILED -> AUTHORIZATION_REQUESTED -> outcome, P-11 A), 30 s timeout; guard on FAILED in the service, not with the generated PaymentTransitions.ensureCanReauthorizePayment (it checks the last step's from); outcomes AUTHORIZED / DECLINED / FAILED with their events; Optional.empty() when there is no Payment (P-12 A); any other state or a stale attemptCount is a no-op (replace the compile stub). On JPA, do not save a Payment loaded before the REQUIRES_NEW block over the one that block committed (optimistic-lock failure on attempts 2 and 3): load and check inside the block, or re-read after it; a test with @Version semantics fails on the bug; a provider exception is FAILED processor_error as in T-48 (Kotlin T-80, T-75)
      done: claude-opus opus
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:42-69, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:138-146, ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:130-136
      test data: data.sql ORD-008, data.sql ORD-009
      tests: PaymentsProcessingServiceTest
      blocked by: T-48, T-52
- [x] T-54 mechanical AFK Wire PaymentRetried to reauthorizePayment (PaymentsProcessingServiceEventsListener + asReauthorizePaymentInput, as the 0.10.1 generator writes them) and make authorizePayment creation only: drop the branch that reuses a FAILED Payment on PaymentRetried
      done: claude-sonnet sonnet
      model: ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:119-136
      test data: data.sql ORD-002, data.sql ORD-008
      tests: PaymentsProcessingServiceTest
      blocked by: T-53
- [/] T-55 advanced AFK Flow tests of the retry loop: attempts counted only by authorizePayment (1) and reauthorizePayment (2, 3), a redelivered PaymentFailed or PaymentRetried with a stale attemptCount changes nothing, backoff respected (fixed Clock or a test backoff)
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:124-146, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:254-266
      test data: data.sql ORD-008, data.sql ORD-009, data.sql ORD-015
      tests: PlaceOrderFlowTest
      blocked by: T-54

## S3 orderCancelled (void refused)
path: OrderConfirmed > scheduleFulfillment[FulfillmentFailed] > voidPayment[void-failed] > PaymentVoidFailed > releaseStock > StockReleased
      > cancelOrder > OrderCancelled > sendOrderCancelledNotification > end orderCancelled
review: [ ]

- [x] T-56 bounded AFK voidPayment: Optional.empty(), no event, when there is no Payment or it is not AUTHORIZED or CAPTURE_FAILED (rewrite voidPaymentFromFailedTest and voidPaymentFromAuthorizationRequestedTest to assert the no-op); a refused void sets VOID_FAILED and voidFailedAt and emits PaymentVoidFailed; the stand-in refuses voids for paymentMethodId pm_void_fail. The no-op tests compare fields (status, failureReason, voidedAt, voidFailedAt), not the entity (equals is by id); the refused-void test asserts failureReason and PaymentVoidFailed.reason; log.warn the provider's refusal reason (ops own VOID_FAILED) (Kotlin T-78)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:236-252, ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:152-161, ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:243-249
      test data: data.sql ORD-002, data.sql ORD-007, data.sql ORD-008, data.sql ORD-012, data.sql ORD-019, data.sql ORD-025
      tests: PaymentsProcessingServiceTest
- [x] T-57 mechanical AFK releaseStock on PaymentVoidFailed (listener and mapper already wired): reason PAYMENT_VOID_FAILED from a CONFIRMED reservation, sold back to available
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:254-266, ../arcadia-editions-modulith-api/catalog-inventory-domain-model.zdl:142-148
      test data: data.sql ORD-025, data.sql ORD-026
      tests: InventoryServiceTest
- [x] T-58 bounded AFK sendOrderCancelledNotification: when the reason is PAYMENT_VOID_FAILED the e-mail says the hold on the card may still show until we release it (the ZFL: ops release the hold by hand), not "for a few days" (the person, 2026-11-18, Kotlin T-79)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-modulith-api/notifications-consumer-domain-model.zdl:96-98, ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:152-161
      test data: data.sql ORD-026
      tests: NotificationsConsumerServiceTest
- [ ] T-59 advanced AFK Flow test void refused: fulfillment fails, the void is refused, the stock is released (PAYMENT_VOID_FAILED), the order is cancelled once and the customer told; ends at OrderCancelledNotificationSent with the Payment VOID_FAILED
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:236-266, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:299-309, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:322-345
      test data: data.sql ORD-026
      tests: PlaceOrderFlowTest
      blocked by: T-56, T-57, T-58

## S4 orderCancelled (capture failed, fulfillment cancelled)
path: FulfillmentScheduled > capturePayment[PaymentCaptureFailed] > voidPayment[voided] > PaymentVoided > releaseStock > StockReleased
      > cancelOrder > OrderCancelled > cancelFulfillment > FulfillmentCancelled, sendOrderCancelledNotification > end orderCancelled
review: [ ]

- [x] T-60 bounded AFK cancelFulfillment: on OrderCancelled, SCHEDULED or READY_TO_SHIP becomes CANCELLED with cancelledAt and cancellationReason = OrderCancelled.reason, emits FulfillmentCancelled; Optional.empty(), no event, when there is no Fulfillment or it is CANCELLED or FAILED (replace the no-op compile stub)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:311-320, ../arcadia-editions-modulith-api/fulfillment-shipping-domain-model.zdl:116-119, ../arcadia-editions-modulith-api/fulfillment-shipping-domain-model.zdl:151-157
      test data: data.sql ORD-013, data.sql ORD-014, data.sql ORD-024
      tests: FulfillmentShippingServiceTest
- [ ] T-61 bounded AFK Flow test capture failed: after the void and the cancellation the Fulfillment is CANCELLED and FulfillmentCancelled was emitted; the order is cancelled once: record OrderCancelled and assert exactly one for the orderId (Kotlin T-85)
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:190-200, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:311-320
      test data: data.sql ORD-024
      tests: PlaceOrderFlowTest
      blocked by: T-56, T-60

## S5 orderCancelled (reservation expired, query timer)
path: ReservationExpired (10 minutes after OrderCreated) > expireReservation > getPaymentStatus[NoPaymentResult] > StockReleased > cancelOrder
      > OrderCancelled > sendOrderCancelledNotification > end orderCancelled; getPaymentStatus[PaymentStartedResult]: no release, payment owns the end
review: [ ]

- [x] T-62 bounded AFK getPaymentStatus: read-only; NO_PAYMENT when no Payment exists for orderId, STARTED otherwise (AUTHORIZATION_REQUESTED included) (replace the compile stub)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:293-297, ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:163-164
      test data: data.sql ORD-002, data.sql ORD-027, data.sql ORD-028
      tests: PaymentsProcessingServiceTest
- [/] T-63 advanced AFK expireReservation: calls getPaymentStatus and releases the RESERVED lines of the order (RESERVATION_EXPIRED, StockReleased) only on NO_PAYMENT; no-op on STARTED or when nothing is RESERVED (replace the compile stub)
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:42-69, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:280-291, ../arcadia-editions-modulith-api/catalog-inventory-domain-model.zdl:150-158
      test data: data.sql ORD-027, data.sql ORD-028, data.sql ORD-021
      tests: InventoryServiceTest
      blocked by: T-48, T-62
- [ ] T-64 advanced AFK ReservationExpired timer (reopens T-35 of baseline 2): a scheduler on StockReservation.expiresAt with the injected Clock that runs expireReservation once per order whose RESERVED reservations are past their deadline; channel reservation-expired-event-v1 as the ZDL @timer names it
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:268-278, ../arcadia-editions-modulith-api/catalog-inventory-domain-model.zdl:150-158
      test data: data.sql ORD-002, data.sql ORD-027, data.sql ORD-028
      tests: InventoryServiceTest
      blocked by: T-63
- [ ] T-65 advanced AFK Flow test reservation expired: with no payment started the stock is released and the order cancelled; with a payment started the timer releases nothing and payment ends the order
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:42-69, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:268-309
      test data: data.sql ORD-021, data.sql ORD-027, data.sql ORD-028
      tests: PlaceOrderFlowTest
      blocked by: T-64

## S6 orderCancelled (late authorization rejected)
path: PaymentAuthorized (after expiry) > confirmOrder > confirmStockReservation[StockReservationNotHeldResult] or order CANCELLED
      > OrderConfirmationRejected > voidPayment[voided] > PaymentVoided > releaseStock (no-op, RELEASED) > end orderCancelled
review: [ ]

- [x] T-66 bounded AFK confirmStockReservation NOT_HELD: when the order has no RESERVED or CONFIRMED reservation (released by the timer) answer NOT_HELD, no change, no event; CONFIRMED stays the redelivery answer
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:166-176, ../arcadia-editions-modulith-api/catalog-inventory-domain-model.zdl:161-163
      test data: data.sql ORD-003, data.sql ORD-004, data.sql ORD-022
      tests: InventoryServiceTest
- [x] T-67 bounded AFK confirmOrder rejections: a CANCELLED order emits OrderConfirmationRejected (ORDER_CANCELLED) without calling inventory; NOT_HELD emits OrderConfirmationRejected (STOCK_NOT_HELD) and the order stays CREATED; rejectedAt from the Clock
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:148-164, ../arcadia-editions-modulith-api/orders-checkout-domain-model.zdl:101-108, ../arcadia-editions-modulith-api/orders-checkout-domain-model.zdl:177-183
      test data: data.sql ORD-003, data.sql ORD-021, data.sql ORD-022
      tests: OrdersCheckoutServiceTest
      blocked by: T-66
- [x] T-68 mechanical AFK voidPayment on OrderConfirmationRejected (listener and mapper already wired): the void reason is the rejection reason
      done: claude-sonnet sonnet
      model: ../arcadia-editions-modulith-api/payments-processing-domain-model.zdl:152-161
      test data: data.sql ORD-021
      tests: PaymentsProcessingServiceTest
      blocked by: T-56
- [ ] T-69 advanced AFK Flow test late authorization: the authorization arrives after the timer released the stock; confirmOrder rejects, the hold is voided, releaseStock and cancelOrder are no-ops, the order is cancelled once
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:148-176, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:236-266, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:299-309
      test data: data.sql ORD-021, data.sql ORD-022
      tests: PlaceOrderFlowTest
      blocked by: T-64, T-67, T-68

## S7 notificationFailed
path: OrderCancelled > sendOrderCancelledNotification[failed] > NotificationDeliveryFailed > end notificationFailed
review: [ ] (review-S7 2026-11-20: PASS with SHOULD FIX, accepted as T-72)

- [x] T-70 bounded AFK Flow test notification failed: the stand-in refuses the e-mail (@fail.invalid), Notification FAILED, the flow ends at NotificationDeliveryFailed with its type; the business outcome is untouched: Fulfillment.paymentCapturedAt set, stock counters relative to the position read before the checkout (available -1, sold +1), NotificationDeliveryFailed.notificationId equals the stored Notification; no assertion that only proves the stand-in (Kotlin T-87)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:322-345
      test data: data.sql ORD-015
      tests: PlaceOrderFlowTest

- [ ] T-72 bounded AFK Flow test notification failed, cancelled branch: OrderCancelled > sendOrderCancelledNotification[failed] > NotificationDeliveryFailed (type ORDER_CANCELLED), e.g. pm_decline + @fail.invalid: order CANCELLED, reservation RELEASED, stock counters back to the position read before, Notification FAILED with the notificationId of the event. Also fix T-70's test: its comment cites the lines it runs (zfl:207-212, sendOrderConfirmation failed, fresh order), paymentCapturedAt isEqualTo Payment.capturedAt, no OrderConfirmationSent for that order; add end notificationFailed (both branches) to the class javadoc list of ends
      model: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:207-212, ../arcadia-editions-architecture/business-flows/place-order-flow.zfl:322-345
      test data: data.sql ORD-015
      tests: PlaceOrderFlowTest
      fixes: .zenwave/handoff/place-order-flow/reports/review-S7.md
      blocked by: T-55

## S8 Polish
review: [x] (review-S8 PASS 2026-11-20, claude-opus; 3 suite failures outside S8: T-56, T-61)

- [x] T-71 mechanical AFK startOrderCheckout contract: the 409 body follows the StockUnavailable schema of the 0.10.1 OpenAPI (orderId and unavailableItems, P-02 update); quantity >= 1 and unitPrice >= 0 (ZDL bounds) answer 400 before any reservation: assert StockReservation and Order counts unchanged after each 400 request and the ProblemDetail field (items[0].quantity / items[0].unitPrice), one case per test (Kotlin T-77)
      done: claude-sonnet sonnet
      model: ../arcadia-editions-modulith-api/orders-checkout-domain-model.zdl:68-69, ../arcadia-editions-modulith-api/orders-checkout-domain-model.zdl:95-99, ../arcadia-editions-modulith-api/orders-checkout-openapi-overlay.yml:10-20
      test data: data.sql ORD-020
      tests: OrdersCheckoutApiControllerTest, OrdersCheckoutApiIntegrationTest
