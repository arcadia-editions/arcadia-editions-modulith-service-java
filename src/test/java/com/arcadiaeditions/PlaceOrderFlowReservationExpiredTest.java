package com.arcadiaeditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.HttpMethod.POST;

import com.arcadiaeditions.catalog.inventory.domain.InventoryPosition;
import com.arcadiaeditions.catalog.inventory.domain.ReservationStatus;
import com.arcadiaeditions.catalog.inventory.domain.StockReleaseReason;
import com.arcadiaeditions.catalog.inventory.domain.events.StockReleased;
import com.arcadiaeditions.catalog.inventory.repository.jpa.InventoryPositionRepository;
import com.arcadiaeditions.catalog.inventory.repository.jpa.StockReservationRepository;
import com.arcadiaeditions.catalog.inventory.scheduling.ReservationExpiredScheduler;
import com.arcadiaeditions.config.TestClock;
import com.arcadiaeditions.fulfillment.shipping.FulfillmentRepository;
import com.arcadiaeditions.notifications.consumer.domain.NotificationStatus;
import com.arcadiaeditions.notifications.consumer.domain.NotificationType;
import com.arcadiaeditions.notifications.consumer.domain.events.OrderCancelledNotificationSent;
import com.arcadiaeditions.notifications.consumer.domain.events.OrderConfirmationSent;
import com.arcadiaeditions.notifications.consumer.repository.jpa.NotificationRepository;
import com.arcadiaeditions.orders.checkout.core.domain.OrderStatus;
import com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmationRejected;
import com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.OrderRepository;
import com.arcadiaeditions.payments.processing.application.port.driven.jpa.PaymentRepository;
import com.arcadiaeditions.payments.processing.application.port.driven.provider.PaymentProvider;
import com.arcadiaeditions.payments.processing.application.port.driving.PaymentsProcessingService;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.AuthorizePaymentInput;
import com.arcadiaeditions.payments.processing.domain.PaymentStatus;
import com.arcadiaeditions.payments.processing.domain.event.PaymentAuthorized;
import com.arcadiaeditions.payments.processing.domain.event.PaymentVoided;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * PlaceOrderFlow, timer ReservationExpired (T-65), place-order-flow.zfl:42-69, 268-309. A class of its own: it
 * replaces the application Clock with a {@link MutableClock} (Spring Modulith binds the Scenario listener to the first
 * context of a class only). The timer is off in the test profile; the test fires it with
 * {@link ReservationExpiredScheduler#expireDueReservations()}.
 *
 * <ol>
 *   <li>No payment started (data.sql ORD-027, past its deadline at the fixed now): expireReservation releases the line
 *       (RESERVATION_EXPIRED), StockReleased, cancelOrder, OrderCancelled once, the customer is told; the order ends as
 *       data.sql ORD-021 stores a reservation-expired cancellation. A second fire is a no-op.</li>
 *   <li>Payment started (data.sql ORD-028, AUTHORIZATION_REQUESTED): the timer releases nothing.</li>
 *   <li>Payment started, call in flight (live order, provider held while the timer fires past expiresAt): nothing is
 *       released; payment ends the order (authorized, confirmed, OrderConfirmationSent).</li>
 * </ol>
 *
 * Late authorization (T-69), place-order-flow.zfl:148-176, 236-266, 299-309: the authorization arrives after the timer
 * released the stock. confirmOrder rejects (OrderConfirmationRejected), voidPayment voids the hold (PaymentVoided),
 * releaseStock is a no-op on the RELEASED lines (no StockReleased), so cancelOrder does not run again: the order is
 * cancelled once, by the timer.
 * <ol>
 *   <li>Live order: authorizePayment held until the timer released the stock and cancelled the order; the late
 *       PaymentAuthorized is rejected ORDER_CANCELLED.</li>
 *   <li>data.sql ORD-021 (CANCELLED, RELEASED, AUTHORIZED late): rejected ORDER_CANCELLED.</li>
 *   <li>data.sql ORD-022 (CREATED, RELEASED, AUTHORIZED late): confirmStockReservation NOT_HELD, rejected
 *       STOCK_NOT_HELD, the order stays CREATED.</li>
 * </ol>
 *
 * Ordered: every fire at or after the fixed now expires ORD-027, so the fixture case runs first.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnableScenarios
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaceOrderFlowReservationExpiredTest {

    /** Own PostgreSQL on a random port: the project docker-compose binds fixed ports shared with other runs. */
    static final GenericContainer<?> postgres = new GenericContainer<>("postgres:16.3")
            .withEnv("POSTGRES_DB", "app")
            .withEnv("POSTGRES_USER", "postgres")
            .withEnv("POSTGRES_PASSWORD", "admin123")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add(
                "DATASOURCE_URL",
                () -> "jdbc:postgresql://%s:%d/app".formatted(postgres.getHost(), postgres.getMappedPort(5432)));
    }

    /** A Clock the test moves; starts at the fixed test now of data.sql. */
    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(TestClock.NOW);

        void set(Instant instant) {
            now.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now.get(), zone);
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    /** The movable Clock, and the events published in any thread (cancelOrder and the timer run off the test thread). */
    @TestConfiguration
    static class Config {
        final Queue<StockReleased> stockReleased = new ConcurrentLinkedQueue<>();
        final Queue<OrderCancelled> orderCancelled = new ConcurrentLinkedQueue<>();
        final Queue<OrderConfirmationRejected> orderConfirmationRejected = new ConcurrentLinkedQueue<>();
        final Queue<PaymentVoided> paymentVoided = new ConcurrentLinkedQueue<>();

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }

        @EventListener
        void on(StockReleased event) {
            stockReleased.add(event);
        }

        @EventListener
        void on(OrderCancelled event) {
            orderCancelled.add(event);
        }

        @EventListener
        void on(OrderConfirmationRejected event) {
            orderConfirmationRejected.add(event);
        }

        @EventListener
        void on(PaymentVoided event) {
            paymentVoided.add(event);
        }
    }

    @Autowired
    Config events;

    @Autowired
    MutableClock clock;

    @Autowired
    ReservationExpiredScheduler reservationExpiredScheduler;

    @Autowired
    WebApplicationContext context;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    StockReservationRepository stockReservationRepository;

    @Autowired
    InventoryPositionRepository inventoryPositionRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    FulfillmentRepository fulfillmentRepository;

    @Autowired
    NotificationRepository notificationRepository;

    /** The stand-in processor, real behavior; held only to fire the timer while an authorization is in flight. */
    @MockitoSpyBean
    PaymentProvider paymentProvider;

    /** Real behavior; held only so the timer releases the stock before the payment of a live order starts. */
    @MockitoSpyBean
    PaymentsProcessingService paymentsProcessingService;

    WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        clock.set(TestClock.NOW);
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @Order(1)
    void reservationExpired_noPaymentStarted_stockReleasedAndOrderCancelled_paymentStarted_nothingReleased(
            Scenario scenario) {
        // data.sql at the fixed now 10:00: ORD-027 (no Payment) and ORD-028 (AUTHORIZATION_REQUESTED) are past their
        // deadline; ORD-021 was already released by the timer; every other RESERVED row expires after now
        InventoryPosition before = position("SKU-005");
        var expiresAt027 = stockReservationRepository.findByOrderId("ORD-027").getFirst().getExpiresAt();
        assertThat(expiresAt027).isBefore(TestClock.NOW);

        scenario.stimulate(() -> reservationExpiredScheduler.expireDueReservations())
                .andWaitAtMost(Duration.ofSeconds(30))
                .andWaitForEventOfType(OrderCancelledNotificationSent.class)
                .matchingMappedValue(OrderCancelledNotificationSent::getOrderId, "ORD-027")
                .toArriveAndVerify((event, fired) -> {
                    // the timer ran once per due order; ORD-021 has nothing RESERVED and is not due
                    assertThat(fired).containsExactly("ORD-027", "ORD-028");
                    assertThat(event.getReason()).isEqualTo("RESERVATION_EXPIRED");
                    assertThat(event.getRecipient()).isEqualTo("customer027@example.com");

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        // NoPaymentResult: one StockReleased RESERVATION_EXPIRED, at the Clock's now
                        assertThat(events.stockReleased.stream().filter(e -> "ORD-027".equals(e.getOrderId())))
                                .singleElement()
                                .satisfies(e -> {
                                    assertThat(e.getReason()).isEqualTo(StockReleaseReason.RESERVATION_EXPIRED);
                                    assertThat(e.getReleasedAt()).isEqualTo(TestClock.NOW);
                                    assertThat(e.getItems())
                                            .singleElement()
                                            .satisfies(i -> {
                                                assertThat(i.getSku()).isEqualTo("SKU-005");
                                                assertThat(i.getQuantity()).isEqualTo(1);
                                            });
                                });
                        assertThat(stockReservationRepository.findByOrderId("ORD-027"))
                                .singleElement()
                                .satisfies(r -> {
                                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                                    assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.RESERVATION_EXPIRED);
                                    assertThat(r.getReleasedAt()).isEqualTo(TestClock.NOW);
                                });
                        // the line goes back to the pool
                        InventoryPosition after = position("SKU-005");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity() + 1);
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity() - 1);
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        // cancelled once, as data.sql ORD-021 stores a reservation-expired cancellation
                        assertThat(events.orderCancelled.stream().filter(e -> "ORD-027".equals(e.getOrderId())))
                                .singleElement()
                                .satisfies(e -> assertThat(e.getReason()).isEqualTo("RESERVATION_EXPIRED"));
                        assertThat(orderRepository.findByOrderId("ORD-027")).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getCancellationReason()).isEqualTo("RESERVATION_EXPIRED");
                            assertThat(order.getCancelledAt()).isEqualTo(TestClock.NOW);
                        });
                        assertThat(orderRepository.findByOrderId("ORD-021")).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getCancellationReason()).isEqualTo("RESERVATION_EXPIRED");
                        });
                        // no Payment was ever created, no Fulfillment
                        assertThat(paymentRepository.findByOrderId("ORD-027")).isEmpty();
                        assertThat(fulfillmentRepository.findByOrderId("ORD-027")).isEmpty();
                        assertThat(notificationRepository.findByOrderIdAndType("ORD-027", NotificationType.ORDER_CANCELLED))
                                .hasValueSatisfying(n -> assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT));
                    });

                    // PaymentStartedResult: ORD-028 keeps its reservation, the order and the payment are untouched
                    assertPaymentOwnsTheEnd("ORD-028");
                    assertThat(paymentRepository.findByOrderId("ORD-028"))
                            .hasValueSatisfying(
                                    p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.AUTHORIZATION_REQUESTED));

                    // ORD-021 (released by the timer earlier) is not touched again
                    assertThat(events.stockReleased.stream().filter(e -> "ORD-021".equals(e.getOrderId())))
                            .isEmpty();
                    assertThat(events.orderCancelled.stream().filter(e -> "ORD-021".equals(e.getOrderId())))
                            .isEmpty();
                });

        // a late fire: ORD-027 has nothing RESERVED, ORD-028 still payment-owned; nothing more is released or cancelled
        assertThat(reservationExpiredScheduler.expireDueReservations()).containsExactly("ORD-028");
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(events.stockReleased.stream().filter(e -> "ORD-027".equals(e.getOrderId())))
                    .hasSize(1);
            assertThat(events.orderCancelled.stream().filter(e -> "ORD-027".equals(e.getOrderId())))
                    .hasSize(1);
        });
        assertPaymentOwnsTheEnd("ORD-028");
    }

    @Test
    @Order(2)
    void reservationExpired_paymentInFlight_timerReleasesNothing_paymentEndsTheOrder(Scenario scenario) {
        // the authorization of the live order is held until the timer has fired past its deadline
        var customerId = "CUS-FLOW-EXPIRY";
        // the provider request carries no customerId: the live order is told apart by its amount
        var heldAmount = new java.math.BigDecimal("49.97");
        var heldOrderId = new AtomicReference<String>();
        var authorizeEntered = new CountDownLatch(1);
        var timerFired = new CountDownLatch(1);
        doAnswer(invocation -> {
                    PaymentProvider.AuthorizationRequest request = invocation.getArgument(0);
                    if (heldAmount.compareTo(request.amount()) == 0) {
                        heldOrderId.set(request.orderId());
                        authorizeEntered.countDown();
                        assertThat(timerFired.await(20, TimeUnit.SECONDS)).isTrue();
                    }
                    return invocation.callRealMethod();
                })
                .when(paymentProvider)
                .authorize(any());

        // the Scenario stimulus runs in a transaction: the listeners start after it returns, so the timer fires from
        // a thread of its own while the authorization is held
        CompletableFuture<List<String>> timer = CompletableFuture.supplyAsync(() -> {
            try {
                assertThat(authorizeEntered.await(25, TimeUnit.SECONDS)).isTrue();
                var orderId = heldOrderId.get();
                // the call is in flight: the Payment is committed AUTHORIZATION_REQUESTED, the line RESERVED
                assertThat(paymentRepository.findByOrderId(orderId))
                        .hasValueSatisfying(
                                p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.AUTHORIZATION_REQUESTED));
                var expiresAt = stockReservationRepository.findByOrderId(orderId).getFirst().getExpiresAt();
                assertThat(expiresAt).isEqualTo(TestClock.NOW.plus(Duration.ofMinutes(10)));

                // ten minutes after OrderCreated the timer fires for the order, and releases nothing
                clock.set(expiresAt.plusSeconds(1));
                List<String> fired = reservationExpiredScheduler.expireDueReservations();
                assertPaymentOwnsTheEnd(orderId);
                return fired;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                timerFired.countDown();
            }
        });

        // data.sql SKU-001: plenty; pm_ok, postal 28099 and example.com are non-trigger stand-in values
        String requestBody = """
            {
              "customerId": "%s",
              "customerEmail": "flow.expiry@example.com",
              "paymentMethodId": "pm_ok",
              "currency": "EUR",
              "items": [ { "sku": "SKU-001", "quantity": 1, "unitPrice": 49.97 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "city": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """.formatted(customerId);

        scenario.stimulate(() -> (String) webTestClient
                        .method(POST)
                        .uri("/api/orders/start-checkout")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .exchange()
                        .expectStatus()
                        .isEqualTo(201)
                        .expectBody(Map.class)
                        .returnResult()
                        .getResponseBody()
                        .get("orderId"))
                .andWaitAtMost(Duration.ofSeconds(30))
                .andWaitForEventOfType(OrderConfirmationSent.class)
                .matchingMappedValue(OrderConfirmationSent::getCustomerId, customerId)
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    // the held call was this order's, and the timer ran expireReservation for it
                    assertThat(heldOrderId.get()).isEqualTo(orderId);
                    assertThat(timer.join()).contains(orderId);
                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        // payment ended the order: authorized, confirmed, the line sold, never released nor cancelled
                        assertThat(orderRepository.findByOrderId(orderId))
                                .hasValueSatisfying(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED));
                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> {
                                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                                    assertThat(r.getReleaseReason()).isNull();
                                });
                        assertThat(paymentRepository.findByOrderId(orderId))
                                .hasValueSatisfying(
                                        p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.CAPTURED));
                    });
                    assertThat(events.stockReleased.stream().filter(e -> orderId.equals(e.getOrderId())))
                            .isEmpty();
                    assertThat(events.orderCancelled.stream().filter(e -> orderId.equals(e.getOrderId())))
                            .isEmpty();
                });
    }

    @Test
    @Order(3)
    void lateAuthorization_afterTimerReleasedTheStock_rejectedVoided_orderCancelledOnce(Scenario scenario) {
        var customerId = "CUS-FLOW-LATE-AUTH";
        // data.sql SKU-001: plenty; pm_ok, postal 28098 and example.com are non-trigger stand-in values
        var sku = "SKU-001";
        InventoryPosition before = position(sku);
        var heldOrderId = new AtomicReference<String>();
        var authorizeEntered = new CountDownLatch(1);
        var cancelledByTimer = new CountDownLatch(1);
        // late OrderCreated delivery: no Payment exists for the live order when the timer fires
        doAnswer(invocation -> {
                    AuthorizePaymentInput input = invocation.getArgument(0);
                    if (customerId.equals(input.getCustomerId())) {
                        heldOrderId.set(input.getOrderId());
                        authorizeEntered.countDown();
                        assertThat(cancelledByTimer.await(25, TimeUnit.SECONDS)).isTrue();
                    }
                    return invocation.callRealMethod();
                })
                .when(paymentsProcessingService)
                .authorizePayment(any());

        CompletableFuture<List<String>> timer = CompletableFuture.supplyAsync(() -> {
            try {
                assertThat(authorizeEntered.await(25, TimeUnit.SECONDS)).isTrue();
                var orderId = heldOrderId.get();
                assertThat(paymentRepository.findByOrderId(orderId)).isEmpty();
                var expiresAt = stockReservationRepository.findByOrderId(orderId).getFirst().getExpiresAt();
                assertThat(expiresAt).isEqualTo(TestClock.NOW.plus(Duration.ofMinutes(10)));

                // ten minutes after OrderCreated: NoPaymentResult, the line is released and the order cancelled
                clock.set(expiresAt.plusSeconds(1));
                List<String> fired = reservationExpiredScheduler.expireDueReservations();
                await().atMost(Duration.ofSeconds(10))
                        .untilAsserted(() -> assertThat(orderRepository.findByOrderId(orderId))
                                .hasValueSatisfying(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED)));
                return fired;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                cancelledByTimer.countDown();
            }
        });

        String requestBody = """
            {
              "customerId": "%s",
              "customerEmail": "flow.lateauth@example.com",
              "paymentMethodId": "pm_ok",
              "currency": "EUR",
              "items": [ { "sku": "%s", "quantity": 1, "unitPrice": 29.95 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "city": "Madrid",
                "postalCode": "28098",
                "country": "ES"
              }
            }
            """.formatted(customerId, sku);

        scenario.stimulate(() -> (String) webTestClient
                        .method(POST)
                        .uri("/api/orders/start-checkout")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .exchange()
                        .expectStatus()
                        .isEqualTo(201)
                        .expectBody(Map.class)
                        .returnResult()
                        .getResponseBody()
                        .get("orderId"))
                .andWaitAtMost(Duration.ofSeconds(30))
                .andWaitForEventOfType(PaymentVoided.class)
                .matchingMappedValue(PaymentVoided::getCustomerId, customerId)
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(heldOrderId.get()).isEqualTo(orderId);
                    assertThat(timer.join()).contains(orderId);
                    // the hold is voided; the void reason is the rejection reason
                    assertThat(event.getReason()).isEqualTo("ORDER_CANCELLED");
                    assertThat(event.getProviderReference()).isEqualTo("STANDIN-" + orderId);
                    assertThat(events.orderConfirmationRejected.stream().filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getReason()).isEqualTo("ORDER_CANCELLED");
                                assertThat(e.getCustomerId()).isEqualTo(customerId);
                                assertThat(e.getRejectedAt()).isEqualTo(clock.instant());
                            });

                    assertLateChainEndsAtTheRelease(orderId, before, sku);
                    // released once, by the timer (RESERVATION_EXPIRED); cancelled once
                    assertThat(events.stockReleased.stream().filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> assertThat(e.getReason()).isEqualTo(StockReleaseReason.RESERVATION_EXPIRED));
                    assertThat(events.orderCancelled.stream().filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> assertThat(e.getReason()).isEqualTo("RESERVATION_EXPIRED"));
                    assertThat(stockReservationRepository.findByOrderId(orderId))
                            .singleElement()
                            .satisfies(r -> {
                                assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                                assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.RESERVATION_EXPIRED);
                                assertThat(r.getConfirmedAt()).isNull();
                            });
                    assertThat(orderRepository.findByOrderId(orderId)).hasValueSatisfying(order -> {
                        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                        assertThat(order.getCancellationReason()).isEqualTo("RESERVATION_EXPIRED");
                        assertThat(order.getConfirmedAt()).isNull();
                    });
                    // the customer is told of the cancellation, never of a confirmation; nothing to fulfil
                    await().atMost(Duration.ofSeconds(10))
                            .untilAsserted(() -> assertThat(notificationRepository.findByOrderIdAndType(
                                            orderId, NotificationType.ORDER_CANCELLED))
                                    .hasValueSatisfying(n -> assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT)));
                    assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                            .isEmpty();
                    assertThat(fulfillmentRepository.findByOrderId(orderId)).isEmpty();
                });
    }

    @Test
    @Order(4)
    void lateAuthorization_orderAlreadyCancelled_rejectedOrderCancelled_voided_nothingReleasedNorCancelled(
            Scenario scenario) {
        // data.sql ORD-021: CANCELLED (RESERVATION_EXPIRED), its line RELEASED by the timer, Payment AUTHORIZED late
        InventoryPosition before = position("SKU-002");
        scenario.publish(lateAuthorization("ORD-021", "CUST-021", "2026-10-14T09:51:01Z"))
                .andWaitAtMost(Duration.ofSeconds(30))
                .andWaitForEventOfType(PaymentVoided.class)
                .matchingMappedValue(PaymentVoided::getOrderId, "ORD-021")
                .toArriveAndVerify(event -> {
                    assertThat(event.getReason()).isEqualTo("ORDER_CANCELLED");
                    assertThat(event.getVoidedAt()).isEqualTo(TestClock.NOW);
                    assertThat(events.orderConfirmationRejected.stream().filter(e -> "ORD-021".equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getReason()).isEqualTo("ORDER_CANCELLED");
                                assertThat(e.getCustomerId()).isEqualTo("CUST-021");
                                assertThat(e.getRejectedAt()).isEqualTo(TestClock.NOW);
                            });
                    assertLateChainEndsAtTheRelease("ORD-021", before, "SKU-002");
                    assertThat(events.stockReleased.stream().filter(e -> "ORD-021".equals(e.getOrderId())))
                            .isEmpty();
                    assertThat(events.orderCancelled.stream().filter(e -> "ORD-021".equals(e.getOrderId())))
                            .isEmpty();
                    // as stored: cancelled once, by the timer
                    assertThat(orderRepository.findByOrderId("ORD-021")).hasValueSatisfying(order -> {
                        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                        assertThat(order.getCancellationReason()).isEqualTo("RESERVATION_EXPIRED");
                        assertThat(order.getCancelledAt()).isEqualTo(Instant.parse("2026-10-14T09:50:41Z"));
                        assertThat(order.getConfirmedAt()).isNull();
                    });
                });
    }

    @Test
    @Order(5)
    void lateAuthorization_stockReleasedOrderNotYetCancelled_rejectedStockNotHeld_voided_orderStaysCreated(
            Scenario scenario) {
        // data.sql ORD-022: CREATED, its line RELEASED by the timer (cancelOrder not yet run), Payment AUTHORIZED late
        InventoryPosition before = position("SKU-002");
        scenario.publish(lateAuthorization("ORD-022", "CUST-022", "2026-10-14T09:59:42Z"))
                .andWaitAtMost(Duration.ofSeconds(30))
                .andWaitForEventOfType(PaymentVoided.class)
                .matchingMappedValue(PaymentVoided::getOrderId, "ORD-022")
                .toArriveAndVerify(event -> {
                    assertThat(event.getReason()).isEqualTo("STOCK_NOT_HELD");
                    assertThat(event.getVoidedAt()).isEqualTo(TestClock.NOW);
                    assertThat(events.orderConfirmationRejected.stream().filter(e -> "ORD-022".equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getReason()).isEqualTo("STOCK_NOT_HELD");
                                assertThat(e.getRejectedAt()).isEqualTo(TestClock.NOW);
                            });
                    assertLateChainEndsAtTheRelease("ORD-022", before, "SKU-002");
                    assertThat(events.stockReleased.stream().filter(e -> "ORD-022".equals(e.getOrderId())))
                            .isEmpty();
                    assertThat(events.orderCancelled.stream().filter(e -> "ORD-022".equals(e.getOrderId())))
                            .isEmpty();
                    // NOT_HELD changes nothing: the line stays as the timer left it, the order is not confirmed
                    assertThat(stockReservationRepository.findByOrderId("ORD-022"))
                            .singleElement()
                            .satisfies(r -> {
                                assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                                assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.RESERVATION_EXPIRED);
                                assertThat(r.getConfirmedAt()).isNull();
                            });
                    assertThat(orderRepository.findByOrderId("ORD-022")).hasValueSatisfying(order -> {
                        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
                        assertThat(order.getConfirmedAt()).isNull();
                    });
                });
    }

    /** The late PaymentAuthorized of a data.sql AUTHORIZED Payment (stand-in reference STANDIN-orderId). */
    private static PaymentAuthorized lateAuthorization(String orderId, String customerId, String authorizedAt) {
        return new PaymentAuthorized()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setAmount(new BigDecimal("59.90"))
                .setCurrency("EUR")
                .setProviderReference("STANDIN-" + orderId)
                .setAuthorizedAt(Instant.parse(authorizedAt));
    }

    /**
     * After PaymentVoided: the Payment is VOIDED, never captured; releaseStock (PAYMENT_VOIDED) is a no-op on the
     * RELEASED lines: no StockReleased of that reason, no OrderCancelled beyond the timer's, counters as before.
     */
    private void assertLateChainEndsAtTheRelease(String orderId, InventoryPosition before, String sku) {
        assertThat(events.paymentVoided.stream().filter(e -> orderId.equals(e.getOrderId())))
                .hasSize(1);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(paymentRepository.findByOrderId(orderId))
                .hasValueSatisfying(p -> {
                    assertThat(p.getStatus()).isEqualTo(PaymentStatus.VOIDED);
                    assertThat(p.getVoidedAt()).isNotNull();
                    assertThat(p.getCapturedAt()).isNull();
                }));
        // releaseStock and cancelOrder would run after PaymentVoided: give them time to emit what they must not
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(events.stockReleased.stream()
                            .filter(e -> orderId.equals(e.getOrderId()))
                            .filter(e -> e.getReason() != StockReleaseReason.RESERVATION_EXPIRED))
                    .isEmpty();
            assertThat(events.orderCancelled.stream()
                            .filter(e -> orderId.equals(e.getOrderId()))
                            .filter(e -> !"RESERVATION_EXPIRED".equals(e.getReason())))
                    .isEmpty();
            assertThat(events.orderCancelled.stream().filter(e -> orderId.equals(e.getOrderId())))
                    .hasSizeLessThanOrEqualTo(1);
            InventoryPosition after = position(sku);
            assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
            assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
            assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());
        });
    }

    /** PaymentStartedResult: the timer step ends; the line stays RESERVED and the order CREATED, no event. */
    private void assertPaymentOwnsTheEnd(String orderId) {
        assertThat(stockReservationRepository.findByOrderId(orderId))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVED);
                    assertThat(r.getReleaseReason()).isNull();
                    assertThat(r.getReleasedAt()).isNull();
                });
        assertThat(orderRepository.findByOrderId(orderId))
                .hasValueSatisfying(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.CREATED));
        assertThat(events.stockReleased.stream().filter(e -> orderId.equals(e.getOrderId())))
                .isEmpty();
        assertThat(events.orderCancelled.stream().filter(e -> orderId.equals(e.getOrderId())))
                .isEmpty();
    }

    private InventoryPosition position(String sku) {
        return inventoryPositionRepository.findAll().stream()
                .filter(p -> sku.equals(p.getSku()))
                .findFirst()
                .orElseThrow();
    }
}
