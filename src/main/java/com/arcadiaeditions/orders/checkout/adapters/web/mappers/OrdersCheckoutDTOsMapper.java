package com.arcadiaeditions.orders.checkout.adapters.web.mappers;

import com.arcadiaeditions.common.mappers.*;
import com.arcadiaeditions.orders.checkout.adapters.web.dtos.*;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = BaseMapper.class)
public interface OrdersCheckoutDTOsMapper {

    OrdersCheckoutDTOsMapper INSTANCE = Mappers.getMapper(OrdersCheckoutDTOsMapper.class);

    // request mappings
    StartOrderCheckoutInput asStartOrderCheckoutInput(StartOrderCheckoutInputDTO dto);

    // response mappings

    OrderDTO asOrderDTO(Order entity);
}
