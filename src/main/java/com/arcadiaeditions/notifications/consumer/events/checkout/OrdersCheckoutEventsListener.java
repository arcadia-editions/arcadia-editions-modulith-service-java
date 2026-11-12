package com.arcadiaeditions.notifications.consumer.events.checkout;

import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.events.checkout.mappers.OrdersCheckoutEventsListenerMapper;
import com.arcadiaeditions.notifications.consumer.service.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for OrdersCheckoutZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.layered.InterfaceLayer
public class OrdersCheckoutEventsListener {
    private final NotificationsConsumerService notificationsConsumerService;
    private final OrdersCheckoutEventsListenerMapper mapper = OrdersCheckoutEventsListenerMapper.INSTANCE;

    public OrdersCheckoutEventsListener(NotificationsConsumerService notificationsConsumerService) {
        this.notificationsConsumerService = notificationsConsumerService;
    }

    @ApplicationModuleListener
    public void onStockUnavailable(com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable event) {
        notificationsConsumerService.sendStockUnavailableNotification(
                mapper.asSendStockUnavailableNotificationInput(event));
    }

    @ApplicationModuleListener
    public void onOrderCancelled(com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled event) {
        notificationsConsumerService.sendOrderCancelledNotification(
                mapper.asSendOrderCancelledNotificationInput(event));
    }
}
