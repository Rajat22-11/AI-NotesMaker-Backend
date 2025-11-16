package com.contentprocessor.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * Generic wrapper for all API responses.
 * Provides a consistent structure across all endpoints.
 *
 * Example success response:
 * {
 *   "success": true,
 *   "message": "Job created successfully",
 *   "data": { ... job details ... },
 *   "timestamp": "2025-10-25T10:30:00"
 * }
 *
 * Example error response:
 * {
 *   "success": false,
 *   "message": "Job not found with id: 123",
 *   "timestamp": "2025-10-25T10:30:00"
 * }
 *
 * @param <T> The type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) //Only includes non-null fields in JSON
public class ApiResponse<T> {

    private boolean success;
    private String message;

    //Actual Data payload
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    //success res.
    public static <T> ApiResponse<T> success(String message, T data){
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    //success res. without data
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    //error res.
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
