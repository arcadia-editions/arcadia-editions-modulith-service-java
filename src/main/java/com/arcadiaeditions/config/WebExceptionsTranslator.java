package com.arcadiaeditions.config;

import com.arcadiaeditions.common.InvalidStateTransitionException;
import com.arcadiaeditions.orders.checkout.core.inbound.StockUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class WebExceptionsTranslator extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ProblemDetail body = ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        fillBindingErrors(body, ex);
        return this.createResponseEntity(body, headers, statusCode, request);
    }

    private void fillBindingErrors(ProblemDetail body, MethodArgumentNotValidException errors) {
        errors.getBindingResult()
                .getGlobalErrors()
                .forEach(fieldError -> body.setProperty(fieldError.getObjectName(), fieldError.toString()));
        errors.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> body.setProperty(
                        fieldError.getObjectName() + "." + fieldError.getField(), fieldError.getDefaultMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidStateTransition(InvalidStateTransitionException ex) {
        return problem(HttpStatus.CONFLICT, "Invalid state transition", ex.getMessage());
    }

    @ExceptionHandler(StockUnavailableException.class)
    public ProblemDetail handleStockUnavailable(StockUnavailableException ex, HttpServletRequest request) {
        var event = ex.getStockUnavailable();
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Stock unavailable", ex.getMessage());
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

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            logger.error(String.format("Internal Error: %s %s", request.getContextPath(), statusCode), ex);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
