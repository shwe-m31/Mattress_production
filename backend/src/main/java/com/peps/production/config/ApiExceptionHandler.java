package com.peps.production.config;

import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> databaseError(DataAccessException error) {
        log.error("Database error occurred", error);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Database unavailable. Check the MySQL connection."));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> serverError(Exception error) {
        log.error("Unhandled server error occurred", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Unable to load dashboard data: " + error.getMessage()));
    }
}
