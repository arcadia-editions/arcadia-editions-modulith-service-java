package com.arcadiaeditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;

import com.arcadiaeditions.catalog.inventory.domain.InventoryPosition;
import com.arcadiaeditions.catalog.inventory.domain.StockReleaseReason;
import com.arcadiaeditions.catalog.inventory.domain.ReservationStatus;
import com.arcadiaeditions.catalog.inventory.domain.StockReservation;
import com.arcadiaeditions.catalog.inventory.repository.jpa.InventoryPositionRepository;
import com.arcadiaeditions.catalog.inventory.repository.jpa.StockReservationRepository;
import com.arcadiaeditions.fulfillment.shipping.FulfillmentRepository;
import com.arcadiaeditions.fulfillment.shipping.domain.FulfillmentStatus;
import com.arcadiaeditions.notifications.consumer.domain.NotificationStatus;
import com.arcadiaeditions.notifications.consumer.domain.NotificationType;
import com.arcadiaeditions.notifications.consumer.domain.events.OrderCancelledNotificationSent;
import com.arcadiaeditions.notifications.consumer.domain.events.OrderConfirmationSent;
import com.arcadiaeditions.notifications.consumer.domain.events.StockUnavailableNotificationSent;
import com.arcadiaeditions.notifications.consumer.repository.jpa.NotificationRepository;
import com.arcadiaeditions.orders.checkout.core.domain.OrderStatus;
import com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.OrderRepository;
import com.arcadiaeditions.payments.processing.application.port.driven.jpa.PaymentRepository;
import com.arcadiaeditions.payments.processing.domain.PaymentStatus;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
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
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnableScenarios
@TestPropertySource(properties = "arcadia.payments.retry-delay=0")
@RecordApplicationEvents
class PlaceOrderFlowTest {

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

    WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void completed_startCheckoutToOrderConfirmationSent(Scenario scenario) {
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

                    assertThat(orderRepository.findByOrderId(orderId))
                            .hasValueSatisfying(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED));

                    List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
                    assertThat(reservations).hasSize(1);
                    assertThat(reservations)
                            .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED));

                    assertThat(paymentRepository.findByOrderId(orderId))
                            .hasValueSatisfying(
                                    payment -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED));

                    assertThat(fulfillmentRepository.findByOrderId(orderId))
                            .hasValueSatisfying(fulfillment ->
                                    assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED));

                    assertThat(notificationRepository.findByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMATION))
                            .hasValueSatisfying(notification -> {
                                assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
                                assertThat(notification.getNotificationId()).isEqualTo(event.getNotificationId());
                            });
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
