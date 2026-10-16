package com.arcadiaeditions.orders.checkout.core.application;

import com.arcadiaeditions.catalog.inventory.service.InventoryService;
import com.arcadiaeditions.catalog.inventory.service.dtos.ConfirmStockReservationInput;
import com.arcadiaeditions.catalog.inventory.service.dtos.ReserveStockInput;
import com.arcadiaeditions.catalog.inventory.service.dtos.ReserveStockItemInput;
import com.arcadiaeditions.common.BusinessIds;
import com.arcadiaeditions.orders.checkout.core.application.mappers.*;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.inbound.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import com.arcadiaeditions.orders.checkout.core.outbound.events.*;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing [Order].
 */
@Service
@Transactional
@org.springframework.modulith.NamedInterface("OrdersCheckoutService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.hexagonal.Application
public class OrdersCheckoutServiceImpl implements OrdersCheckoutService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final OrdersCheckoutServiceMapper ordersCheckoutServiceMapper = OrdersCheckoutServiceMapper.INSTANCE;

    private final OrderRepository orderRepository;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    private final InventoryService inventoryService;
    private final Clock clock;

    /**
     * Constructor.
     */
    public OrdersCheckoutServiceImpl(
            OrderRepository orderRepository,
            EventPublisher eventPublisher,
            InventoryService inventoryService,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.inventoryService = inventoryService;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = StockUnavailableException.class)
    public Order startOrderCheckout(StartOrderCheckoutInput input) {
        log.debug("Request startOrderCheckout: {}", input);

        var order = ordersCheckoutServiceMapper.update(new Order(), input);
        order.setOrderId(BusinessIds.newId("ORD"));
        order.setTotalAmount(order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        order.setCreatedAt(Instant.now(clock));

        var reserveStockInput = new ReserveStockInput().setOrderId(order.getOrderId());
        order.getItems()
                .forEach(item -> reserveStockInput.addItems(
                        new ReserveStockItemInput().setSku(item.getSku()).setQuantity(item.getQuantity())));
        var result = inventoryService.reserveStock(reserveStockInput);

        switch (result.getOutcome()) {
            case RESERVED -> {
                order.setStatus(OrderStatus.CREATED);
                var saved = orderRepository.save(order);
                eventPublisher.onOrderCreated(eventsMapper.asOrderCreated(saved));
                return saved;
            }
            case UNAVAILABLE -> {
                // Q-05: the order is not persisted; reserveStock wrote nothing
                var stockUnavailable = eventsMapper.asStockUnavailable(order, result);
                eventPublisher.onStockUnavailable(stockUnavailable);
                throw new StockUnavailableException(stockUnavailable);
            }
            default -> throw new IllegalStateException("Unexpected reserveStock outcome: " + result.getOutcome());
        }
    }

    public Order confirmOrder(ConfirmOrderInput input) {
        log.debug("Request confirmOrder: {}", input);

        var order = orderRepository.findByOrderId(input.getOrderId()).orElseThrow();
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return order;
        }
        OrderTransitions.ensureCanConfirmOrder(order);
        inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId(order.getOrderId()));
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now(clock));
        var saved = orderRepository.save(order);
        // emit events
        eventPublisher.onOrderConfirmed(eventsMapper.asOrderConfirmed(saved));
        return saved;
    }

    public Order cancelOrder(CancelOrderInput input) {

        log.debug("Request cancelOrder: {}", input);

        var order = orderRepository.findByOrderId(input.getOrderId()).orElseThrow();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        OrderTransitions.ensureCanCancelOrder(order);
        ordersCheckoutServiceMapper.update(order, input);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now(clock));
        var saved = orderRepository.save(order);
        // emit events
        eventPublisher.onOrderCancelled(eventsMapper.asOrderCancelled(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrder(Long id) {

        log.debug("[CRUD] Request to get Order : {}", id);
        var order = orderRepository.findById(id);
        return order;
    }
}
