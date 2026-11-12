package com.arcadiaeditions.orders.checkout.adapters.web;

import com.arcadiaeditions.orders.checkout.adapters.web.dtos.*;
import com.arcadiaeditions.orders.checkout.config.ServicesInMemoryConfig;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.slf4j.LoggerFactory;

/**
 * Test controller for OrdersCheckoutApiController.
 */
public class OrdersCheckoutApiControllerTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();

    OrdersCheckoutApiController controller = new OrdersCheckoutApiController(context.ordersCheckoutService());

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    public void startOrderCheckoutTest() {
        StartOrderCheckoutInputDTO reqBody = null;
        var response = controller.startOrderCheckout(reqBody);
        Assertions.assertEquals(201, response.getStatusCode().value());
    }

    @Test
    public void getOrderTest() {
        Long id = null;
        var response = controller.getOrder(id);
        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    // --- Problem details (WebExceptionsTranslator) ---

    @RestController
    static class ThrowingController {
        @GetMapping("/missing")
        String missing() {
            throw new NoSuchElementException("Order 999 not found");
        }

        @GetMapping("/typed/{id}")
        String typed(@org.springframework.web.bind.annotation.PathVariable Long id) {
            return "ok";
        }

        @GetMapping("/transition")
        String transition() {
            var order = new com.arcadiaeditions.orders.checkout.core.domain.Order();
            order.setId(1L);
            order.setStatus(com.arcadiaeditions.orders.checkout.core.domain.OrderStatus.CANCELLED);
            com.arcadiaeditions.orders.checkout.core.domain.OrderTransitions.ensureCanConfirmOrder(order);
            return "ok";
        }
    }

    private MockMvc problemMvc() {
        return MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new com.arcadiaeditions.config.WebExceptionsTranslator())
                .build();
    }

    @Test
    public void noSuchElementIsProblemDetail404() throws Exception {
        problemMvc().perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order 999 not found"));
    }

    @Test
    public void invalidStateTransitionIsProblemDetail409() throws Exception {
        problemMvc().perform(get("/transition"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("confirmOrder")));
    }

    @Test
    public void nonNumericIdIsProblemDetail400() throws Exception {
        problemMvc().perform(get("/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
