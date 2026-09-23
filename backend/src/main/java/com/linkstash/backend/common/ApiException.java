package com.linkstash.backend.common;

public class ApiException extends RuntimeException {
    private final int status;
    private final String message;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(401, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message);
    }
}
