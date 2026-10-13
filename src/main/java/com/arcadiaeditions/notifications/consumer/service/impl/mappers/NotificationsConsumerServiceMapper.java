package com.arcadiaeditions.notifications.consumer.service.impl.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface NotificationsConsumerServiceMapper {

    NotificationsConsumerServiceMapper INSTANCE = Mappers.getMapper(NotificationsConsumerServiceMapper.class);

    // input mappings
    // SendOrderCancelledNotificationInputnull-Notification sendOrderCancelledNotification
    Notification asNotification(SendOrderCancelledNotificationInput input);

    @Mapping(target = "id", ignore = true)
    Notification update(@MappingTarget Notification entity, SendOrderCancelledNotificationInput input);
    // SendOrderConfirmationInputnull-Notification sendOrderConfirmation
    Notification asNotification(SendOrderConfirmationInput input);

    @Mapping(target = "id", ignore = true)
    Notification update(@MappingTarget Notification entity, SendOrderConfirmationInput input);
    // SendStockUnavailableNotificationInputnull-Notification sendStockUnavailableNotification
    Notification asNotification(SendStockUnavailableNotificationInput input);

    @Mapping(target = "id", ignore = true)
    Notification update(@MappingTarget Notification entity, SendStockUnavailableNotificationInput input);
    // output mappings
}
