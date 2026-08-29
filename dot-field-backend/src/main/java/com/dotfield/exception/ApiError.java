package com.dotfield.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardised error response body.
 * <p>
 * Every API error follows the shape:
 * <pre>
 * {
 *   "status": 404,
 *   "message": "Resource not found",
 *   "timestamp": "2025-01-01T12:00:00",
 *   "errors": { ... }   // optional — present for validation errors
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;

}
