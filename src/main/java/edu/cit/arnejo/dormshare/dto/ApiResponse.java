package edu.cit.arnejo.dormshare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private boolean success;
    private Object data;
    private ErrorDetail error;
    private String timestamp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private Object details;

        public ErrorDetail() {}

        public ErrorDetail(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Object getDetails() { return details; }
        public void setCode(String code) { this.code = code; }
        public void setMessage(String message) { this.message = message; }
        public void setDetails(Object details) { this.details = details; }
    }

    // Getters
    public boolean isSuccess() { return success; }
    public Object getData() { return data; }
    public ErrorDetail getError() { return error; }
    public String getTimestamp() { return timestamp; }

    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setData(Object data) { this.data = data; }
    public void setError(ErrorDetail error) { this.error = error; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public static ApiResponse ok(Object data) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now().toString());
        return response;
    }

    public static ApiResponse error(String code, String message, Object details) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(false);
        response.setError(new ErrorDetail(code, message, details));
        response.setTimestamp(LocalDateTime.now().toString());
        return response;
    }
}
