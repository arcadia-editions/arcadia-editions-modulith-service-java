package com.arcadiaeditions.orders.checkout.adapters.web;

import com.arcadiaeditions.orders.checkout.adapters.web.*;
import com.arcadiaeditions.orders.checkout.adapters.web.dtos.*;
import com.arcadiaeditions.common.BaseWebTestClientTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

import static org.springframework.http.HttpMethod.*;

/**
* Integration tests for the {@link OrdersCheckoutApi} REST controller.
*/
public class OrdersCheckoutApiIntegrationTest extends BaseWebTestClientTest {



    /**
    * Test: startOrderCheckout for OK.
    */
    @Test
    public void testStartOrderCheckout_201() {
        String requestBody = """
            {
              "customerId" : "customerId-wjx0krzw",
              "customerEmail" : "lorilee.kerluke@hotmail.co",
              "paymentMethodId" : "paymentMethodId-89296ckjsv",
              "currency" : "EUR",
              "items" : [ {
                "sku" : "SKU-001",
                "quantity" : 1,
                "unitPrice" : 69
              } ],
              "shippingAddress" : {
                "name" : "name-j6yb",
                "line1" : "line1-d6us",
                "line2" : "line2-1",
                "city" : "Lake Budmouth",
                "region" : "region-2lr544gq8vyy7h",
                "postalCode" : "postalCode-3",
                "country" : "Honduras"
              }
            }
        """;

        webTestClient.method(POST).uri("/api/orders/start-checkout")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isEqualTo(201)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isNotEmpty()
            .jsonPath("$.version").isNotEmpty()
            .jsonPath("$.orderId").isNotEmpty()
            .jsonPath("$.status").isNotEmpty()
            .jsonPath("$.customerId").isNotEmpty()
            .jsonPath("$.customerEmail").isNotEmpty()
            .jsonPath("$.paymentMethodId").isNotEmpty()
            .jsonPath("$.items").isNotEmpty()
            .jsonPath("$.items").isArray()
            .jsonPath("$.items[0].sku").isNotEmpty()
            .jsonPath("$.items[0].quantity").isNotEmpty()
            .jsonPath("$.items[0].unitPrice").isNotEmpty()
            .jsonPath("$.shippingAddress").isNotEmpty()
            .jsonPath("$.shippingAddress.name").isNotEmpty()
            .jsonPath("$.shippingAddress.line1").isNotEmpty()
            .jsonPath("$.shippingAddress.line2").isNotEmpty()
            .jsonPath("$.shippingAddress.city").isNotEmpty()
            .jsonPath("$.shippingAddress.region").isNotEmpty()
            .jsonPath("$.shippingAddress.postalCode").isNotEmpty()
            .jsonPath("$.shippingAddress.country").isNotEmpty()
            .jsonPath("$.totalAmount").isNotEmpty()
            .jsonPath("$.currency").isNotEmpty()
            .jsonPath("$.createdAt").isNotEmpty()
            .jsonPath("$.status").isEqualTo("CREATED")
            .jsonPath("$.orderId").value(org.hamcrest.Matchers.startsWith("ORD-"))
            .jsonPath("$.totalAmount").isEqualTo(69);
    }

    /**
    * Test: startOrderCheckout for Invalid request.
    */
    @Test
    public void testStartOrderCheckout_400() {
        String requestBody = """
            {
              "customerId" : "customerId-txyjuhwr12yzvfa",
              "customerEmail" : "mirtha.lind@gmail.com",
              "paymentMethodId" : "paymentMethodId-kgpr",
              "currency" : "currency-rx",
              "items" : [ {
                "sku" : "sku-mmopg04ig417lsvbyq",
                "quantity" : 75,
                "unitPrice" : 83
              } ],
              "shippingAddress" : {
                "name" : "name-wfo6bs1z0d5lwuvpbezxq",
                "line1" : "line1-lh703tg22ky0npe6b",
                "line2" : "line2-jcsx9r3s",
                "city" : "Hintzchester",
                "region" : "region-rozqobn393b",
                "postalCode" : "postalCode-yoke3aml1",
                "country" : "Belize"
              }
            }
        """;

        webTestClient.method(POST).uri("/api/orders/start-checkout")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isEqualTo(400);
    }

    /**
    * Test: getOrder for OK.
    */
    @Test
    public void testGetOrder_200() {
        var id = "";

        webTestClient.method(GET).uri("/api/orders/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isNotEmpty()
            .jsonPath("$.version").isNotEmpty()
            .jsonPath("$.orderId").isNotEmpty()
            .jsonPath("$.status").isNotEmpty()
            .jsonPath("$.customerId").isNotEmpty()
            .jsonPath("$.customerEmail").isNotEmpty()
            .jsonPath("$.paymentMethodId").isNotEmpty()
            .jsonPath("$.items").isNotEmpty()
            .jsonPath("$.items").isArray()
            .jsonPath("$.items[0].sku").isNotEmpty()
            .jsonPath("$.items[0].quantity").isNotEmpty()
            .jsonPath("$.items[0].unitPrice").isNotEmpty()
            .jsonPath("$.shippingAddress").isNotEmpty()
            .jsonPath("$.shippingAddress.name").isNotEmpty()
            .jsonPath("$.shippingAddress.line1").isNotEmpty()
            .jsonPath("$.shippingAddress.line2").isNotEmpty()
            .jsonPath("$.shippingAddress.city").isNotEmpty()
            .jsonPath("$.shippingAddress.region").isNotEmpty()
            .jsonPath("$.shippingAddress.postalCode").isNotEmpty()
            .jsonPath("$.shippingAddress.country").isNotEmpty()
            .jsonPath("$.totalAmount").isNotEmpty()
            .jsonPath("$.currency").isNotEmpty()
            .jsonPath("$.createdAt").isNotEmpty()
            .jsonPath("$.confirmedAt").isNotEmpty()
            .jsonPath("$.cancelledAt").isNotEmpty()
            .jsonPath("$.cancellationReason").isNotEmpty();
    }

    /**
    * Test: getOrder for Invalid request.
    */
    @Test
    public void testGetOrder_400() {
        var id = "abc";

        webTestClient.method(GET).uri("/api/orders/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(400);
    }

}
