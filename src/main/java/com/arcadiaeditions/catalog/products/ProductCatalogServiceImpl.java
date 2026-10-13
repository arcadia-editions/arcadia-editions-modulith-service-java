package com.arcadiaeditions.catalog.products;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.domain.events.*;
import com.arcadiaeditions.catalog.products.dtos.*;
import com.arcadiaeditions.catalog.products.events.*;
import com.arcadiaeditions.catalog.products.mappers.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing [Product].
 */
@Service
@Transactional
@org.springframework.modulith.NamedInterface("ProductCatalogService")
@org.jspecify.annotations.NullMarked
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProductCatalogServiceMapper productCatalogServiceMapper = ProductCatalogServiceMapper.INSTANCE;

    private final ProductRepository productRepository;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    /**
     * Constructor.
     */
    public ProductCatalogServiceImpl(ProductRepository productRepository, EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    public Product createProduct(CreateProductInput input) {

        log.debug("[CRUD] Request to save Product: {}", input);
        var product = productCatalogServiceMapper.update(new Product(), input);
        product = productRepository.save(product);
        // TODO: may need to reload the entity to fetch relationships 'mapped by id'
        return product;
    }

    public Product updateProduct(Long id, UpdateProductInput input) {

        log.debug("Request updateProduct: {} {}", id, input);

        var product = productRepository
                .findById(id)
                .map(existingProduct -> {
                    ProductTransitions.ensureCanUpdateProduct(existingProduct);
                    productCatalogServiceMapper.update(existingProduct, input);
                    existingProduct.setStatus(ProductStatus.DRAFT);
                    return existingProduct;
                })
                .map(productRepository::save)
                .orElseThrow();
        return product;
    }

    public Product publishProduct(Long id) {

        log.debug("Request publishProduct: {}", id);

        var product = productRepository
                .findById(id)
                .map(existingProduct -> {
                    ProductTransitions.ensureCanPublishProduct(existingProduct);
                    existingProduct.setStatus(ProductStatus.PUBLISHED);
                    return existingProduct;
                })
                .map(productRepository::save)
                .orElseThrow();
        // emit events
        var productPublished = eventsMapper.asProductPublished(product);
        eventPublisher.onProductPublished(productPublished);
        return product;
    }

    public Product retireProduct(Long id) {

        log.debug("Request retireProduct: {}", id);

        var product = productRepository
                .findById(id)
                .map(existingProduct -> {
                    ProductTransitions.ensureCanRetireProduct(existingProduct);
                    existingProduct.setStatus(ProductStatus.RETIRED);
                    return existingProduct;
                })
                .map(productRepository::save)
                .orElseThrow();
        // emit events
        var productRetired = eventsMapper.asProductRetired(product);
        eventPublisher.onProductRetired(productRetired);
        return product;
    }
}
