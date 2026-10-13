package com.arcadiaeditions.catalog.products;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.domain.events.*;

public interface EventPublisher {

    void onProductPublished(ProductPublished event);

    void onProductRetired(ProductRetired event);
}
