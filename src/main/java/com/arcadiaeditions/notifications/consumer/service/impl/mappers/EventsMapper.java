package com.arcadiaeditions.notifications.consumer.service.impl.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    com.arcadiaeditions.notifications.consumer.domain.events.NotificationDeliveryFailed asNotificationDeliveryFailed(
            Notification notification);

    com.arcadiaeditions.notifications.consumer.domain.events.StockUnavailableNotificationSent
            asStockUnavailableNotificationSent(Notification notification);

    com.arcadiaeditions.notifications.consumer.domain.events.OrderConfirmationSent asOrderConfirmationSent(
            Notification notification);

    com.arcadiaeditions.notifications.consumer.domain.events.OrderCancelledNotificationSent
            asOrderCancelledNotificationSent(Notification notification);
}
