package com.smart_ecomernce_api.smart_ecomernce_api.exception;


import com.smart_ecomernce_api.Smart_ecommerce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.Smart_ecommerce_api.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle constraint violation exceptions (from custom validators)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status("error")
                .message("Constraint violation")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .errors(errors)
                .build();

        log.warn("Constraint violation: {}", errors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status("error")
                .message(ex.getMessage())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        log.error("Bad request: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();

        // Add field errors
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        }

        // Add global errors
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            String fieldName = error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status("error")
                .message("Validation failed")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .errors(errors)
                .build();

        log.warn("Validation error: {}", errors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.error("Duplicate resource: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .status("error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .statusCode(HttpStatus.CONFLICT.value())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }



    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResponseEntity<ApiResponse<Object>> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Too many requests. Please try again later.")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
    /**
     * Handle InvalidDataException
     */
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidData(
            InvalidDataException ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status("error")
                .message(ex.getMessage())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.warn("Invalid data: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle missing static resources (avoid turning these into 500s)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoStaticResource(
            NoResourceFoundException ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status("error")
                .message("No static resource " + ex.getMessage())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.debug("Static resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Global exception handler for all other exceptions
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status("error")
                .message("An unexpected error occurred: " + ex.getMessage())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.error("Unexpected error", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

