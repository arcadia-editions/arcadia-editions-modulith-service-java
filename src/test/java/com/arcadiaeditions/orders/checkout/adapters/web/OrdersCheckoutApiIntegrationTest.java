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
              "customerId" : "customerId-6augar3y9e2qp4c",
              "customerEmail" : "buddy.grant@gmail.com",
              "paymentMethodId" : "paymentMethodId-q2y7kwlr2u",
              "currency" : "currency-tpxmmdb6",
              "items" : [ {
                "sku" : "sku-sdcx1t4fh",
                "quantity" : 88,
                "unitPrice" : 1
              } ],
              "shippingAddress" : {
                "name" : "name-h6yr7",
                "line1" : "line1-mfoc",
                "line2" : "line2-zo0c6o",
                "city" : "Batzstad",
                "region" : "region-nuvk66",
                "postalCode" : "postalCode-2ujhl3p7xpjta4v",
                "country" : "Albania"
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
            .jsonPath("$.confirmedAt").isNotEmpty()
            .jsonPath("$.cancelledAt").isNotEmpty()
            .jsonPath("$.cancellationReason").isNotEmpty();
    }

}
