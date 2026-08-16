package com.signalnotes.blog.config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException error) { return error(HttpStatus.NOT_FOUND, error.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, Object>> badRequest(Exception error) { return error(HttpStatus.BAD_REQUEST, error.getMessage()); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception error) {
        log.error("Unhandled API exception", error);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务器暂时无法处理请求");
    }
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) { return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message, "timestamp", Instant.now().toString())); }
}
