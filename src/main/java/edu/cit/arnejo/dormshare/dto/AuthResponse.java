package edu.cit.arnejo.dormshare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private boolean success;
    private Object data;
    private ErrorDetail error;
    private String timestamp;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private Object details;

        // Getters
        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Object getDetails() {
            return details;
        }

        // Setters
        public void setCode(String code) {
            this.code = code;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setDetails(Object details) {
            this.details = details;
        }
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public ErrorDetail getError() {
        return error;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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
