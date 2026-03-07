package edu.cit.arnejo.dormshare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private boolean success;
    private Object data;
    private ErrorDetail error;
    private String timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private Object details;
    }

    public static AuthResponse ok(Object data) {
        return AuthResponse.builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    public static AuthResponse error(String code, String message, Object details) {
        return AuthResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
