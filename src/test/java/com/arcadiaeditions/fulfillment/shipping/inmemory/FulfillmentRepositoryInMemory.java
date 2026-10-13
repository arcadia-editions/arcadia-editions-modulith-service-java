package com.arcadiaeditions.fulfillment.shipping.inmemory;

import com.arcadiaeditions.fulfillment.shipping.FulfillmentRepository;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class FulfillmentRepositoryInMemory extends InMemoryJpaRepository<Fulfillment>
        implements FulfillmentRepository {}
