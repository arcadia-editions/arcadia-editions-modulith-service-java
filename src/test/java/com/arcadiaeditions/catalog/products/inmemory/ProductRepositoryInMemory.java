package com.arcadiaeditions.catalog.products.inmemory;

import com.arcadiaeditions.catalog.products.ProductRepository;
import com.arcadiaeditions.catalog.products.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ProductRepositoryInMemory extends InMemoryJpaRepository<Product> implements ProductRepository {}
