package com.example.Tech.exception;

import com.example.Tech.dto.response.common.ApiError;
import com.example.Tech.dto.response.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException ex) {
        return build(ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // Type conversion failures (e.g. ?categoryId=abc) carry a long technical message
            String message = fieldError.isBindingFailure() ? "Invalid value" : fieldError.getDefaultMessage();
            details.putIfAbsent(fieldError.getField(), message);
        }
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResult<Void>> handleMalformed(Exception ex) {
        return build(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.getDefaultMessage(), null);
    }

    /** Unknown sort property, e.g. ?sort=foo */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResult<Void>> handleUnknownProperty(PropertyReferenceException ex) {
        return build(ErrorCode.MALFORMED_REQUEST, "Unknown sort property '%s'".formatted(ex.getPropertyName()), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_INTEGRITY_VIOLATION, ErrorCode.DATA_INTEGRITY_VIOLATION.getDefaultMessage(), null);
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ApiResult<Void>> handleFrameworkError(Exception ex) {
        ErrorResponse errorResponse = (ErrorResponse) ex;
        HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
        ApiError error = new ApiError(status.name(), errorResponse.getBody().getDetail(), null);
        return ResponseEntity.status(errorResponse.getStatusCode()).body(ApiResult.fail(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null);
    }

    private ResponseEntity<ApiResult<Void>> build(ErrorCode code, String message, Map<String, String> details) {
        ApiError error = new ApiError(code.name(), message, details);
        return ResponseEntity.status(code.getStatus()).body(ApiResult.fail(error));
    }
}
