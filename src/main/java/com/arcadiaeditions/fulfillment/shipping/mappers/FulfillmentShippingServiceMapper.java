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
    // CancelFulfillmentInputnull-Fulfillment cancelFulfillment
    Fulfillment asFulfillment(CancelFulfillmentInput input);

    @Mapping(target = "id", ignore = true)
    Fulfillment update(@MappingTarget Fulfillment entity, CancelFulfillmentInput input);
    // ScheduleFulfillmentInputnull-Fulfillment scheduleFulfillment
    Fulfillment asFulfillment(ScheduleFulfillmentInput input);

    @Mapping(target = "id", ignore = true)
    Fulfillment update(@MappingTarget Fulfillment entity, ScheduleFulfillmentInput input);
    // RecordPaymentCapturedInputnull-Fulfillment recordPaymentCaptured
    Fulfillment asFulfillment(RecordPaymentCapturedInput input);

    @Mapping(target = "id", ignore = true)
    Fulfillment update(@MappingTarget Fulfillment entity, RecordPaymentCapturedInput input);
    // output mappings
}
