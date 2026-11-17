package com.arcadiaeditions.config;

import org.springframework.context.i18n.LocaleContextHolder;
import java.util.NoSuchElementException;
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
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
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
                .forEach(fieldError -> body.setProperty(fieldError.getObjectName() + "." + fieldError.getField(),
                        fieldError.getDefaultMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNoSuchElement(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleInvalidStateTransition(IllegalStateException ex) {
        if (!ex.getClass().getSimpleName().equals("InvalidStateTransitionException")) {
            logger.error("Internal Error", ex);
            return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            logger.error(String.format("Internal Error: %s %s", request.getContextPath(), statusCode), ex);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

}
