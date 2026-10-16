package com.arcadiaeditions.fulfillment.shipping.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface FulfillmentShippingServiceMapper {

    FulfillmentShippingServiceMapper INSTANCE = Mappers.getMapper(FulfillmentShippingServiceMapper.class);

    // input mappings
    // ScheduleFulfillmentInputnull-Fulfillment scheduleFulfillment
    // fulfillmentId, scheduledAt, failedAt, failureReason are set by
    // FulfillmentShippingServiceImpl.scheduleFulfillment;
    // readyAt by markReadyToShip; carrierCode and trackingNumber are not produced by this flow.
    @Mapping(target = "fulfillmentId", ignore = true)
    @Mapping(target = "carrierCode", ignore = true)
    @Mapping(target = "trackingNumber", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "readyAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    Fulfillment asFulfillment(ScheduleFulfillmentInput input);

    @Mapping(target = "id", ignore = true)
    // fulfillmentId, scheduledAt, failedAt, failureReason are set by
    // FulfillmentShippingServiceImpl.scheduleFulfillment;
    // readyAt by markReadyToShip; carrierCode and trackingNumber are not produced by this flow.
    @Mapping(target = "fulfillmentId", ignore = true)
    @Mapping(target = "carrierCode", ignore = true)
    @Mapping(target = "trackingNumber", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "readyAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    Fulfillment update(@MappingTarget Fulfillment entity, ScheduleFulfillmentInput input);
    // output mappings
}
