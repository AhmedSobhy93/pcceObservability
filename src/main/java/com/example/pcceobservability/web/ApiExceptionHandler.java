package com.example.pcceobservability.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            IllegalArgumentException.class
    })
    ResponseEntity<Map<String, Object>> badRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    ResponseEntity<Map<String, Object>> sqlProblem(BadSqlGrammarException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "SQL failed. Check the configured PCCE/CVP query and database schema: " + ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> serverError(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
