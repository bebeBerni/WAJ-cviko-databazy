package com.example.rest.exception;

import com.example.rest.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.validation.method.ParameterErrors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        String message;
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validation = (MethodArgumentNotValidException) ex;
            message = "Vstupné údaje nie sú platné.";
            validation.getBindingResult().getFieldErrors().forEach(error ->
                    addError(errors, error.getField(), error.getDefaultMessage()));
            validation.getBindingResult().getGlobalErrors().forEach(error ->
                    addError(errors, "request", error.getDefaultMessage()));
        } else if (ex instanceof HandlerMethodValidationException
                && !((HandlerMethodValidationException) ex).isForReturnValue()) {
            HandlerMethodValidationException validation = (HandlerMethodValidationException) ex;
            message = "Vstupné údaje nie sú platné.";
            validation.getParameterValidationResults().forEach(result -> {
                String name = result.getMethodParameter().getParameterName();
                if (result instanceof ParameterErrors) {
                    ParameterErrors parameterErrors = (ParameterErrors) result;
                    parameterErrors.getFieldErrors().forEach(error ->
                            addError(errors, error.getField(), error.getDefaultMessage()));
                    parameterErrors.getGlobalErrors().forEach(error ->
                            addError(errors, name == null ? "request" : name, error.getDefaultMessage()));
                } else {
                    result.getResolvableErrors().forEach(error ->
                            addError(errors, name == null ? "request" : name, error.getDefaultMessage()));
                }
            });
        } else {
            message = messageFor(ex, status);
            addError(errors, "request", message);
        }
        return errorResponse(status, headers, message, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                addError(errors, violation.getPropertyPath().toString(), violation.getMessage()));
        return errorResponse(HttpStatus.BAD_REQUEST, HttpHeaders.EMPTY,
                "Vstupné údaje nie sú platné.", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Údaje sú v konflikte s existujúcimi záznamami alebo obmedzeniami databázy.";
        return errorResponse(HttpStatus.CONFLICT, HttpHeaders.EMPTY, message,
                Collections.singletonMap("request", Collections.singletonList(message)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex) {
        log.error("Neočakávaná chyba pri spracovaní požiadavky", ex);
        String message = "Nastala interná chyba servera.";
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, message,
                Collections.singletonMap("request", Collections.singletonList(message)));
    }

    private String messageFor(Exception ex, HttpStatusCode status) {
        if (status.is5xxServerError()) {
            log.error("Chyba servera pri spracovaní požiadavky", ex);
            return "Nastala interná chyba servera.";
        }
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatus = (ResponseStatusException) ex;
            if (responseStatus.getReason() != null) {
                return responseStatus.getReason();
            }
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Telo požiadavky chýba alebo obsahuje neplatný JSON či nesprávne typy hodnôt.";
        }
        switch (status.value()) {
            case 400: return "Neplatná požiadavka alebo parameter URL.";
            case 404: return "Požadovaný zdroj neexistuje.";
            case 405: return "HTTP metóda nie je pre tento zdroj povolená.";
            case 406: return "API poskytuje odpovede vo formáte application/json.";
            case 415: return "Nepodporovaný typ obsahu. Použite application/json.";
            default: return "Požiadavku sa nepodarilo spracovať.";
        }
    }

    private static void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, key -> new ArrayList<>()).add(message);
    }

    private ResponseEntity<Object> errorResponse(HttpStatusCode status, HttpHeaders headers,
            String message, Map<String, List<String>> errors) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new ApiResponse<>(message, null, errors), responseHeaders, status);
    }
}
