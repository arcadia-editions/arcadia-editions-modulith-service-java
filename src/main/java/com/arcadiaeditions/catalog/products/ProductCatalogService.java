package com.arcadiaeditions.catalog.products;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Inbound Service Port for managing [Product].
 */
@org.springframework.modulith.NamedInterface("ProductCatalogService")
@org.jspecify.annotations.NullMarked
public interface ProductCatalogService {

    /**
     *
     *
     */
    public Product createProduct(CreateProductInput input);
    /**
     *
     *
     */
    public Product updateProduct(Long id, UpdateProductInput input);
    /**
     *
     *
     * With Events: [ProductPublished].
     *
     */
    public Product publishProduct(Long id);
    /**
     *
     *
     * With Events: [ProductRetired].
     *
     */
    public Product retireProduct(Long id);
}
