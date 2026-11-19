package com.arcadiaeditions.orders.checkout.core.application;

import com.arcadiaeditions.catalog.inventory.service.InventoryService;
import com.arcadiaeditions.catalog.inventory.service.dtos.ConfirmStockReservationInput;
import com.arcadiaeditions.catalog.inventory.service.dtos.ReserveStockInput;
import com.arcadiaeditions.catalog.inventory.service.dtos.ReserveStockItemInput;
import com.arcadiaeditions.catalog.inventory.domain.ConfirmStockReservationOutcome;
import com.arcadiaeditions.catalog.inventory.domain.ReserveStockOutcome;
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

        var orderId = BusinessIds.newId("ORD");
        var reserveStockInput = new ReserveStockInput().setOrderId(orderId);
        input.getItems()
                .forEach(item -> reserveStockInput.addItems(
                        new ReserveStockItemInput().setSku(item.getSku()).setQuantity(item.getQuantity())));
        var reservation = inventoryService.reserveStock(reserveStockInput);
        if (reservation.getOutcome() != ReserveStockOutcome.RESERVED) {
            // no order persisted; orderId is only the correlation id. Thrown after publishing: no rollback for it.
            var stockUnavailable = new StockUnavailable()
                    .setOrderId(orderId)
                    .setCustomerId(input.getCustomerId())
                    .setCustomerEmail(input.getCustomerEmail())
                    .setUnavailableItems(reservation.getUnavailable().getUnavailableItems().stream()
                            .map(line -> new StockUnavailableItem()
                                    .setSku(line.getSku())
                                    .setRequestedQuantity(line.getRequestedQuantity())
                                    .setAvailableQuantity(line.getAvailableQuantity()))
                            .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
            eventPublisher.onStockUnavailable(stockUnavailable);
            throw new StockUnavailableException(stockUnavailable);
        }

        var order = ordersCheckoutServiceMapper.update(new Order(), input);
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(clock.instant());
        order.setTotalAmount(input.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        order = orderRepository.save(order);
        // emit events
        var orderCreated = eventsMapper.asOrderCreated(order);
        eventPublisher.onOrderCreated(orderCreated);
        return order;
    }

    public Order confirmOrder(ConfirmOrderInput input) {

        log.debug("Request confirmOrder: {}", input);

        var existing = orderRepository.findByOrderId(input.getOrderId()).orElseThrow();
        if (existing.getStatus() == OrderStatus.CONFIRMED) {
            log.debug("confirmOrder redelivery, order already CONFIRMED: {}", input.getOrderId());
            return existing;
        }
        if (existing.getStatus() == OrderStatus.CANCELLED) {
            reject(existing, "ORDER_CANCELLED");
            return existing;
        }
        OrderTransitions.ensureCanConfirmOrder(existing);
        var stock = inventoryService.confirmStockReservation(
                new ConfirmStockReservationInput().setOrderId(existing.getOrderId()));
        if (stock.getOutcome() == ConfirmStockReservationOutcome.NOT_HELD) {
            reject(existing, "STOCK_NOT_HELD");
            return existing;
        }
        existing.setStatus(OrderStatus.CONFIRMED);
        existing.setConfirmedAt(clock.instant());
        var order = orderRepository.save(existing);
        // emit events
        var orderConfirmed = eventsMapper.asOrderConfirmed(order);
        eventPublisher.onOrderConfirmed(orderConfirmed);
        return order;
    }

    private void reject(Order order, String reason) {
        var rejected = eventsMapper.asOrderConfirmationRejected(order);
        rejected.setReason(reason);
        rejected.setRejectedAt(clock.instant());
        eventPublisher.onOrderConfirmationRejected(rejected);
    }

    public Order cancelOrder(CancelOrderInput input) {

        log.debug("Request cancelOrder: {}", input);

        var existing = orderRepository.findByOrderId(input.getOrderId()).orElseThrow();
        if (existing.getStatus() == OrderStatus.CANCELLED) {
            log.debug("cancelOrder redelivery, order already CANCELLED: {}", input.getOrderId());
            return existing;
        }
        OrderTransitions.ensureCanCancelOrder(existing);
        existing.setStatus(OrderStatus.CANCELLED);
        existing.setCancellationReason(input.getReason());
        existing.setCancelledAt(clock.instant());
        var order = orderRepository.save(existing);
        // emit events
        var orderCancelled = eventsMapper.asOrderCancelled(order);
        eventPublisher.onOrderCancelled(orderCancelled);
        return order;
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrder(Long id) {

        log.debug("[CRUD] Request to get Order : {}", id);
        var order = orderRepository.findById(id);
        return order;
    }
}
