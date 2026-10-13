package com.arcadiaeditions.fulfillment.shipping;

import static org.mockito.Mockito.*;

import com.arcadiaeditions.fulfillment.shipping.config.*;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import com.arcadiaeditions.fulfillment.shipping.inmemory.*;
import com.arcadiaeditions.fulfillment.shipping.mappers.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for FulfillmentShippingService.
 */
class FulfillmentShippingServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    FulfillmentShippingServiceImpl fulfillmentShippingService = context.fulfillmentShippingService();

    FulfillmentRepositoryInMemory fulfillmentRepository = context.fulfillmentRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void scheduleFulfillmentTest() { // TODO: implement this test
    }

    @Test
    void markReadyToShipTest() { // TODO: implement this test
    }
}
