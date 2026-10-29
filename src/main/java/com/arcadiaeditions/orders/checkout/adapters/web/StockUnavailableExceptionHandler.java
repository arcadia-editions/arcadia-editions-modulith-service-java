package com.arcadiaeditions.orders.checkout.adapters.web;

import com.arcadiaeditions.orders.checkout.core.inbound.StockUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps the orders-owned {@link StockUnavailableException} so the shared {@code config} module need not depend on it. */
@RestControllerAdvice
public class StockUnavailableExceptionHandler {

    @ExceptionHandler(StockUnavailableException.class)
    public ProblemDetail handleStockUnavailable(StockUnavailableException ex, HttpServletRequest request) {
        var event = ex.getStockUnavailable();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Stock unavailable");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("orderId", event.getOrderId());
        List<Map<String, Object>> items = event.getUnavailableItems().stream()
                .map(item -> Map.<String, Object>of(
                        "sku", item.getSku(),
                        "requestedQuantity", item.getRequestedQuantity(),
                        "availableQuantity", item.getAvailableQuantity()))
                .toList();
        problem.setProperty("unavailableItems", items);
        return problem;
    }
}
