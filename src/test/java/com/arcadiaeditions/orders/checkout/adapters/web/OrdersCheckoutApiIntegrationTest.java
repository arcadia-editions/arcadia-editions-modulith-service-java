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
              "customerId" : "customerId-rhso4c0f7tp47tk",
              "customerEmail" : "michaele.lang@yahoo.com",
              "paymentMethodId" : "paymentMethodId-8m8qcm0yf2",
              "currency" : "currency-6cayd2rb9h3o",
              "items" : [ {
                "sku" : "sku-8nl65z1fne5rilyuuokckg",
                "quantity" : 44,
                "unitPrice" : 68
              } ],
              "shippingAddress" : {
                "name" : "name-2ttvvtjpzdkpyyl6qvqxn",
                "line1" : "line1-xo42zu72e3",
                "line2" : "line2-v6f257xh5dys0zc2fvbu",
                "city" : "Shanaestad",
                "region" : "region-hu9o6k3ytzbc",
                "postalCode" : "postalCode-818v71hiie4zmyn",
                "country" : "Marshall Islands"
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

}
