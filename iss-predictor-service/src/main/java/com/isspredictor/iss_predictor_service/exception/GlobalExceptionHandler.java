/**
 * Domain exceptions + centralized ControllerAdvice error handling.
 */
package com.isspredictor.iss_predictor_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.List;

/**
 * Centralized error translation, per the LLD's exception strategy table:
 * validation failures -> 400 with field-level messages, anything unexpected
 * -> 500 with a generic message (no stack trace leaked to the client).
 * <p>
 * Note what's deliberately absent here: no handler for upstream client
 * failures. Every {@code client} implementation already catches its own
 * failures internally and degrades via the Option A fallback chain - by
 * design, upstream exceptions should never actually reach this layer.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    public record ErrorResponse(Instant timestamp, int status, String error, List<String> messages) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> messages = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "Validation failed", messages));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error handling request", ex);
        return ResponseEntity.internalServerError().body(new ErrorResponse(
                Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred", List.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "Malformed request body", List.of("Request body is not valid JSON")));
    }
}