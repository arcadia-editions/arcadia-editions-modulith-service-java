package com.arcadiaeditions.fulfillment.shipping.config;

import com.arcadiaeditions.config.TestDataLoader;
import com.arcadiaeditions.fulfillment.shipping.*;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Services InMemory Config. It can be used standalone or with @SpringBootTest.
 */
@Configuration
@Profile("in-memory")
public class ServicesInMemoryConfig extends RepositoriesInMemoryConfig {

    private InMemoryEventPublisher eventPublisher = new InMemoryEventPublisher();
    protected final FulfillmentShippingServiceImpl fulfillmentShippingService =
            new FulfillmentShippingServiceImpl(fulfillmentRepository(), eventPublisher);

    @Bean
    public <T extends FulfillmentShippingService> T fulfillmentShippingService() {
        return (T) fulfillmentShippingService;
    }

    static List<Fulfillment> _fulfillments;

    public void reloadTestData() {

        var testDataLoader =
                new TestDataLoader(List.of(Fulfillment.class, ShippingAddress.class, FulfillmentItem.class));
        var fulfillments = _fulfillments != null
                ? _fulfillments
                : testDataLoader.loadCollectionTestDataAsObjects(Fulfillment.class);
        fulfillmentRepository().deleteAll();
        fulfillmentRepository().saveAll(fulfillments);
        eventPublisher.getEvents().clear();
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return eventPublisher;
    }
}
