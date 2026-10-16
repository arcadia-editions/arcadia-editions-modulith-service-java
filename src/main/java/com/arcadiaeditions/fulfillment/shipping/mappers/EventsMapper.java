package com.arcadiaeditions.fulfillment.shipping.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentScheduled asFulfillmentScheduled(
            Fulfillment fulfillment);

    @Mapping(target = "reason", source = "failureReason")
    com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentFailed asFulfillmentFailed(
            Fulfillment fulfillment);

    com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentReadyToShip asFulfillmentReadyToShip(
            Fulfillment fulfillment);
}
