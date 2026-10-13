package com.arcadiaeditions.catalog.products.events;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.events.*;
import com.arcadiaeditions.catalog.products.EventPublisher;
import com.arcadiaeditions.catalog.products.domain.events.ProductPublished;
import com.arcadiaeditions.catalog.products.domain.events.ProductRetired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DefaultEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void onProductPublished(ProductPublished event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onProductRetired(ProductRetired event) {
        applicationEventPublisher.publishEvent(event);
    }
}
