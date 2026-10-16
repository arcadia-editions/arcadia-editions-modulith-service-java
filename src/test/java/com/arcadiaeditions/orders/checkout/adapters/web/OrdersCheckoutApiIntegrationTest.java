package com.arcadiaeditions.orders.checkout.adapters.web;

import static org.springframework.http.HttpMethod.*;

import com.arcadiaeditions.catalog.inventory.repository.jpa.InventoryPositionRepository;
import com.arcadiaeditions.common.BaseWebTestClientTest;
import com.arcadiaeditions.orders.checkout.adapters.web.dtos.*;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for the {@link OrdersCheckoutApi} REST controller.
 */
public class OrdersCheckoutApiIntegrationTest extends BaseWebTestClientTest {

    @Autowired
    InventoryPositionRepository inventoryPositionRepository;

    @Autowired
    OrderRepository orderRepository;

    private static String body(String currency, String items) {
        return """
            {
              "customerId" : "CUST-100",
              "customerEmail" : "buyer@example.com",
              "paymentMethodId" : "pm_ok",
              "currency" : "%s",
              "items" : %s,
              "shippingAddress" : {
                "name" : "Ada Lovelace",
                "line1" : "Calle Mayor 1",
                "city" : "Madrid",
                "postalCode" : "28001",
                "country" : "ES"
              }
            }
        """.formatted(currency, items);
    }

    private java.util.Map<String, Integer> availableBySku() {
        var map = new java.util.TreeMap<String, Integer>();
        inventoryPositionRepository.findAll().forEach(p -> map.put(p.getSku(), p.getAvailableQuantity()));
        return map;
    }

    @Test
    public void testStartOrderCheckout_409_stockUnavailable() {
        var before = availableBySku();

        webTestClient
                .method(POST)
                .uri("/api/orders/start-checkout")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("EUR", "[ { \"sku\" : \"SKU-001\", \"quantity\" : 100, \"unitPrice\" : 12.50 } ]"))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Stock unavailable")
                .jsonPath("$.status")
                .isEqualTo(409)
                .jsonPath("$.detail")
                .isEqualTo("Some items are not available in the requested quantity")
                .jsonPath("$.instance")
                .isEqualTo("/api/orders/start-checkout")
                .jsonPath("$.orderId")
                .value(org.hamcrest.Matchers.startsWith("ORD-"))
                .jsonPath("$.unavailableItems.length()")
                .isEqualTo(1)
                .jsonPath("$.unavailableItems[0].sku")
                .isEqualTo("SKU-001")
                .jsonPath("$.unavailableItems[0].requestedQuantity")
                .isEqualTo(100)
                .jsonPath("$.unavailableItems[0].availableQuantity")
                .isEqualTo(before.get("SKU-001"));

        org.assertj.core.api.Assertions.assertThat(availableBySku()).isEqualTo(before);
    }

    @Test
    public void testStartOrderCheckout_400_currencyTooLong() {
        var before = availableBySku();

        webTestClient
                .method(POST)
                .uri("/api/orders/start-checkout")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("EURO", "[ { \"sku\" : \"SKU-001\", \"quantity\" : 1, \"unitPrice\" : 12.50 } ]"))
                .exchange()
                .expectStatus()
                .isEqualTo(400);

        org.assertj.core.api.Assertions.assertThat(availableBySku()).isEqualTo(before);
    }

    @Test
    public void testStartOrderCheckout_400_emptyItems() {
        var before = availableBySku();

        webTestClient
                .method(POST)
                .uri("/api/orders/start-checkout")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("EUR", "[]"))
                .exchange()
                .expectStatus()
                .isEqualTo(400);

        org.assertj.core.api.Assertions.assertThat(availableBySku()).isEqualTo(before);
    }

    /**
     * Test: startOrderCheckout for OK.
     */
    @Test
    public void testStartOrderCheckout_201() {
        String requestBody = """
            {
              "customerId" : "CUST-100",
              "customerEmail" : "buyer@example.com",
              "paymentMethodId" : "pm_ok",
              "currency" : "EUR",
              "items" : [ {
                "sku" : "SKU-001",
                "quantity" : 2,
                "unitPrice" : 12.50
              } ],
              "shippingAddress" : {
                "name" : "Ada Lovelace",
                "line1" : "Calle Mayor 1",
                "line2" : "2B",
                "city" : "Madrid",
                "region" : "Madrid",
                "postalCode" : "28001",
                "country" : "ES"
              }
            }
        """;

        webTestClient
                .method(POST)
                .uri("/api/orders/start-checkout")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isEqualTo(201)
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isNotEmpty()
                .jsonPath("$.orderId")
                .value(org.hamcrest.Matchers.startsWith("ORD-"))
                .jsonPath("$.status")
                .isEqualTo("CREATED")
                .jsonPath("$.customerId")
                .isEqualTo("CUST-100")
                .jsonPath("$.customerEmail")
                .isEqualTo("buyer@example.com")
                .jsonPath("$.paymentMethodId")
                .isEqualTo("pm_ok")
                .jsonPath("$.items")
                .isArray()
                .jsonPath("$.items[0].sku")
                .isEqualTo("SKU-001")
                .jsonPath("$.items[0].quantity")
                .isEqualTo(2)
                .jsonPath("$.items[0].unitPrice")
                .isEqualTo(12.50)
                .jsonPath("$.shippingAddress.postalCode")
                .isEqualTo("28001")
                .jsonPath("$.totalAmount")
                .isEqualTo(25.00)
                .jsonPath("$.currency")
                .isEqualTo("EUR")
                .jsonPath("$.createdAt")
                .isNotEmpty()
                .jsonPath("$.confirmedAt")
                .doesNotExist()
                .jsonPath("$.cancelledAt")
                .doesNotExist()
                .jsonPath("$.cancellationReason")
                .doesNotExist();
    }

    /**
     * Test: getOrder for OK (fixture id 1 = ORD-001, CONFIRMED).
     */
    @Test
    public void testGetOrder_200() {
        var id = 1L;

        webTestClient
                .method(GET)
                .uri("/api/orders/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(200)
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(1)
                .jsonPath("$.orderId")
                .isEqualTo("ORD-001")
                .jsonPath("$.status")
                .isEqualTo("CONFIRMED")
                .jsonPath("$.customerId")
                .isEqualTo("CUST-001")
                .jsonPath("$.items[0].sku")
                .isEqualTo("SKU-001")
                .jsonPath("$.items[0].quantity")
                .isEqualTo(1)
                .jsonPath("$.totalAmount")
                .isEqualTo(49.99)
                .jsonPath("$.currency")
                .isEqualTo("EUR")
                .jsonPath("$.createdAt")
                .isNotEmpty()
                .jsonPath("$.confirmedAt")
                .isNotEmpty()
                .jsonPath("$.cancelledAt")
                .doesNotExist()
                .jsonPath("$.cancellationReason")
                .doesNotExist();
    }

    /**
     * Test: getOrder of a CANCELLED fixture (ORD-014; DB id looked up, cancelled after payment declined).
     */
    @Test
    public void testGetOrder_200_cancelled() {
        var id = orderRepository.findByOrderId("ORD-014").orElseThrow().getId();

        webTestClient
                .method(GET)
                .uri("/api/orders/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(200)
                .expectBody()
                .jsonPath("$.orderId")
                .isEqualTo("ORD-014")
                .jsonPath("$.status")
                .isEqualTo("CANCELLED")
                .jsonPath("$.cancelledAt")
                .isNotEmpty()
                .jsonPath("$.cancellationReason")
                .isEqualTo("PAYMENT_DECLINED");
    }

    /**
     * Test: getOrder of an unknown id.
     */
    @Test
    public void testGetOrder_404() {
        webTestClient
                .method(GET)
                .uri("/api/orders/{id}", 999999L)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(404);
    }
}
