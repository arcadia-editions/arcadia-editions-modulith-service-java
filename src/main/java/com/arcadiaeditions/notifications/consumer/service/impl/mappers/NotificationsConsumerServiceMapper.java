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
    // notificationId, type, channel, createdAt are completed by NotificationsConsumerServiceImpl.deliverOnce; subject,
    // body by the command;
    // sentAt, failedAt, failureReason by the delivery outcome.
    @Mapping(target = "recipient", source = "customerEmail")
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "body", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "skus", ignore = true)
    Notification asNotification(SendOrderCancelledNotificationInput input);

    @Mapping(target = "id", ignore = true)
    // notificationId, type, channel, createdAt are completed by NotificationsConsumerServiceImpl.deliverOnce; subject,
    // body by the command;
    // sentAt, failedAt, failureReason by the delivery outcome.
    @Mapping(target = "recipient", source = "customerEmail")
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "body", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "skus", ignore = true)
    Notification update(@MappingTarget Notification entity, SendOrderCancelledNotificationInput input);
    // SendOrderConfirmationInputnull-Notification sendOrderConfirmation
    // notificationId, type, channel, createdAt are completed by NotificationsConsumerServiceImpl.deliverOnce; subject,
    // body by the command;
    // sentAt, failedAt, failureReason by the delivery outcome.
    @Mapping(target = "recipient", source = "customerEmail")
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "body", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "skus", ignore = true)
    Notification asNotification(SendOrderConfirmationInput input);

    @Mapping(target = "id", ignore = true)
    // notificationId, type, channel, createdAt are completed by NotificationsConsumerServiceImpl.deliverOnce; subject,
    // body by the command;
    // sentAt, failedAt, failureReason by the delivery outcome.
    @Mapping(target = "recipient", source = "customerEmail")
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "body", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "skus", ignore = true)
    Notification update(@MappingTarget Notification entity, SendOrderConfirmationInput input);
    // SendStockUnavailableNotificationInputnull-Notification sendStockUnavailableNotification
    // notificationId, type, channel, createdAt are completed by NotificationsConsumerServiceImpl.deliverOnce; subject,
    // body, skus by the command;
    // sentAt, failedAt, failureReason by the delivery outcome.
    @Mapping(target = "recipient", source = "customerEmail")
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "body", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    Notification asNotification(SendStockUnavailableNotificationInput input);

    @Mapping(target = "id", ignore = true)
    // notificationId, type, channel, createdAt are completed by NotificationsConsumerServiceImpl.deliverOnce; subject,
    // body, skus by the command;
    // sentAt, failedAt, failureReason by the delivery outcome.
    @Mapping(target = "recipient", source = "customerEmail")
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "body", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    Notification update(@MappingTarget Notification entity, SendStockUnavailableNotificationInput input);
    // output mappings
}
