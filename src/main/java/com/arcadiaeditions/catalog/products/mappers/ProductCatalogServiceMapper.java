package com.arcadiaeditions.catalog.products.mappers;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface ProductCatalogServiceMapper {

    ProductCatalogServiceMapper INSTANCE = Mappers.getMapper(ProductCatalogServiceMapper.class);

    // input mappings
    // UpdateProductInputnull-Product updateProduct
    Product asProduct(UpdateProductInput input);

    @Mapping(target = "id", ignore = true)
    Product update(@MappingTarget Product entity, UpdateProductInput input);
    // CreateProductInputnull-Product createProduct
    Product asProduct(CreateProductInput input);

    @Mapping(target = "id", ignore = true)
    Product update(@MappingTarget Product entity, CreateProductInput input);
    // output mappings
}
