package com.dotfield.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper.
 * <p>
 * Every successful API response follows the shape:
 * <pre>
 * {
 *   "data": { ... },
 *   "message": "Success"
 * }
 * </pre>
 *
 * @param <T> type of the response payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private T data;
    private String message;

    /**
     * Create a successful response with data and a default message.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .message("Success")
                .build();
    }

    /**
     * Create a successful response with data and a custom message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .build();
    }

}
