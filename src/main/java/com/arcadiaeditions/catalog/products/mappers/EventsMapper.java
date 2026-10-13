package com.arcadiaeditions.catalog.products.mappers;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    com.arcadiaeditions.catalog.products.domain.events.ProductPublished asProductPublished(Product product);

    com.arcadiaeditions.catalog.products.domain.events.ProductRetired asProductRetired(Product product);
}
