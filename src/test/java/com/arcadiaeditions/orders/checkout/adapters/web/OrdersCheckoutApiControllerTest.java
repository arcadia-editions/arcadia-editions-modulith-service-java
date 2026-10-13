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
        Long id = 1L;
        var response = controller.getOrder(id);
        Assertions.assertEquals(200, response.getStatusCode().value());
    }
}
