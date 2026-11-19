package com.arcadiaeditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.HttpMethod.POST;

import com.arcadiaeditions.catalog.inventory.domain.InventoryPosition;
import com.arcadiaeditions.catalog.inventory.domain.StockReleaseReason;
import com.arcadiaeditions.catalog.inventory.domain.ReservationStatus;
import com.arcadiaeditions.catalog.inventory.domain.StockReservation;
import com.arcadiaeditions.catalog.inventory.repository.jpa.InventoryPositionRepository;
import com.arcadiaeditions.catalog.inventory.repository.jpa.StockReservationRepository;
import com.arcadiaeditions.fulfillment.shipping.FulfillmentRepository;
import com.arcadiaeditions.fulfillment.shipping.FulfillmentShippingService;
import com.arcadiaeditions.fulfillment.shipping.PaymentNotCapturedException;
import com.arcadiaeditions.fulfillment.shipping.domain.FulfillmentStatus;
import com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentCancelled;
import com.arcadiaeditions.notifications.consumer.domain.NotificationStatus;
import com.arcadiaeditions.notifications.consumer.domain.NotificationType;
import com.arcadiaeditions.notifications.consumer.domain.events.NotificationDeliveryFailed;
import com.arcadiaeditions.notifications.consumer.domain.events.OrderCancelledNotificationSent;
import com.arcadiaeditions.notifications.consumer.domain.events.OrderConfirmationSent;
import com.arcadiaeditions.notifications.consumer.domain.events.StockUnavailableNotificationSent;
import com.arcadiaeditions.notifications.consumer.repository.jpa.NotificationRepository;
import com.arcadiaeditions.orders.checkout.core.domain.OrderStatus;
import com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled;
import com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.OrderRepository;
import com.arcadiaeditions.payments.processing.application.port.driven.jpa.PaymentRepository;
import com.arcadiaeditions.payments.processing.application.port.driven.provider.PaymentProvider;
import com.arcadiaeditions.payments.processing.domain.Payment;
import com.arcadiaeditions.payments.processing.domain.PaymentStatus;
import com.arcadiaeditions.payments.processing.domain.event.PaymentCaptureFailed;
import com.arcadiaeditions.payments.processing.domain.event.PaymentFailed;
import com.arcadiaeditions.payments.processing.domain.event.PaymentRetried;
import com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted;
import com.arcadiaeditions.payments.processing.domain.event.PaymentVoidFailed;
import com.arcadiaeditions.payments.processing.domain.event.PaymentVoided;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.modulith.events.core.EventPublicationRepository;
import org.springframework.modulith.events.core.TargetEventPublication;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * PlaceOrderFlow, end "completed": POST start-checkout follows the in-process events
 * (OrderCreated, PaymentAuthorized, OrderConfirmed, FulfillmentScheduled, PaymentCaptured) to OrderConfirmationSent.
 * place-order-flow.zfl:65-192, 262-266. End "stockGone": StockUnavailable to StockUnavailableNotificationSent,
 * place-order-flow.zfl:82-87, 199-202, 262-266. Not @Transactional: the module listeners run after commit.
 * StockUnavailable is published in the request (test) thread, so {@link ApplicationEvents} records it.
 * End "orderCancelled" by decline: OrderCreated, PaymentDeclined, StockReleased, OrderCancelled to
 * OrderCancelledNotificationSent, place-order-flow.zfl:108-116, 223-260, 262-266.
 * End "completed" after one retry: PaymentFailed, PaymentRetried, PaymentAuthorized to OrderConfirmationSent,
 * place-order-flow.zfl:118-140, 262-266. PaymentFailed and PaymentRetried are published in listener threads, which
 * {@link ApplicationEvents} does not record: {@link PaymentEventsRecorder} counts them.
 * End "orderCancelled" by retries exhausted: PaymentFailed and PaymentRetried until max-attempts (3), then
 * PaymentRetryExhausted, StockReleased, OrderCancelled to OrderCancelledNotificationSent,
 * place-order-flow.zfl:124-130, 223-260, 262-266.
 * End "orderCancelled" by fulfillment failure: OrderConfirmed, FulfillmentFailed, PaymentVoided, StockReleased,
 * OrderCancelled to OrderCancelledNotificationSent, place-order-flow.zfl:160-170, 204-260, 262-266.
 * End "orderCancelled" by capture failure: FulfillmentScheduled, PaymentCaptureFailed, PaymentVoided, StockReleased,
 * OrderCancelled to OrderCancelledNotificationSent, place-order-flow.zfl:172-182, 204-260, 262-266. Repository state is
 * read with Awaitility: the terminal event can arrive before the last listener's transaction is visible.
 * End "orderCancelled" by void refused (T-59): OrderConfirmed, FulfillmentFailed, PaymentVoidFailed, StockReleased
 * (PAYMENT_VOID_FAILED), OrderCancelled once to OrderCancelledNotificationSent, Payment VOID_FAILED,
 * place-order-flow.zfl:236-266, 299-309, 322-345 (data.sql ORD-026).
 * End "notificationFailed" (both branches, NotificationDeliveryFailed): PaymentCaptured to sendOrderConfirmation
 * failed (type ORDER_CONFIRMATION, order stays CONFIRMED), place-order-flow.zfl:207-212, 337-345; and OrderCancelled to
 * sendOrderCancelledNotification failed (type ORDER_CANCELLED, order stays CANCELLED), place-order-flow.zfl:322-345.
 * Retry loop (T-55), place-order-flow.zfl:124-146, 254-266: attempts are counted only by authorizePayment (1) and
 * reauthorizePayment (2, 3), the test backoff ({@link #BACKOFF_BEFORE_ATTEMPT_2}, {@link #BACKOFF_BEFORE_ATTEMPT_3})
 * is waited before each reauthorization, and a redelivered PaymentFailed or PaymentRetried with a stale attemptCount
 * changes nothing (data.sql ORD-008, ORD-009, ORD-015, and the live order after its cancellation).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnableScenarios
@TestPropertySource(properties = "arcadia.payments.retry-backoff=300ms,700ms")
@RecordApplicationEvents
class PlaceOrderFlowTest {

    /** The test backoff of arcadia.payments.retry-backoff: short, and distinct, so the gap shows which one was waited. */
    static final Duration BACKOFF_BEFORE_ATTEMPT_2 = Duration.ofMillis(300);

    static final Duration BACKOFF_BEFORE_ATTEMPT_3 = Duration.ofMillis(700);

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

    /** Records payment events published in any thread (ApplicationEvents only sees the test thread). */
    @TestConfiguration
    static class PaymentEventsRecorder {
        final Queue<PaymentFailed> paymentFailed = new ConcurrentLinkedQueue<>();
        final Queue<PaymentRetried> paymentRetried = new ConcurrentLinkedQueue<>();
        final Queue<PaymentRetryExhausted> paymentRetryExhausted = new ConcurrentLinkedQueue<>();
        final Queue<PaymentCaptureFailed> paymentCaptureFailed = new ConcurrentLinkedQueue<>();
        final Queue<PaymentVoided> paymentVoided = new ConcurrentLinkedQueue<>();
        final Queue<PaymentVoidFailed> paymentVoidFailed = new ConcurrentLinkedQueue<>();
        final Queue<OrderCancelled> orderCancelled = new ConcurrentLinkedQueue<>();
        final Queue<FulfillmentCancelled> fulfillmentCancelled = new ConcurrentLinkedQueue<>();
        final Queue<OrderConfirmationSent> orderConfirmationSent = new ConcurrentLinkedQueue<>();
        final Queue<OrderCancelledNotificationSent> orderCancelledNotificationSent = new ConcurrentLinkedQueue<>();

        @EventListener
        void on(PaymentFailed event) {
            paymentFailed.add(event);
        }

        @EventListener
        void on(PaymentRetried event) {
            paymentRetried.add(event);
        }

        @EventListener
        void on(PaymentRetryExhausted event) {
            paymentRetryExhausted.add(event);
        }

        @EventListener
        void on(PaymentCaptureFailed event) {
            paymentCaptureFailed.add(event);
        }

        @EventListener
        void on(PaymentVoided event) {
            paymentVoided.add(event);
        }

        @EventListener
        void on(PaymentVoidFailed event) {
            paymentVoidFailed.add(event);
        }

        @EventListener
        void on(OrderCancelled event) {
            orderCancelled.add(event);
        }

        @EventListener
        void on(FulfillmentCancelled event) {
            fulfillmentCancelled.add(event);
        }

        @EventListener
        void on(OrderConfirmationSent event) {
            orderConfirmationSent.add(event);
        }

        @EventListener
        void on(OrderCancelledNotificationSent event) {
            orderCancelledNotificationSent.add(event);
        }
    }

    @Autowired
    PaymentEventsRecorder paymentEvents;

    @Autowired
    WebApplicationContext context;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    StockReservationRepository stockReservationRepository;

    @Autowired
    InventoryPositionRepository inventoryPositionRepository;

    @Autowired
    ApplicationEvents applicationEvents;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    FulfillmentRepository fulfillmentRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    FulfillmentShippingService fulfillmentShippingService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    EventPublicationRepository eventPublicationRepository;

    /** The stand-in processor, real behavior; a test stubs it only to look at the state while a call is in flight. */
    @MockitoSpyBean
    PaymentProvider paymentProvider;

    WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void completed_startCheckoutToOrderConfirmationSent(Scenario scenario) {
        // T-51, place-order-flow.zfl:202-221, 336-345: while the processor authorizes, the Payment is already committed
        // AUTHORIZATION_REQUESTED; while it captures, the Fulfillment is SCHEDULED without paymentCapturedAt and
        // markReadyToShip is refused. Keyed by orderId: a listener of an earlier test may still be running.
        Map<String, Optional<PaymentStatus>> statusDuringAuthorize = new ConcurrentHashMap<>();
        Map<String, Optional<Throwable>> markReadyToShipDuringCapture = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
                    PaymentProvider.AuthorizationRequest request = invocation.getArgument(0);
                    statusDuringAuthorize.put(
                            request.orderId(),
                            paymentRepository.findByOrderId(request.orderId()).map(p -> p.getStatus()));
                    return invocation.callRealMethod();
                })
                .when(paymentProvider)
                .authorize(any());
        doAnswer(invocation -> {
                    PaymentProvider.CaptureRequest request = invocation.getArgument(0);
                    // own thread, own transactions: the Fulfillment stays out of the capture transaction's persistence
                    // context, and a refusal does not mark that transaction rollback-only
                    Optional<Throwable> outcome;
                    try {
                        CompletableFuture.runAsync(() -> fulfillmentShippingService.markReadyToShip(fulfillmentRepository
                                        .findByOrderId(request.orderId())
                                        .orElseThrow()
                                        .getId()))
                                .join();
                        outcome = Optional.empty();
                    } catch (CompletionException e) {
                        outcome = Optional.of(e.getCause());
                    }
                    markReadyToShipDuringCapture.put(request.orderId(), outcome);
                    return invocation.callRealMethod();
                })
                .when(paymentProvider)
                .capture(any());

        // data.sql SKU-001: plenty (avail 9); pm_ok, postal 28099 and example.com are non-trigger stand-in values
        String requestBody = """
            {
              "customerId": "CUS-FLOW-001",
              "customerEmail": "flow.customer@example.com",
              "paymentMethodId": "pm_ok",
              "currency": "EUR",
              "items": [ { "sku": "SKU-001", "quantity": 1, "unitPrice": 49.99 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

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
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getRecipient()).isEqualTo("flow.customer@example.com");
                    assertThat(event.getSentAt()).isNotNull();

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(orderRepository.findByOrderId(orderId))
                                .hasValueSatisfying(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED));

                        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
                        assertThat(reservations).hasSize(1);
                        assertThat(reservations)
                                .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED));

                        assertThat(paymentRepository.findByOrderId(orderId))
                                .hasValueSatisfying(
                                        payment -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED));

                        var capturedAt = paymentRepository.findByOrderId(orderId).orElseThrow().getCapturedAt();
                        assertThat(capturedAt).isNotNull();
                        assertThat(fulfillmentRepository.findByOrderId(orderId))
                                .hasValueSatisfying(fulfillment -> {
                                    assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED);
                                    // recordPaymentCaptured (place-order-flow.zfl:215-221)
                                    assertThat(fulfillment.getPaymentCapturedAt()).isEqualTo(capturedAt);
                                });

                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                });
                    });

                    // the processor was called with the Payment already AUTHORIZATION_REQUESTED; it then ended
                    // AUTHORIZED (and CAPTURED, above)
                    assertThat(statusDuringAuthorize)
                            .containsEntry(orderId, Optional.of(PaymentStatus.AUTHORIZATION_REQUESTED));

                    // before the capture was recorded, markReadyToShip was refused and named its cause
                    assertThat(markReadyToShipDuringCapture).containsKey(orderId);
                    assertThat(markReadyToShipDuringCapture.get(orderId))
                            .hasValueSatisfying(refusal -> assertThat(refusal)
                                    .isInstanceOf(PaymentNotCapturedException.class)
                                    .hasMessageContaining("payment not captured yet"));

                    // after it, markReadyToShip works
                    var fulfillmentId =
                            fulfillmentRepository.findByOrderId(orderId).orElseThrow().getId();
                    assertThat(fulfillmentShippingService.markReadyToShip(fulfillmentId).getStatus())
                            .isEqualTo(FulfillmentStatus.READY_TO_SHIP);
                    assertThat(fulfillmentRepository.findByOrderId(orderId))
                            .hasValueSatisfying(
                                    f -> assertThat(f.getStatus()).isEqualTo(FulfillmentStatus.READY_TO_SHIP));
                });
    }

    @Test
    void stockGone_startCheckoutToStockUnavailableNotificationSent(Scenario scenario) {
        // data.sql SKU-004: zero available; any quantity is short
        InventoryPosition before = sku004Position();
        String requestBody = """
            {
              "customerId": "CUS-FLOW-004",
              "customerEmail": "flow.stockgone@example.com",
              "paymentMethodId": "pm_ok",
              "currency": "EUR",
              "items": [ { "sku": "SKU-004", "quantity": 1, "unitPrice": 79.00 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

        scenario.stimulate(() -> (Map<?, ?>) webTestClient
                        .method(POST)
                        .uri("/api/orders/start-checkout")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .exchange()
                        .expectStatus()
                        .isEqualTo(409)
                        .expectBody(Map.class)
                        .returnResult()
                        .getResponseBody())
                .andWaitAtMost(Duration.ofSeconds(30))
                .andWaitForEventOfType(StockUnavailableNotificationSent.class)
                .matchingMappedValue(StockUnavailableNotificationSent::getCustomerId, "CUS-FLOW-004")
                .toArriveAndVerify((event, problem) -> {
                    assertThat((List<?>) problem.get("unavailableItems"))
                            .singleElement()
                            .isEqualTo(Map.of("sku", "SKU-004", "requestedQuantity", 1, "availableQuantity", 0));

                    List<StockUnavailable> stockUnavailable = applicationEvents.stream(StockUnavailable.class)
                            .filter(e -> "CUS-FLOW-004".equals(e.getCustomerId()))
                            .toList();
                    assertThat(stockUnavailable).hasSize(1);
                    String orderId = stockUnavailable.get(0).getOrderId();
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(stockUnavailable.get(0).getCustomerEmail()).isEqualTo("flow.stockgone@example.com");

                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getSkus()).containsExactly("SKU-004");
                    assertThat(event.getRecipient()).isEqualTo("flow.stockgone@example.com");
                    assertThat(event.getSentAt()).isNotNull();

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(orderRepository.findByOrderId(orderId)).isEmpty();
                        assertThat(stockReservationRepository.findByOrderId(orderId)).isEmpty();
                        assertThat(paymentRepository.findByOrderId(orderId)).isEmpty();
                        assertThat(fulfillmentRepository.findByOrderId(orderId)).isEmpty();

                        InventoryPosition after = sku004Position();
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.STOCK_UNAVAILABLE))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                    assertThat(notification.getSkus()).containsExactly("SKU-004");
                                });
                    });
                });
    }

    @Test
    void orderCancelled_paymentDeclinedToOrderCancelledNotificationSent(Scenario scenario) {
        // data.sql SKU-002: plenty (avail 20); pm_decline is the stand-in provider's decline trigger
        InventoryPosition before = position("SKU-002");
        String requestBody = """
            {
              "customerId": "CUS-FLOW-002",
              "customerEmail": "flow.declined@example.com",
              "paymentMethodId": "pm_decline",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

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
                .andWaitForEventOfType(OrderCancelledNotificationSent.class)
                .matchingMappedValue(OrderCancelledNotificationSent::getCustomerId, "CUS-FLOW-002")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getReason()).isEqualTo("PAYMENT_DECLINED");
                    assertThat(event.getRecipient()).isEqualTo("flow.declined@example.com");
                    assertThat(event.getSentAt()).isNotNull();

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(paymentRepository.findByOrderId(orderId)).hasValueSatisfying(payment -> {
                            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
                            assertThat(payment.getFailureReason()).isEqualTo("card_declined");
                            assertThat(payment.getDeclinedAt()).isNotNull();
                        });

                        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
                        assertThat(reservations).singleElement().satisfies(r -> {
                            assertThat(r.getSku()).isEqualTo("SKU-002");
                            assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                            assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.PAYMENT_DECLINED);
                            assertThat(r.getReleasedAt()).isNotNull();
                        });

                        InventoryPosition after = position("SKU-002");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        assertThat(orderRepository.findByOrderId(orderId)).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getCancellationReason()).isEqualTo("PAYMENT_DECLINED");
                            assertThat(order.getCancelledAt()).isNotNull();
                        });

                        assertThat(fulfillmentRepository.findByOrderId(orderId)).isEmpty();

                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                    assertThat(notification.getRecipient()).isEqualTo("flow.declined@example.com");
                                });
                    });
                });
    }

    @Test
    void completed_paymentRetriedThenAuthorizedToOrderConfirmationSent(Scenario scenario) {
        // data.sql SKU-002: plenty (avail 20); pm_fail_once is the stand-in provider's fail-first-attempt trigger
        String requestBody = """
            {
              "customerId": "CUS-FLOW-RETRY",
              "customerEmail": "flow.retry@example.com",
              "paymentMethodId": "pm_fail_once",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

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
                .matchingMappedValue(OrderConfirmationSent::getRecipient, "flow.retry@example.com")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getSentAt()).isNotNull();

                    assertThat(paymentEvents.paymentFailed.stream()
                                    .filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getAttemptCount()).isEqualTo(1);
                                assertThat(e.getReason()).isEqualTo("processor_unavailable");
                            });
                    // PaymentRetried carries the failed attempt, not incremented (ZDL 138-145)
                    assertThat(paymentEvents.paymentRetried.stream()
                                    .filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> assertThat(e.getAttemptCount()).isEqualTo(1));

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(paymentRepository.findByOrderId(orderId)).hasValueSatisfying(payment -> {
                            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
                            assertThat(payment.getAttemptCount()).isEqualTo(2);
                            assertThat(payment.getAuthorizedAt()).isNotNull();
                            assertThat(payment.getCapturedAt()).isNotNull();
                        });

                        assertThat(orderRepository.findByOrderId(orderId))
                                .hasValueSatisfying(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED));

                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> {
                                    assertThat(r.getSku()).isEqualTo("SKU-002");
                                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                                });

                        assertThat(fulfillmentRepository.findByOrderId(orderId))
                                .hasValueSatisfying(fulfillment ->
                                        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED));

                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                    assertThat(notification.getRecipient()).isEqualTo("flow.retry@example.com");
                                });
                    });
                });
    }

    @Test
    void orderCancelled_paymentRetryExhaustedToOrderCancelledNotificationSent(Scenario scenario) {
        // data.sql SKU-002: plenty (avail 20); pm_fail_always is the stand-in provider's fail-every-attempt trigger
        InventoryPosition before = position("SKU-002");
        String requestBody = """
            {
              "customerId": "CUS-FLOW-EXHAUSTED",
              "customerEmail": "flow.exhausted@example.com",
              "paymentMethodId": "pm_fail_always",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

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
                .andWaitForEventOfType(OrderCancelledNotificationSent.class)
                .matchingMappedValue(OrderCancelledNotificationSent::getCustomerId, "CUS-FLOW-EXHAUSTED")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getReason()).isEqualTo("PAYMENT_RETRY_EXHAUSTED");
                    assertThat(event.getRecipient()).isEqualTo("flow.exhausted@example.com");
                    assertThat(event.getSentAt()).isNotNull();

                    // attempts 1, 2, 3 fail; retried to attempts 2 and 3; the third failure exhausts the retries
                    assertThat(paymentEvents.paymentFailed.stream()
                                    .filter(e -> orderId.equals(e.getOrderId()))
                                    .map(PaymentFailed::getAttemptCount))
                            .containsExactlyInAnyOrder(1, 2, 3);
                    // PaymentRetried carries the failed attempt, not incremented (ZDL 138-145)
                    assertThat(paymentEvents.paymentRetried.stream()
                                    .filter(e -> orderId.equals(e.getOrderId()))
                                    .map(PaymentRetried::getAttemptCount))
                            .containsExactlyInAnyOrder(1, 2);
                    assertThat(paymentEvents.paymentRetryExhausted.stream()
                                    .filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getAttemptCount()).isEqualTo(3);
                                assertThat(e.getReason()).isEqualTo("processor_unavailable");
                                assertThat(e.getExhaustedAt()).isNotNull();
                            });

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(paymentRepository.findByOrderId(orderId)).hasValueSatisfying(payment -> {
                            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RETRY_EXHAUSTED);
                            assertThat(payment.getAttemptCount()).isEqualTo(3);
                            assertThat(payment.getFailureReason()).isEqualTo("processor_unavailable");
                            assertThat(payment.getAuthorizedAt()).isNull();
                        });

                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> {
                                    assertThat(r.getSku()).isEqualTo("SKU-002");
                                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                                    assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.PAYMENT_RETRY_EXHAUSTED);
                                    assertThat(r.getReleasedAt()).isNotNull();
                                });

                        InventoryPosition after = position("SKU-002");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        assertThat(orderRepository.findByOrderId(orderId)).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getCancellationReason()).isEqualTo("PAYMENT_RETRY_EXHAUSTED");
                            assertThat(order.getCancelledAt()).isNotNull();
                        });

                        assertThat(fulfillmentRepository.findByOrderId(orderId)).isEmpty();

                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                    assertThat(notification.getRecipient()).isEqualTo("flow.exhausted@example.com");
                                });
                    });
                });
    }

    @Test
    void orderCancelled_fulfillmentFailedToOrderCancelledNotificationSent(Scenario scenario) {
        // data.sql SKU-002: plenty (avail 20); postal 00000 is the stand-in fulfillment decision's carrier_unavailable trigger
        InventoryPosition before = position("SKU-002");
        String requestBody = """
            {
              "customerId": "CUS-FLOW-FULFILLMENT",
              "customerEmail": "flow.fulfillment@example.com",
              "paymentMethodId": "pm_ok",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "00000",
                "country": "ES"
              }
            }
            """;

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
                .andWaitForEventOfType(OrderCancelledNotificationSent.class)
                .matchingMappedValue(OrderCancelledNotificationSent::getCustomerId, "CUS-FLOW-FULFILLMENT")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getReason()).isEqualTo("PAYMENT_VOIDED");
                    assertThat(event.getRecipient()).isEqualTo("flow.fulfillment@example.com");
                    assertThat(event.getSentAt()).isNotNull();

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(fulfillmentRepository.findByOrderId(orderId)).hasValueSatisfying(fulfillment -> {
                            assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.FAILED);
                            assertThat(fulfillment.getFailureReason()).isEqualTo("carrier_unavailable");
                            assertThat(fulfillment.getFailedAt()).isNotNull();
                        });

                        // authorized, then voided instead of captured
                        assertThat(paymentRepository.findByOrderId(orderId)).hasValueSatisfying(payment -> {
                            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
                            assertThat(payment.getAuthorizedAt()).isNotNull();
                            assertThat(payment.getVoidedAt()).isNotNull();
                            assertThat(payment.getCapturedAt()).isNull();
                        });

                        // the reservation was CONFIRMED (sold) at OrderConfirmed; releasing it moves sold back to available
                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> {
                                    assertThat(r.getSku()).isEqualTo("SKU-002");
                                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                                    assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.PAYMENT_VOIDED);
                                    assertThat(r.getConfirmedAt()).isNotNull();
                                    assertThat(r.getReleasedAt()).isNotNull();
                                });

                        InventoryPosition after = position("SKU-002");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        // cancelled from CONFIRMED
                        assertThat(orderRepository.findByOrderId(orderId)).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getConfirmedAt()).isNotNull();
                            assertThat(order.getCancellationReason()).isEqualTo("PAYMENT_VOIDED");
                            assertThat(order.getCancelledAt()).isNotNull();
                        });

                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                    assertThat(notification.getRecipient()).isEqualTo("flow.fulfillment@example.com");
                                });
                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                                .isEmpty();
                    });
                });
    }

    @Test
    void orderCancelled_paymentCaptureFailedToOrderCancelledNotificationSent(Scenario scenario) {
        // data.sql SKU-002: plenty (avail 20); pm_capture_fail is the stand-in provider's capture_rejected trigger
        InventoryPosition before = position("SKU-002");
        String requestBody = """
            {
              "customerId": "CUS-FLOW-CAPTURE",
              "customerEmail": "flow.capture@example.com",
              "paymentMethodId": "pm_capture_fail",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

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
                .andWaitForEventOfType(OrderCancelledNotificationSent.class)
                .matchingMappedValue(OrderCancelledNotificationSent::getCustomerId, "CUS-FLOW-CAPTURE")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getReason()).isEqualTo("PAYMENT_VOIDED");
                    assertThat(event.getRecipient()).isEqualTo("flow.capture@example.com");
                    assertThat(event.getSentAt()).isNotNull();

                    // CAPTURE_FAILED first (capture_rejected), then voided: one event each
                    assertThat(paymentEvents.paymentCaptureFailed.stream()
                                    .filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getReason()).isEqualTo("capture_rejected");
                                assertThat(e.getFailedAt()).isNotNull();
                            });
                    assertThat(paymentEvents.paymentVoided.stream()
                                    .filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getProviderReference()).isEqualTo("STANDIN-CAPFAIL-" + orderId);
                                assertThat(e.getVoidedAt()).isNotNull();
                            });

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        // authorized, capture failed (failedAt), then voided; never captured
                        assertThat(paymentRepository.findByOrderId(orderId)).hasValueSatisfying(payment -> {
                            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
                            assertThat(payment.getAuthorizedAt()).isNotNull();
                            assertThat(payment.getFailedAt()).isNotNull();
                            assertThat(payment.getVoidedAt()).isNotNull();
                            assertThat(payment.getCapturedAt()).isNull();
                        });

                        // fulfillment was scheduled; the capture failed after it; the cancellation then cancelled it
                        assertThat(fulfillmentRepository.findByOrderId(orderId)).hasValueSatisfying(fulfillment -> {
                            assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.CANCELLED);
                            assertThat(fulfillment.getCancelledAt()).isNotNull();
                        });
                        assertThat(paymentEvents.fulfillmentCancelled.stream()
                                        .filter(e -> orderId.equals(e.getOrderId())))
                                .singleElement()
                                .satisfies(e -> assertThat(e.getCancelledAt()).isNotNull());

                        // the order is cancelled once
                        assertThat(paymentEvents.orderCancelled.stream().filter(e -> orderId.equals(e.getOrderId())))
                                .hasSize(1);

                        // the reservation was CONFIRMED (sold) at OrderConfirmed; releasing it moves sold back to available
                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> {
                                    assertThat(r.getSku()).isEqualTo("SKU-002");
                                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                                    assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.PAYMENT_VOIDED);
                                    assertThat(r.getConfirmedAt()).isNotNull();
                                    assertThat(r.getReleasedAt()).isNotNull();
                                });

                        InventoryPosition after = position("SKU-002");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        assertThat(orderRepository.findByOrderId(orderId)).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getConfirmedAt()).isNotNull();
                            assertThat(order.getCancellationReason()).isEqualTo("PAYMENT_VOIDED");
                            assertThat(order.getCancelledAt()).isNotNull();
                        });

                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                    assertThat(notification.getRecipient()).isEqualTo("flow.capture@example.com");
                                });
                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                                .isEmpty();
                    });
                });
    }

    @Test
    void orderCancelled_paymentVoidFailedToOrderCancelledNotificationSent(Scenario scenario) {
        // data.sql ORD-026 chain: SKU-002 (avail 20), pm_void_fail (stand-in refuses the void), postal 00000
        // (stand-in fulfillment decision's carrier_unavailable trigger)
        InventoryPosition before = position("SKU-002");
        String requestBody = """
            {
              "customerId": "CUS-FLOW-VOIDFAIL",
              "customerEmail": "flow.voidfail@example.com",
              "paymentMethodId": "pm_void_fail",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "00000",
                "country": "ES"
              }
            }
            """;

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
                .andWaitForEventOfType(OrderCancelledNotificationSent.class)
                .matchingMappedValue(OrderCancelledNotificationSent::getCustomerId, "CUS-FLOW-VOIDFAIL")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getReason()).isEqualTo("PAYMENT_VOID_FAILED");
                    assertThat(event.getRecipient()).isEqualTo("flow.voidfail@example.com");
                    assertThat(event.getSentAt()).isNotNull();

                    // the void was refused: one PaymentVoidFailed, never PaymentVoided
                    assertThat(paymentEvents.paymentVoidFailed.stream()
                                    .filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> {
                                assertThat(e.getCustomerId()).isEqualTo("CUS-FLOW-VOIDFAIL");
                                assertThat(e.getProviderReference()).isEqualTo("STANDIN-" + orderId);
                                assertThat(e.getFailedAt()).isNotNull();
                            });
                    assertThat(paymentEvents.paymentVoided.stream().filter(e -> orderId.equals(e.getOrderId())))
                            .isEmpty();

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        // cancelled once (OrdersCheckout owns OrderCancelled), reason PAYMENT_VOID_FAILED
                        assertThat(paymentEvents.orderCancelled.stream().filter(e -> orderId.equals(e.getOrderId())))
                                .singleElement()
                                .satisfies(e -> {
                                    assertThat(e.getReason()).isEqualTo("PAYMENT_VOID_FAILED");
                                    assertThat(e.getCancelledAt()).isNotNull();
                                });

                        assertThat(fulfillmentRepository.findByOrderId(orderId)).hasValueSatisfying(fulfillment -> {
                            // FAILED stays FAILED: cancelFulfillment is a no-op on it
                            assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.FAILED);
                            assertThat(fulfillment.getFailureReason()).isEqualTo("carrier_unavailable");
                            assertThat(fulfillment.getFailedAt()).isNotNull();
                        });

                        // authorized, void refused: VOID_FAILED for ops; never voided nor captured
                        assertThat(paymentRepository.findByOrderId(orderId)).hasValueSatisfying(payment -> {
                            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOID_FAILED);
                            assertThat(payment.getAuthorizedAt()).isNotNull();
                            assertThat(payment.getVoidFailedAt()).isNotNull();
                            assertThat(payment.getVoidedAt()).isNull();
                            assertThat(payment.getCapturedAt()).isNull();
                        });

                        // the reservation was CONFIRMED (sold) at OrderConfirmed; releasing it moves sold back to available
                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> {
                                    assertThat(r.getSku()).isEqualTo("SKU-002");
                                    assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                                    assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.PAYMENT_VOID_FAILED);
                                    assertThat(r.getConfirmedAt()).isNotNull();
                                    assertThat(r.getReleasedAt()).isNotNull();
                                });

                        InventoryPosition after = position("SKU-002");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        assertThat(orderRepository.findByOrderId(orderId)).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getConfirmedAt()).isNotNull();
                            assertThat(order.getCancellationReason()).isEqualTo("PAYMENT_VOID_FAILED");
                            assertThat(order.getCancelledAt()).isNotNull();
                        });

                        // the customer is told, and told the hold may still show (place-order-flow.zfl:322-327)
                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                    assertThat(notification.getRecipient()).isEqualTo("flow.voidfail@example.com");
                                    assertThat(notification.getBody())
                                            .contains("PAYMENT_VOID_FAILED")
                                            .contains("hold");
                                });
                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                                .isEmpty();
                    });
                });
    }

    @Test
    void notificationFailed_startCheckoutToNotificationDeliveryFailed(Scenario scenario) {
        // place-order-flow.zfl:207-212, 337-345: PaymentCaptured > sendOrderConfirmation[failed] on a fresh order; the
        // stand-in refuses the @fail.invalid domain (the one of data.sql ORD-015); the business outcome is the happy path
        InventoryPosition before = position("SKU-001");
        String requestBody = """
            {
              "customerId": "CUS-FLOW-NOTIF",
              "customerEmail": "flow.notif@fail.invalid",
              "paymentMethodId": "pm_ok",
              "currency": "EUR",
              "items": [ { "sku": "SKU-001", "quantity": 1, "unitPrice": 49.99 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

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
                .andWaitForEventOfType(NotificationDeliveryFailed.class)
                .matchingMappedValue(NotificationDeliveryFailed::getCustomerId, "CUS-FLOW-NOTIF")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getType()).isEqualTo(NotificationType.ORDER_CONFIRMATION);
                    assertThat(event.getRecipient()).isEqualTo("flow.notif@fail.invalid");
                    assertThat(event.getFailedAt()).isNotNull();

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                });

                        // the business outcome is untouched
                        assertThat(orderRepository.findByOrderId(orderId))
                                .hasValueSatisfying(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED));
                        assertThat(paymentRepository.findByOrderId(orderId))
                                .hasValueSatisfying(
                                        payment -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED));
                        assertThat(fulfillmentRepository.findByOrderId(orderId)).hasValueSatisfying(fulfillment -> {
                            assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED);
                            assertThat(fulfillment.getPaymentCapturedAt())
                                    .isEqualTo(paymentRepository
                                            .findByOrderId(orderId)
                                            .orElseThrow()
                                            .getCapturedAt());
                        });
                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED));

                        InventoryPosition after = position("SKU-001");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity() - 1);
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity() + 1);

                        // exactly one outcome: no confirmation was sent for this order
                        assertThat(paymentEvents.orderConfirmationSent).noneMatch(e -> orderId.equals(e.getOrderId()));
                    });
                });
    }

    @Test
    void notificationFailed_orderCancelledToNotificationDeliveryFailed(Scenario scenario) {
        // place-order-flow.zfl:322-345: OrderCancelled > sendOrderCancelledNotification[failed] > NotificationDeliveryFailed;
        // pm_decline cancels the order, the stand-in refuses the @fail.invalid domain (data.sql ORD-015 e-mail domain)
        InventoryPosition before = position("SKU-002");
        String requestBody = """
            {
              "customerId": "CUS-FLOW-NOTIF-CANC",
              "customerEmail": "flow.notifcanc@fail.invalid",
              "paymentMethodId": "pm_decline",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

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
                .andWaitForEventOfType(NotificationDeliveryFailed.class)
                .matchingMappedValue(NotificationDeliveryFailed::getCustomerId, "CUS-FLOW-NOTIF-CANC")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(orderId).startsWith("ORD-");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
                    assertThat(event.getRecipient()).isEqualTo("flow.notifcanc@fail.invalid");
                    assertThat(event.getFailedAt()).isNotNull();

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED))
                                .hasValueSatisfying(notification -> {
                                    assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
                                    assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                                });

                        // the cancellation is untouched
                        assertThat(orderRepository.findByOrderId(orderId)).hasValueSatisfying(order -> {
                            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                            assertThat(order.getCancellationReason()).isEqualTo("PAYMENT_DECLINED");
                        });
                        assertThat(stockReservationRepository.findByOrderId(orderId))
                                .singleElement()
                                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED));

                        InventoryPosition after = position("SKU-002");
                        assertThat(after.getAvailableQuantity()).isEqualTo(before.getAvailableQuantity());
                        assertThat(after.getReservedQuantity()).isEqualTo(before.getReservedQuantity());
                        assertThat(after.getSoldQuantity()).isEqualTo(before.getSoldQuantity());

                        // exactly one outcome: neither a cancellation nor a confirmation was sent
                        assertThat(paymentEvents.orderCancelledNotificationSent)
                                .noneMatch(e -> orderId.equals(e.getOrderId()));
                        assertThat(paymentEvents.orderConfirmationSent).noneMatch(e -> orderId.equals(e.getOrderId()));
                        assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                                .isEmpty();
                    });
                });
    }

    /** One processor authorization call: the attempt asked for, the Payment as committed at that moment, when. */
    record AuthorizeCall(int attempt, Optional<PaymentStatus> status, Optional<Integer> attemptCount, long nanoTime) {}

    @Test
    void retryLoop_attemptsCountedByAuthorizeAndReauthorizeOnly_backoffWaited_staleRedeliveryChangesNothing(
            Scenario scenario) {
        // T-55, place-order-flow.zfl:124-146: pm_fail_always fails every attempt. Keyed by orderId: a listener of an
        // earlier test may still be running.
        Map<String, Queue<AuthorizeCall>> authorizeCalls = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
                    PaymentProvider.AuthorizationRequest request = invocation.getArgument(0);
                    var payment = paymentRepository.findByOrderId(request.orderId());
                    authorizeCalls
                            .computeIfAbsent(request.orderId(), k -> new ConcurrentLinkedQueue<>())
                            .add(new AuthorizeCall(
                                    request.attempt(),
                                    payment.map(Payment::getStatus),
                                    payment.map(Payment::getAttemptCount),
                                    System.nanoTime()));
                    return invocation.callRealMethod();
                })
                .when(paymentProvider)
                .authorize(any());

        String requestBody = """
            {
              "customerId": "CUS-FLOW-RETRYLOOP",
              "customerEmail": "flow.retryloop@example.com",
              "paymentMethodId": "pm_fail_always",
              "currency": "EUR",
              "items": [ { "sku": "SKU-002", "quantity": 1, "unitPrice": 59.90 } ],
              "shippingAddress": {
                "name": "Flow Customer",
                "line1": "Calle Mayor 1",
                "line2": "2B",
                "city": "Madrid",
                "region": "Madrid",
                "postalCode": "28099",
                "country": "ES"
              }
            }
            """;

        var cancelledOrderId = new AtomicReference<String>();
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
                .andWaitForEventOfType(OrderCancelledNotificationSent.class)
                .matchingMappedValue(OrderCancelledNotificationSent::getCustomerId, "CUS-FLOW-RETRYLOOP")
                .toArriveAndVerify((event, orderId) -> {
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getReason()).isEqualTo("PAYMENT_RETRY_EXHAUSTED");
                    cancelledOrderId.set(orderId);

                    // three processor calls, attempts 1, 2, 3; each one already counted and committed
                    // AUTHORIZATION_REQUESTED: authorizePayment counts 1, reauthorizePayment 2 and 3
                    List<AuthorizeCall> calls =
                            List.copyOf(authorizeCalls.getOrDefault(orderId, new ConcurrentLinkedQueue<>()));
                    assertThat(calls).extracting(AuthorizeCall::attempt).containsExactly(1, 2, 3);
                    assertThat(calls).allSatisfy(call -> {
                        assertThat(call.status()).hasValue(PaymentStatus.AUTHORIZATION_REQUESTED);
                        assertThat(call.attemptCount()).hasValue(call.attempt());
                    });

                    // retryPayment never counts: PaymentRetried carries the attempt of the PaymentFailed it answers
                    assertThat(paymentEvents.paymentFailed.stream()
                                    .filter(e -> orderId.equals(e.getOrderId()))
                                    .map(PaymentFailed::getAttemptCount))
                            .containsExactly(1, 2, 3);
                    assertThat(paymentEvents.paymentRetried.stream()
                                    .filter(e -> orderId.equals(e.getOrderId()))
                                    .map(PaymentRetried::getAttemptCount))
                            .containsExactly(1, 2);
                    assertThat(paymentEvents.paymentRetryExhausted.stream().filter(e -> orderId.equals(e.getOrderId())))
                            .singleElement()
                            .satisfies(e -> assertThat(e.getAttemptCount()).isEqualTo(3));

                    // the backoff is waited before each reauthorization: the first before attempt 2, the second
                    // before attempt 3 (the second is the longer one, so a gap below it means the wrong one was used)
                    assertThat(Duration.ofNanos(
                                    calls.get(1).nanoTime() - calls.get(0).nanoTime()))
                            .isGreaterThanOrEqualTo(BACKOFF_BEFORE_ATTEMPT_2);
                    assertThat(Duration.ofNanos(
                                    calls.get(2).nanoTime() - calls.get(1).nanoTime()))
                            .isGreaterThanOrEqualTo(BACKOFF_BEFORE_ATTEMPT_3);

                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        assertThat(paymentRepository.findByOrderId(orderId)).hasValueSatisfying(payment -> {
                            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RETRY_EXHAUSTED);
                            assertThat(payment.getAttemptCount()).isEqualTo(3);
                        });
                        assertThat(orderRepository.findByOrderId(orderId))
                                .hasValueSatisfying(
                                        order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED));
                    });
                });

        // redelivery of the loop's own events after the end: PaymentFailed of attempts 1 and 2, PaymentRetried of
        // attempts 1 and 2, all stale against the Payment RETRY_EXHAUSTED at attempt 3
        String orderId = cancelledOrderId.get();
        var redelivered = new java.util.ArrayList<Object>();
        paymentEvents.paymentFailed.stream()
                .filter(e -> orderId.equals(e.getOrderId()) && e.getAttemptCount() < 3)
                .forEach(redelivered::add);
        paymentEvents.paymentRetried.stream()
                .filter(e -> orderId.equals(e.getOrderId()))
                .forEach(redelivered::add);
        assertThat(redelivered).hasSize(4);
        assertStaleRedeliveryChangesNothing(orderId, authorizeCalls, redelivered);
    }

    @Test
    void retryLoop_staleRedeliveryOfApprovedFixturesChangesNothing() {
        // T-55, place-order-flow.zfl:124-146, data.sql ORD-008 (FAILED a1), ORD-009 (FAILED a3, at the limit),
        // ORD-015 (RETRY_EXHAUSTED a3, order CANCELLED): each PaymentFailed and PaymentRetried below carries an
        // attemptCount that is not the Payment's, or the Payment is no longer FAILED
        Map<String, Queue<AuthorizeCall>> authorizeCalls = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
                    PaymentProvider.AuthorizationRequest request = invocation.getArgument(0);
                    authorizeCalls
                            .computeIfAbsent(request.orderId(), k -> new ConcurrentLinkedQueue<>())
                            .add(new AuthorizeCall(
                                    request.attempt(), Optional.empty(), Optional.empty(), System.nanoTime()));
                    return invocation.callRealMethod();
                })
                .when(paymentProvider)
                .authorize(any());

        // ORD-008 at attempt 1: a PaymentFailed and a PaymentRetried of an attempt it has not reached
        assertStaleRedeliveryChangesNothing(
                "ORD-008",
                authorizeCalls,
                List.of(paymentFailed("ORD-008", "CUST-008", 2), paymentRetried("ORD-008", "CUST-008", 2)));
        // ORD-009 at attempt 3: the PaymentFailed and PaymentRetried of attempt 2, and a PaymentRetried at the limit
        assertStaleRedeliveryChangesNothing(
                "ORD-009",
                authorizeCalls,
                List.of(
                        paymentFailed("ORD-009", "CUST-009", 2),
                        paymentRetried("ORD-009", "CUST-009", 2),
                        paymentRetried("ORD-009", "CUST-009", 3)));
        // ORD-015 exhausted: the last PaymentFailed redelivered, and the PaymentRetried of attempt 2
        assertStaleRedeliveryChangesNothing(
                "ORD-015",
                authorizeCalls,
                List.of(paymentFailed("ORD-015", "CUST-015", 3), paymentRetried("ORD-015", "CUST-015", 2)));
    }

    /** The Payment fields a retry step could change. */
    record PaymentSnapshot(
            PaymentStatus status,
            Integer attemptCount,
            Integer version,
            String failureReason,
            java.time.Instant failedAt,
            java.time.Instant authorizedAt,
            java.time.Instant declinedAt) {
        static PaymentSnapshot of(Payment p) {
            return new PaymentSnapshot(
                    p.getStatus(),
                    p.getAttemptCount(),
                    p.getVersion(),
                    p.getFailureReason(),
                    p.getFailedAt(),
                    p.getAuthorizedAt(),
                    p.getDeclinedAt());
        }
    }

    /**
     * Publishes each event, waits until every listener of it has completed (Spring Modulith event publication
     * registry), and asserts that nothing changed: the Payment and the Order are as before, the processor was not
     * called and no payment event was emitted for the order beyond the redelivered ones.
     */
    private void assertStaleRedeliveryChangesNothing(
            String orderId, Map<String, Queue<AuthorizeCall>> authorizeCalls, List<Object> events) {
        var paymentBefore =
                PaymentSnapshot.of(paymentRepository.findByOrderId(orderId).orElseThrow());
        var orderStatusBefore =
                orderRepository.findByOrderId(orderId).orElseThrow().getStatus();
        int callsBefore = authorizeCalls
                .getOrDefault(orderId, new ConcurrentLinkedQueue<>())
                .size();
        long failedBefore = countFor(paymentEvents.paymentFailed, orderId, PaymentFailed::getOrderId);
        long retriedBefore = countFor(paymentEvents.paymentRetried, orderId, PaymentRetried::getOrderId);
        long exhaustedBefore =
                countFor(paymentEvents.paymentRetryExhausted, orderId, PaymentRetryExhausted::getOrderId);
        long redeliveredFailed =
                events.stream().filter(PaymentFailed.class::isInstance).count();
        long redeliveredRetried =
                events.stream().filter(PaymentRetried.class::isInstance).count();

        for (Object event : events) {
            publishAndAwaitListeners(event);
        }

        assertThat(paymentRepository.findByOrderId(orderId).map(PaymentSnapshot::of))
                .hasValue(paymentBefore);
        assertThat(orderRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(orderStatusBefore);
        assertThat(authorizeCalls.getOrDefault(orderId, new ConcurrentLinkedQueue<>()))
                .hasSize(callsBefore);
        // the recorder also sees the redelivered events themselves
        assertThat(countFor(paymentEvents.paymentFailed, orderId, PaymentFailed::getOrderId))
                .isEqualTo(failedBefore + redeliveredFailed);
        assertThat(countFor(paymentEvents.paymentRetried, orderId, PaymentRetried::getOrderId))
                .isEqualTo(retriedBefore + redeliveredRetried);
        assertThat(countFor(paymentEvents.paymentRetryExhausted, orderId, PaymentRetryExhausted::getOrderId))
                .isEqualTo(exhaustedBefore);
    }

    /** Publishes the event in a transaction of its own and waits for every listener publication of it to complete. */
    private void publishAndAwaitListeners(Object event) {
        Predicate<TargetEventPublication> same = p -> sameEvent(p.getEvent(), event);
        long completedBefore = eventPublicationRepository.findCompletedPublications().stream()
                .filter(same)
                .count();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> eventPublisher.publishEvent(event));
        // the publications are written in the publishing transaction: their number is known once it commits
        long published = eventPublicationRepository.findIncompletePublications().stream()
                        .filter(same)
                        .count()
                + eventPublicationRepository.findCompletedPublications().stream()
                        .filter(same)
                        .count()
                - completedBefore;
        assertThat(published).as("listener publications of %s", event).isPositive();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(eventPublicationRepository.findCompletedPublications().stream()
                                .filter(same)
                                .count())
                        .isEqualTo(completedBefore + published));
    }

    private static boolean sameEvent(Object stored, Object event) {
        if (stored instanceof PaymentFailed s && event instanceof PaymentFailed e) {
            return Objects.equals(s.getOrderId(), e.getOrderId())
                    && Objects.equals(s.getAttemptCount(), e.getAttemptCount())
                    && Objects.equals(s.getFailedAt(), e.getFailedAt());
        }
        if (stored instanceof PaymentRetried s && event instanceof PaymentRetried e) {
            return Objects.equals(s.getOrderId(), e.getOrderId())
                    && Objects.equals(s.getAttemptCount(), e.getAttemptCount())
                    && Objects.equals(s.getRetriedAt(), e.getRetriedAt());
        }
        return false;
    }

    private static <E> long countFor(
            Queue<E> events, String orderId, java.util.function.Function<E, String> orderIdOf) {
        return events.stream().filter(e -> orderId.equals(orderIdOf.apply(e))).count();
    }

    private static PaymentFailed paymentFailed(String orderId, String customerId, int attemptCount) {
        return new PaymentFailed()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setAttemptCount(attemptCount)
                .setReason("processor_unavailable")
                .setFailedAt(java.time.Instant.parse("2026-10-14T09:50:00Z").plusSeconds(attemptCount));
    }

    private static PaymentRetried paymentRetried(String orderId, String customerId, int attemptCount) {
        return new PaymentRetried()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setAttemptCount(attemptCount)
                .setRetriedAt(java.time.Instant.parse("2026-10-14T09:50:30Z").plusSeconds(attemptCount));
    }

    private InventoryPosition sku004Position() {
        return position("SKU-004");
    }

    private InventoryPosition position(String sku) {
        return inventoryPositionRepository.findAll().stream()
                .filter(p -> sku.equals(p.getSku()))
                .findFirst()
                .orElseThrow();
    }
}
