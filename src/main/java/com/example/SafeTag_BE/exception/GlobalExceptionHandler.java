//package com.example.SafeTag_BE.exception;
//
//import jakarta.validation.ConstraintViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(ApiException.class)
//    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
//        return ResponseEntity.badRequest().body(error(ex.getCode(), ex.getMessage()));
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
//        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
//        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", message));
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
//        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", ex.getMessage()));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(error("INTERNAL_ERROR", ex.getMessage()));
//    }
//
//    private Map<String, Object> error(String code, String message) {
//        Map<String, Object> res = new HashMap<>();
//        Map<String, Object> err = new HashMap<>();
//        err.put("code", code);
//        err.put("message", message);
//        res.put("error", err);
//        return res;
//    }
//}

package com.example.SafeTag_BE.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        log.error("[API_EXCEPTION] {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[VALIDATION_ERROR] {}", message);
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("[CONSTRAINT_VIOLATION] {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", ex.getMessage()));
    }

    //  JSON 요청 파싱 실패 (잘못된 요청 바디 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBodyParse(HttpMessageNotReadableException ex) {
        log.error("[BAD_REQUEST_BODY] {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("BAD_REQUEST", "Invalid or missing JSON body"));
    }

    //  로그인 관련 예외를 401로 내려줌 (NullPointer 등 포함)
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointer(NullPointerException ex) {
        log.error("[NPE/LOGIN_ERROR] {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error("UNAUTHORIZED", "Login failed: internal null reference"));
    }

    //  그 외 모든 예외 — 로그 남기고 INTERNAL_ERROR → 401로 변경 가능
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("[INTERNAL_ERROR] {}", ex.getMessage(), ex);

        // 로그인 중 터진 예외라면 401로 반환
        String lowerMsg = (ex.getMessage() != null) ? ex.getMessage().toLowerCase() : "";
        if (lowerMsg.contains("login") || lowerMsg.contains("auth")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("UNAUTHORIZED", "Login failed"));
        }

        // 일반 예외는 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", ex.getMessage()));
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> err = new HashMap<>();
        err.put("code", code);
        err.put("message", message);
        res.put("error", err);
        return res;
    }
}
