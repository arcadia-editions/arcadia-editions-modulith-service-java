package com.arcadiaeditions.orders.checkout.adapters.web;

import com.arcadiaeditions.orders.checkout.adapters.web.dtos.*;
import com.arcadiaeditions.orders.checkout.adapters.web.mappers.*;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.inbound.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * REST controller for OrdersCheckoutApi.
 */
@RestController
@RequestMapping("/api")
@org.jmolecules.architecture.hexagonal.PrimaryAdapter
public class OrdersCheckoutApiController implements OrdersCheckoutApi {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private NativeWebRequest request;

    private OrdersCheckoutService ordersCheckoutService;

    @Autowired
    public OrdersCheckoutApiController setOrdersCheckoutService(OrdersCheckoutService ordersCheckoutService) {
        this.ordersCheckoutService = ordersCheckoutService;
        return this;
    }

    private OrdersCheckoutDTOsMapper mapper = OrdersCheckoutDTOsMapper.INSTANCE;

    public OrdersCheckoutApiController(OrdersCheckoutService ordersCheckoutService) {

        this.ordersCheckoutService = ordersCheckoutService;
    }

    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    @Override
    public ResponseEntity<OrderDTO> startOrderCheckout(StartOrderCheckoutInputDTO reqBody) {
        log.debug("REST request to startOrderCheckout: {}", reqBody);
        var input = mapper.asStartOrderCheckoutInput(reqBody);
        var order = ordersCheckoutService.startOrderCheckout(input);
        OrderDTO responseDTO = mapper.asOrderDTO(order);
        return ResponseEntity.status(201).body(responseDTO);
    }

    @Override
    public ResponseEntity<OrderDTO> getOrder(Long id) {
        log.debug("REST request to getOrder: {}", id);
        var order = ordersCheckoutService.getOrder(id);
        if (order.isPresent()) {
            OrderDTO responseDTO = mapper.asOrderDTO(order.get());
            return ResponseEntity.status(200).body(responseDTO);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    protected Pageable pageOf(Integer page, Integer limit, List<String> sort) {
        Sort sortOrder = sort != null
                ? Sort.by(sort.stream()
                        .map(sortParam -> {
                            String[] parts = sortParam.split(":");
                            String property = parts[0];
                            Sort.Direction direction =
                                    parts.length > 1 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC;
                            return new Sort.Order(direction, property);
                        })
                        .toList())
                : Sort.unsorted();
        return PageRequest.of(page != null ? page : 0, limit != null ? limit : 10, sortOrder);
    }
}
