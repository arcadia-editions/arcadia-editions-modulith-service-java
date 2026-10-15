# Feature Specification: Place Order Flow

**Feature Branch**: `001-place-order-flow` (spec directory; no branch was created)

**Created**: 2026-10-14

**Status**: Draft (gap report verdict: READY)

**Input**: User description: "PlaceOrderFlow from `../arcadia-editions-architecture/business-flows/place-order-flow.zfl`, implemented across the bounded contexts of this modulith, using the gap report and the decisions recorded in `docs/zenwave-sdd-overlay.md`."

**Sources of truth**: The flow is defined by `place-order-flow.zfl` (ZFL: when and why) and the
`*-domain-model.zdl` files in `../arcadia-editions-modulith-api/` (ZDL: what). This spec names their
elements exactly and does not restate their structure. Missing behavior is inventoried in
`docs/zenwave-gap-report.md`, whose finding ids (G-\*, X-\*, M-\*, Q-\*) are used below.

## Clarifications

### Session 2026-10-14

Answered by the model owner while reviewing the gap report (also recorded in
`docs/zenwave-sdd-overlay.md`, "Decisions already taken"):

- Q: How does a command triggered by an event find its aggregate when the event carries only the order reference (Q-01)? → A: By `orderId`, looked up in the target context.
- Q: On a payment retry, where does the payment data come from (Q-02)? → A: From the order's one `Payment`; a retry re-authorizes the same payment with the data already stored on it (including `paymentMethodId`).
- Q: How are alternative outcomes recorded when a command declares one target state (Q-03)? → A: The declared state is the happy path; each alternative outcome sets its own lifecycle state (e.g. capture failure → `CAPTURE_FAILED`).
- Q: Is reservation expiry (`ReservationExpired`, Q-04) in scope? → A: No, deferred to a later iteration.
- Q: Is an order kept when stock is unavailable (Q-05)? → A: No. The customer is told immediately, with the unavailable items; the `StockUnavailable` notice still carries an order reference for correlation.
- Q: Where do prices come from (Q-06)? → A: For now, from the customer's request as sent; fetching them from the product catalog is a later iteration.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer buys a limited edition and is charged only when it ships (Priority: P1)

During a drop, a customer submits an order. The items are held for them immediately, their card is
authorized (not charged), the order is confirmed and handed to fulfillment, and the money is
captured only once fulfillment is scheduled. The customer then receives an order confirmation.
ZFL outcome: `completed` (`OrderConfirmationSent`).

**Why this priority**: This is the reason the flow exists: selling scarce stock without overselling
and without charging customers for orders that never ship.

**Independent Test**: Place an order for available stock with a card that authorizes and captures
successfully; verify the order ends confirmed, stock is sold, payment is captured and exactly one
confirmation notice is sent.

**Acceptance Scenarios**:

1. **Given** enough stock for every line, **When** the customer starts checkout (`startOrderCheckout`), **Then** the stock for every line is reserved together, the order is created (`OrderCreated`) and the customer receives an order reference.
2. **Given** a created order, **When** the payment is authorized (`PaymentAuthorized`), **Then** the order is confirmed (`confirmOrder`), every reservation of the order is confirmed (`confirmStockReservation`) and fulfillment is scheduled (`scheduleFulfillment`).
3. **Given** fulfillment is scheduled (`FulfillmentScheduled`), **When** capture succeeds (`PaymentCaptured`), **Then** the customer receives one order confirmation (`sendOrderConfirmation` → `OrderConfirmationSent`).
4. **Given** a transient authorization failure (`PaymentFailed`) with attempts left, **When** the retry runs (`retryPayment` → `PaymentRetried`), **Then** authorization is attempted again on the same payment and, if it succeeds, the flow continues as in scenario 2.

---

### User Story 2 - Customer is told immediately when stock is gone (Priority: P2)

A customer submits an order but at least one item is no longer available. Nothing is held, no
payment is attempted and no order is kept; the customer is told right away which items are
unavailable, and also receives a notice. ZFL outcome: `stockGone` (`StockUnavailableNotificationSent`).

**Why this priority**: In a drop most late customers hit this path; a fast, clear answer protects
trust and avoids phantom orders.

**Independent Test**: Request more units than are available; verify the request is refused with the
unavailable items, no stock moved, no order or payment exists, and one stock-unavailable notice is sent.

**Acceptance Scenarios**:

1. **Given** one line exceeds the available quantity, **When** the customer starts checkout, **Then** no line is reserved (all or nothing), no order is kept, and the response lists each unavailable item with requested and available quantities.
2. **Given** that refusal, **Then** the customer receives one stock-unavailable notice (`StockUnavailable` → `sendStockUnavailableNotification`).

---

### User Story 3 - Order is cancelled and stock returned when payment cannot be authorized (Priority: P3)

The card is declined, or transient failures continue until no attempts are left. The held stock
goes back to the pool, the order is cancelled and the customer is told why. ZFL outcome:
`orderCancelled` (`OrderCancelledNotificationSent`).

**Why this priority**: Without it, failed payments would lock scarce stock and leave customers uninformed.

**Independent Test**: Place an order with a card that declines; verify stock is returned, the order
is cancelled and one cancellation notice with the reason is sent. Repeat with a card that always
fails transiently.

**Acceptance Scenarios**:

1. **Given** a created order, **When** the card is declined (`PaymentDeclined`), **Then** no retry happens, all of the order's stock is released (`releaseStock`, reason payment declined), the order is cancelled (`cancelOrder`) and the customer is notified with the reason (`sendOrderCancelledNotification`).
2. **Given** transient failures, **When** the attempt limit is reached (`PaymentRetryExhausted`), **Then** the same release, cancellation and notice happen, with reason retry exhausted.

---

### User Story 4 - Authorization is voided, never charged, when fulfillment or capture fails (Priority: P4)

After authorization, fulfillment cannot be arranged or the capture fails. The authorization hold is
voided (so no charge appears), the stock is returned, the order is cancelled and the customer is told.

**Why this priority**: It protects customers from charges for orders that will not ship; it is a
rarer path than payment failure.

**Independent Test**: Force a fulfillment failure after authorization; verify the payment is voided
and never captured, stock is returned, the order is cancelled and one notice is sent. Repeat forcing
a capture failure.

**Acceptance Scenarios**:

1. **Given** a confirmed order, **When** fulfillment fails (`FulfillmentFailed`), **Then** the payment is voided (`voidPayment` → `PaymentVoided`), the stock is released (reason payment voided), the order is cancelled and the customer is notified.
2. **Given** fulfillment is scheduled, **When** capture fails (`PaymentCaptureFailed`), **Then** the payment is marked capture-failed, then voided, and the same release, cancellation and notice happen.

---

### User Story 5 - Customer checks the order (Priority: P5)

After submitting, the customer can look up the order by its reference to see whether it is created,
confirmed or cancelled (and why).

**Why this priority**: The flow continues after the initial response; the customer needs a way to
see the outcome.

**Independent Test**: Place an order, then read it while the flow progresses; verify the status
matches the latest outcome.

**Acceptance Scenarios**:

1. **Given** an existing order, **When** the customer requests it (`getOrder`), **Then** they see its current status and, if cancelled, the reason.
2. **Given** an unknown order reference, **When** the customer requests it, **Then** they are told it does not exist.

---

### Edge Cases

- The same step is triggered twice for the same order: no second payment, no second capture, no double stock movement, no duplicate notice; a command that finds its aggregate already in the target state does nothing.
- Two customers compete for the last units of the same item at the same moment: at most the available quantity is reserved; one of them gets the stock-unavailable answer.
- A request with no lines, a non-positive quantity, a currency code longer than 3 characters or a missing shipping address is rejected before any stock is touched.
- A command refers to an order or payment that does not exist: reported as not found, never as a server error.
- A command is not allowed from the aggregate's current state (for example, capturing a payment that is not authorized): reported as a conflict, and nothing changes.
- A void arrives for a payment that was never authorized: the payment is voided without contacting the payment provider.
- Several reservations belong to one order: release and confirmation act on all of them and produce one order-level notice (`StockReleased`, `StockReservationConfirmed`).
- A notice cannot be delivered: the delivery failure is recorded (`NotificationDeliveryFailed`); the order outcome does not change.

## Requirements *(mandatory)*

### Functional Requirements

**Checkout and stock (orders-checkout, catalog-inventory)**

- **FR-001**: System MUST accept a checkout request with customer, contact e-mail, payment method, currency, one or more lines (item, quantity, unit price) and a shipping address, and reject invalid requests before touching stock.
- **FR-002**: System MUST reserve stock for all lines of an order or for none of them, and MUST never reserve more than the available quantity of an item. (G-CI-01, X-04)
- **FR-003**: When every line is reserved, System MUST keep the order as created, with an order reference and a total equal to the sum of quantity × unit price, and announce it (`OrderCreated`). (G-OC-01, G-OC-03)
- **FR-004**: When any line cannot be reserved, System MUST NOT keep an order, MUST answer the request with the unavailable items (requested and available quantities), and MUST announce `StockUnavailable` with those items. (G-OC-01, G-OC-04)

**Payment (payments-processing)**

- **FR-005**: For each created order, System MUST create exactly one payment and request authorization from the payment provider, recording exactly one outcome: authorized, declined or failed. (G-PP-01, X-02)
- **FR-006**: A declined authorization MUST NOT be retried. A failed authorization MUST be retried on the same payment until the attempt limit is reached, then the payment MUST be marked as retry exhausted. (G-PP-02)
- **FR-007**: When fulfillment is scheduled, System MUST capture the authorized amount; on failure the payment MUST be marked capture-failed and no money moved. (G-PP-03)
- **FR-008**: When fulfillment fails or capture fails, System MUST void the authorization; a payment that was never authorized MUST be voided without contacting the provider. (G-PP-04)

**Confirmation, fulfillment and cancellation**

- **FR-009**: On authorization, System MUST confirm the order and confirm every stock reservation of that order (stock moves from reserved to sold). (G-OC-02, G-CI-02)
- **FR-010**: On order confirmation, System MUST schedule fulfillment and record exactly one outcome: scheduled or failed. (G-FS-01)
- **FR-011**: On payment declined, retry exhausted or payment voided, System MUST release all of the order's stock back to the pool with the matching reason and announce one `StockReleased` per order. (G-CI-03, G-CI-04, G-CI-06)
- **FR-012**: On stock released, System MUST cancel the order, recording when and why. (G-OC-05)

**Notifications (notifications-consumer)**

- **FR-013**: System MUST send the customer exactly one notice per terminal outcome: order confirmation after capture, stock unavailable, or order cancelled with its reason; delivery failures MUST be recorded. (G-NC-01..04, X-03)

**Query**

- **FR-014**: Customers MUST be able to read an order by its reference, including status and cancellation reason. (`getOrder`)

**Cross-cutting**

- **FR-015**: Every event-driven step MUST be safe to run more than once for the same order (no duplicate payment, capture, stock movement or notice). (X-01)
- **FR-016**: Event-driven steps MUST find their order, payment or reservations by the order reference carried by the event. (G-OC-07, G-PP-07, G-CI-07; G-OC-06 and G-PP-05 resolved in the models)
- **FR-017**: Requests for unknown orders MUST be answered as not found, and disallowed state changes as conflicts. (X-05)
- **FR-018**: Each terminal outcome of the ZFL `end` block (`completed`, `stockGone`, `orderCancelled` through each failure path) MUST be covered by an automated end-to-end check. (G-FT-01..06)

### Key Entities *(defined in the ZDL; listed here only to name them)*

- **Order** (`orders-checkout`): the customer's purchase; lifecycle created → confirmed / cancelled.
- **StockReservation** (`catalog-inventory`): one held quantity of one item for one order line; reserved → confirmed / released.
- **InventoryPosition** (`catalog-inventory`): available, reserved and sold quantities per item.
- **Payment** (`payments-processing`): the one payment of an order, from authorization request to captured or voided.
- **Fulfillment** (`fulfillment-shipping`): the shipping arrangement for a confirmed order.
- **Notification** (`notifications-consumer`): one message sent (or failed) to the customer.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In end-to-end checks, 100% of orders reach exactly one ZFL terminal outcome (`completed`, `stockGone` or `orderCancelled`), and none stays in an intermediate state once its triggering steps have run.
- **SC-002**: No item is ever oversold: for every item, reserved plus sold never exceeds the quantity received, including when many customers order the last units concurrently.
- **SC-003**: No customer is charged for an order that does not reach `completed`: 0 captured payments on cancelled orders.
- **SC-004**: A customer whose items are unavailable gets the answer in the same request, with every unavailable item listed.
- **SC-005**: Each customer receives exactly one notice per order outcome, even when steps are delivered more than once.
- **SC-006**: All held stock of a cancelled order is back in the available pool once the cancellation notice has been sent.
- **SC-007**: A customer can see the current status of their order immediately after placing it and at any later point.

## Assumptions

- **Scope**: Reservation expiry (`ReservationExpired`, G-CI-05, M-01) is out of scope for this iteration; without it, an order whose payment never resolves keeps its stock held.
- **Prices** (Q-06): Unit prices and currency are taken from the checkout request; they are not checked against the product catalog.
- **Payment attempts** (Q-07): at most 3 authorization attempts per order, with a short delay between retries; the limit is configuration.
- **Payment provider and notification channel** (Q-11): for this demo, stand-in adapters simulate the payment provider and e-mail delivery; the outcome can be forced in tests.
- **Fulfillment decision** (Q-08): for this demo, a stand-in decides scheduled or failed; it can be forced in tests. `markReadyToShip` stays out of the flow.
- **Released confirmed stock** (Q-09): releasing a confirmed reservation returns its quantity from sold to available.
- **Business references** (Q-12): order, reservation, fulfillment and notification references use the prefixed format already used in the fixtures (e.g. `ORD-…`).
- **Stock setup**: inventory positions exist before a drop (backoffice; `000-catalog-inventory-baseline`).
- **Readiness**: `docs/zenwave-gap-report.md` says READY for the models at API repo `166be74` and architecture repo `915d073` (re-run after the five event-driven commands were keyed by `orderId`).
