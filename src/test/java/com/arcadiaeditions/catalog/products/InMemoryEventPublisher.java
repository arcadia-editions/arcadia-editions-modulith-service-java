package com.arcadiaeditions.catalog.products;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.domain.events.*;
import java.util.ArrayList;
import java.util.List;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<Object> events = new ArrayList<>();

    public List<Object> getEvents() {
        return events;
    }

    public void onProductPublished(ProductPublished event) {
        events.add(event);
    }

    public void onProductRetired(ProductRetired event) {
        events.add(event);
    }
}
