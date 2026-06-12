package com.example.troubleticket.model;

public class ErrorResponse {
    private String code;
    private String reason;
    private String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String reason, String message) {
        this.code = code;
        this.reason = reason;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
