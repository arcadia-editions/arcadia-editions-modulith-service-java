package com.arcadiaeditions.flows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import com.arcadiaeditions.catalog.inventory.domain.InventoryPosition;
import com.arcadiaeditions.catalog.inventory.domain.ReservationStatus;
import com.arcadiaeditions.catalog.inventory.domain.StockReservation;
import com.arcadiaeditions.catalog.inventory.repository.jpa.InventoryPositionRepository;
import com.arcadiaeditions.catalog.inventory.repository.jpa.StockReservationRepository;
import com.arcadiaeditions.fulfillment.shipping.FulfillmentRepository;
import com.arcadiaeditions.fulfillment.shipping.domain.FulfillmentStatus;
import com.arcadiaeditions.notifications.consumer.domain.Notification;
import com.arcadiaeditions.notifications.consumer.domain.NotificationStatus;
import com.arcadiaeditions.notifications.consumer.domain.NotificationType;
import com.arcadiaeditions.notifications.consumer.repository.jpa.NotificationRepository;
import com.arcadiaeditions.orders.checkout.core.domain.Order;
import com.arcadiaeditions.orders.checkout.core.domain.OrderStatus;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.OrderRepository;
import com.arcadiaeditions.payments.processing.application.port.out.jpa.PaymentRepository;
import com.arcadiaeditions.payments.processing.domain.Payment;
import com.arcadiaeditions.payments.processing.domain.PaymentStatus;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * Flow-level tests for the Place Order Flow (R-09, FR-018): drive {@code POST /api/orders/start-checkout}, wait for
 * the terminal event of the ZFL outcome with Spring Modulith {@link Scenario}, then assert the end state of every
 * module through its repository.
 *
 * <p>Not {@code @Transactional}: the flow crosses modules through real transactions and the event publication
 * registry. Tests stay independent by comparing inventory counters before and after (deltas), never absolute values.
 *
 * <p>Needs PostgreSQL on localhost:5432 (start it with {@code docker compose up -d postgresql}; see the note on the disabled
 * {@code @EnableDockerCompose} below). Runs under surefire (the name ends with {@code Test}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
// @DockerComposeInitializer.EnableDockerCompose: disabled as in BaseWebTestClientTest; its check needs postgresql AND
// kafka
// running, otherwise it tries to start the whole compose through Testcontainers (fails with the local postgresql up).
@EnableScenarios
class PlaceOrderFlowTest {

    private static final Duration FLOW_TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    WebApplicationContext context;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    StockReservationRepository reservationRepository;

    @Autowired
    InventoryPositionRepository inventoryRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    FulfillmentRepository fulfillmentRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient =
                MockMvcWebTestClient.bindToApplicationContext(context).build();
    }

    // ---------------------------------------------------------------- scenarios

    /** ZFL outcome completed: pm_ok. */
    @Test
    void completed(Scenario scenario) {
        var items = List.of(new Line("SKU-001", 1, "49.90"));
        var request = request("pm_ok", "28001", "reader@example.com", items);
        var before = stock(items);
        var orderId = new AtomicReference<String>();

        scenario.stimulate(() -> {
                    orderId.set(startCheckout(request));
                    // SC-007: right after the 201 the order is readable, CREATED or later (listeners are async)
                    assertThat(getOrderBody(orderId.get()).<String>read("$.status"))
                            .as("status right after 201")
                            .isIn("CREATED", "CONFIRMED");
                })
                .andWaitAtMost(FLOW_TIMEOUT)
                .andWaitForStateChange(() -> orderConfirmationSent(orderId.get()), sent -> sent)
                .andVerify(sent -> {
                    assertEndState(orderId.get(), items, before, confirmed(1));
                    // SC-007: after OrderConfirmationSent the read model says CONFIRMED
                    assertThat(getOrderBody(orderId.get()).<String>read("$.status"))
                            .as("GET status after confirmation")
                            .isEqualTo("CONFIRMED");
                });
    }

    /** ZFL outcome completed after one payment retry: pm_fail_once. */
    @Test
    void completedWithOneRetry(Scenario scenario) {
        var items = List.of(new Line("SKU-002", 2, "10.00"));
        var request = request("pm_fail_once", "28002", "retry@example.com", items);
        var before = stock(items);
        var orderId = new AtomicReference<String>();

        scenario.stimulate(() -> orderId.set(startCheckout(request)))
                .andWaitAtMost(FLOW_TIMEOUT)
                .andWaitForStateChange(() -> orderConfirmationSent(orderId.get()), sent -> sent)
                .andVerify(sent -> assertEndState(orderId.get(), items, before, confirmed(2)));
    }

    /** ZFL outcome stockGone: quantity above available; 409, no order, no payment, one STOCK_UNAVAILABLE notification. */
    @Test
    void stockGone(Scenario scenario) {
        var items = List.of(new Line("SKU-003", 50, "25.00"));
        var request = request("pm_ok", "28003", "gone@example.com", items);
        var before = stock(items);
        var orderId = new AtomicReference<String>();

        scenario.stimulate(() -> orderId.set(startCheckoutExpectingConflict(request)))
                .andWaitAtMost(FLOW_TIMEOUT)
                .andWaitForStateChange(() -> stockUnavailableSent(orderId.get()), sent -> sent)
                .andVerify(sent -> assertStockUnavailableEndState(orderId.get(), items, before));
    }

    /** ZFL outcome orderCancelled via declined: pm_decline; stock released, one ORDER_CANCELLED notification. */
    @Test
    void orderCancelledViaDeclined(Scenario scenario) {
        var items = List.of(new Line("SKU-001", 1, "49.90"));
        var request = request("pm_decline", "28004", "declined@example.com", items);
        var before = stock(items);
        var orderId = new AtomicReference<String>();

        scenario.stimulate(() -> orderId.set(startCheckout(request)))
                .andWaitAtMost(FLOW_TIMEOUT)
                .andWaitForStateChange(() -> orderCancelledSent(orderId.get()), sent -> sent)
                .andVerify(sent -> {
                    assertEndState(
                            orderId.get(), items, before, cancelled("PAYMENT_DECLINED", PaymentStatus.DECLINED, 1));
                    // SC-007: the read model says CANCELLED with the reason
                    var body = getOrderBody(orderId.get());
                    assertThat(body.<String>read("$.status")).as("GET status").isEqualTo("CANCELLED");
                    assertThat(body.<String>read("$.cancellationReason"))
                            .as("GET cancellationReason")
                            .isEqualTo("PAYMENT_DECLINED");
                });
    }

    /** GET /api/orders/{id} (numeric id, resolved from the business orderId); expects 200 and returns the body. */
    com.jayway.jsonpath.DocumentContext getOrderBody(String orderId) {
        Long id = orderRepository.findByOrderId(orderId).orElseThrow().getId();
        var response = webTestClient
                .method(GET)
                .uri("/api/orders/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return JsonPath.parse(response);
    }

    /** ZFL outcome orderCancelled via retry exhausted: pm_fail_always; three attempts, stock released. */
    @Test
    void orderCancelledViaRetryExhausted(Scenario scenario) {
        var items = List.of(new Line("SKU-002", 1, "10.00"));
        var request = request("pm_fail_always", "28005", "exhausted@example.com", items);
        var before = stock(items);
        var orderId = new AtomicReference<String>();

        scenario.stimulate(() -> orderId.set(startCheckout(request)))
                .andWaitAtMost(FLOW_TIMEOUT)
                .andWaitForStateChange(() -> orderCancelledSent(orderId.get()), sent -> sent)
                .andVerify(sent -> assertEndState(
                        orderId.get(),
                        items,
                        before,
                        cancelled("PAYMENT_RETRY_EXHAUSTED", PaymentStatus.RETRY_EXHAUSTED, 3)));
    }

    /**
     * ZFL outcome orderCancelled via fulfillment failed, then void: postalCode 00000. The order was CONFIRMED first,
     * but no payment was ever captured (capture follows FulfillmentScheduled), so no ORDER_CONFIRMATION is sent
     * (sendOrderConfirmation is triggered by PaymentCaptured); the only notification is ORDER_CANCELLED.
     */
    @Test
    void orderCancelledViaFulfillmentFailedVoid(Scenario scenario) {
        var items = List.of(new Line("SKU-001", 1, "49.90"));
        var request = request("pm_ok", "00000", "fulfillmentfail@example.com", items);
        var before = stock(items);
        var orderId = new AtomicReference<String>();

        scenario.stimulate(() -> orderId.set(startCheckout(request)))
                .andWaitAtMost(FLOW_TIMEOUT)
                .andWaitForStateChange(() -> orderCancelledSent(orderId.get()), sent -> sent)
                .andVerify(sent -> {
                    assertEndState(orderId.get(), items, before, voidedCancellation(1));
                    assertNeverCaptured(orderId.get());
                    var fulfillment =
                            fulfillmentRepository.findByOrderId(orderId.get()).orElseThrow();
                    assertThat(fulfillment.getStatus()).as("fulfillment status").isEqualTo(FulfillmentStatus.FAILED);
                });
    }

    /**
     * ZFL outcome orderCancelled via capture failed, then void: pm_capture_fail. Authorization succeeds, the
     * fulfillment is scheduled, capture fails, the authorization is voided. No ORDER_CONFIRMATION is sent.
     */
    @Test
    void orderCancelledViaCaptureFailedVoid(Scenario scenario) {
        var items = List.of(new Line("SKU-002", 1, "10.00"));
        var request = request("pm_capture_fail", "28007", "capturefail@example.com", items);
        var before = stock(items);
        var orderId = new AtomicReference<String>();

        scenario.stimulate(() -> orderId.set(startCheckout(request)))
                .andWaitAtMost(FLOW_TIMEOUT)
                .andWaitForStateChange(() -> orderCancelledSent(orderId.get()), sent -> sent)
                .andVerify(sent -> {
                    assertEndState(orderId.get(), items, before, voidedCancellation(1));
                    assertNeverCaptured(orderId.get());
                    var fulfillment =
                            fulfillmentRepository.findByOrderId(orderId.get()).orElseThrow();
                    assertThat(fulfillment.getStatus()).as("fulfillment status").isEqualTo(FulfillmentStatus.SCHEDULED);
                });
    }

    /** SC-003: the payment was voided, never captured. */
    void assertNeverCaptured(String orderId) {
        var payment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(payment.getCapturedAt()).as("capturedAt").isNull();
        assertThat(payment.getVoidedAt()).as("voidedAt").isNotNull();
    }

    /** True once the OrderCancelledNotificationSent outcome is committed (ORDER_CANCELLED notification SENT). */
    boolean orderCancelledSent(String orderId) {
        return orderId != null
                && notificationRepository
                        .findByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED)
                        .filter(n -> n.getStatus() == NotificationStatus.SENT)
                        .isPresent();
    }

    /**
     * True once the StockUnavailableNotificationSent outcome is committed for the correlation orderId (the
     * STOCK_UNAVAILABLE notification is saved as SENT in the transaction that publishes the event).
     */
    boolean stockUnavailableSent(String orderId) {
        return orderId != null
                && notificationRepository
                        .findByOrderIdAndType(orderId, NotificationType.STOCK_UNAVAILABLE)
                        .filter(n -> n.getStatus() == NotificationStatus.SENT)
                        .isPresent();
    }

    /** Nothing reserved, no order, no payment; exactly one STOCK_UNAVAILABLE notification, SENT. */
    void assertStockUnavailableEndState(String orderId, List<Line> items, Map<String, Counters> before) {
        assertThat(stock(items)).as("inventory counters unchanged").isEqualTo(before);
        assertThat(reservationRepository.findByOrderIdAndStatusIn(orderId, Arrays.asList(ReservationStatus.values())))
                .as("reservations")
                .isEmpty();
        assertThat(orderRepository.findByOrderId(orderId)).as("order").isEmpty();
        assertThat(paymentRepository.findByOrderId(orderId)).as("payment").isEmpty();
        var notifications = notificationRepository.findAll().stream()
                .filter(n -> orderId.equals(n.getOrderId()))
                .toList();
        assertThat(notifications).as("notifications").hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.STOCK_UNAVAILABLE);
        assertThat(notifications.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    /**
     * True once the OrderConfirmationSent outcome is committed for the order. The event is published in the same
     * transaction that saves the ORDER_CONFIRMATION notification as SENT, so this is the committed form of the event.
     * (Scenario.andWaitForEventOfType captured the event in the first test of the class but saw no events in the
     * second, see the report; state polling is the fallback allowed by R-09.)
     */
    boolean orderConfirmationSent(String orderId) {
        return orderId != null
                && notificationRepository
                        .findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION)
                        .filter(n -> n.getStatus() == NotificationStatus.SENT)
                        .isPresent();
    }

    // ---------------------------------------------------------------- request builder

    /** One order line. */
    record Line(String sku, int quantity, String unitPrice) {}

    /** Request body as in quickstart.md; change paymentMethodId, postalCode, quantities and recipient per scenario. */
    static String request(String paymentMethodId, String postalCode, String customerEmail, List<Line> items) {
        var lines = items.stream()
                .map(l -> "{ \"sku\": \"%s\", \"quantity\": %d, \"unitPrice\": %s }"
                        .formatted(l.sku(), l.quantity(), l.unitPrice()))
                .collect(Collectors.joining(", "));
        return """
                {
                  "customerId": "CUST-FLOW", "customerEmail": "%s", "paymentMethodId": "%s",
                  "currency": "EUR",
                  "items": [ %s ],
                  "shippingAddress": { "name": "Ada Reader", "line1": "1 Main St", "city": "Madrid", "postalCode": "%s", "country": "ES" }
                }""".formatted(customerEmail, paymentMethodId, lines, postalCode);
    }

    /** POSTs the request, expects 201 and returns the business orderId. */
    String startCheckout(String body) {
        var response = webTestClient
                .method(POST)
                .uri("/api/orders/start-checkout")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(201)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return JsonPath.read(response, "$.orderId");
    }

    /** POSTs the request, expects 409 problem details and returns the correlation orderId they carry. */
    String startCheckoutExpectingConflict(String body) {
        var response = webTestClient
                .method(POST)
                .uri("/api/orders/start-checkout")
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(JsonPath.<List<?>>read(response, "$.unavailableItems"))
                .as("unavailableItems")
                .isNotEmpty();
        return JsonPath.read(response, "$.orderId");
    }

    // ---------------------------------------------------------------- end-state assertions

    /** Inventory counters of a SKU. */
    record Counters(int available, int reserved, int sold) {}

    /** Expected cross-module end state; inventory deltas are per ordered unit. */
    record Expected(
            OrderStatus orderStatus,
            String cancellationReason,
            ReservationStatus reservationStatus,
            int availableDeltaPerUnit,
            int reservedDeltaPerUnit,
            int soldDeltaPerUnit,
            PaymentStatus paymentStatus,
            Integer attemptCount,
            Map<NotificationType, NotificationStatus> notifications) {}

    /** Completed flow: order CONFIRMED, reservations CONFIRMED (reserved to sold), payment CAPTURED, one e-mail. */
    static Expected confirmed(int attempts) {
        var notifications = new EnumMap<NotificationType, NotificationStatus>(NotificationType.class);
        notifications.put(NotificationType.ORDER_CONFIRMATION, NotificationStatus.SENT);
        return new Expected(
                OrderStatus.CONFIRMED,
                null,
                ReservationStatus.CONFIRMED,
                -1,
                0,
                1,
                PaymentStatus.CAPTURED,
                attempts,
                notifications);
    }

    /** Cancelled flow: order CANCELLED, reservations RELEASED (counters back to the start values), one e-mail. */
    static Expected cancelled(String reason, PaymentStatus paymentStatus, int attempts) {
        var notifications = new EnumMap<NotificationType, NotificationStatus>(NotificationType.class);
        notifications.put(NotificationType.ORDER_CANCELLED, NotificationStatus.SENT);
        return new Expected(
                OrderStatus.CANCELLED,
                reason,
                ReservationStatus.RELEASED,
                0,
                0,
                0,
                paymentStatus,
                attempts,
                notifications);
    }

    /** Cancelled after confirmation: reason PAYMENT_VOIDED, payment VOIDED, one ORDER_CANCELLED e-mail only. */
    static Expected voidedCancellation(int attempts) {
        return cancelled("PAYMENT_VOIDED", PaymentStatus.VOIDED, attempts);
    }

    Map<String, Counters> stock(List<Line> items) {
        var skus = items.stream().map(Line::sku).toList();
        return new TransactionTemplate(transactionManager)
                .execute(tx -> inventoryRepository.findAllBySkuInOrderBySku(skus).stream()
                        .collect(Collectors.toMap(
                                InventoryPosition::getSku,
                                p -> new Counters(
                                        p.getAvailableQuantity(), p.getReservedQuantity(), p.getSoldQuantity()),
                                (a, b) -> a,
                                TreeMap::new)));
    }

    void assertEndState(String orderId, List<Line> items, Map<String, Counters> before, Expected expected) {
        // order
        Order order = orderRepository.findByOrderId(orderId).orElseThrow();
        assertThat(order.getStatus()).as("order status").isEqualTo(expected.orderStatus());
        assertThat(order.getCancellationReason()).as("cancellation reason").isEqualTo(expected.cancellationReason());

        // reservations: one per line, all in the expected status
        List<StockReservation> reservations =
                reservationRepository.findByOrderIdAndStatusIn(orderId, Arrays.asList(ReservationStatus.values()));
        assertThat(reservations).as("reservations").hasSize(items.size());
        assertThat(reservations).extracting(StockReservation::getStatus).containsOnly(expected.reservationStatus());

        // inventory counters: deltas against the snapshot taken before the request
        var after = stock(items);
        for (Line line : items) {
            var b = before.get(line.sku());
            var a = after.get(line.sku());
            int q = line.quantity();
            assertThat(a.available() - b.available())
                    .as(line.sku() + " available delta")
                    .isEqualTo(expected.availableDeltaPerUnit() * q);
            assertThat(a.reserved() - b.reserved())
                    .as(line.sku() + " reserved delta")
                    .isEqualTo(expected.reservedDeltaPerUnit() * q);
            assertThat(a.sold() - b.sold()).as(line.sku() + " sold delta").isEqualTo(expected.soldDeltaPerUnit() * q);
        }

        // payment
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).as("payment status").isEqualTo(expected.paymentStatus());
        if (expected.attemptCount() != null) {
            assertThat(payment.getAttemptCount()).as("payment attemptCount").isEqualTo(expected.attemptCount());
        }

        // notifications: exactly the expected types, one each, in the expected status
        var notifications = notificationRepository.findAll().stream()
                .filter(n -> orderId.equals(n.getOrderId()))
                .collect(Collectors.groupingBy(Notification::getType));
        assertThat(notifications.keySet())
                .as("notification types")
                .isEqualTo(expected.notifications().keySet());
        expected.notifications().forEach((type, status) -> {
            assertThat(notifications.get(type)).as("notifications " + type).hasSize(1);
            assertThat(notifications.get(type).get(0).getStatus())
                    .as("notification " + type)
                    .isEqualTo(status);
        });
    }
}
