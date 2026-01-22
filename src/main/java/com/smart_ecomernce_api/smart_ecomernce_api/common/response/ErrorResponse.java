package com.smart_ecomernce_api.smart_ecomernce_api.common.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String status;
    private String message;
    private int statusCode;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> errors;
    private String traceId;

    public static ErrorResponse of(String message, int statusCode, String path) {
        return ErrorResponse.builder()
                .status("error")
                .message(message)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}