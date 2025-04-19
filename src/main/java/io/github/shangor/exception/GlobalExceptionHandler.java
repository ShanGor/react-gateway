package io.github.shangor.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({UserException.class})
    public ResponseEntity<Map<String, String>> handleException(UserException e) {
        return ResponseEntity.status(e.getCode())
                .body(Map.of("errorCode", e.getErrorCode(), "message", e.getMessage()));
    }
}
