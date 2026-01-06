package com.insuretech.pms.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public CustomException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public CustomException(String message, HttpStatus status) {
        this(message, status, status.name());
    }

    // Factory methods
    public static CustomException badRequest(String message) {
        return new CustomException(message, HttpStatus.BAD_REQUEST);
    }

    public static CustomException unauthorized(String message) {
        return new CustomException(message, HttpStatus.UNAUTHORIZED);
    }

    public static CustomException forbidden(String message) {
        return new CustomException(message, HttpStatus.FORBIDDEN);
    }

    public static CustomException notFound(String message) {
        return new CustomException(message, HttpStatus.NOT_FOUND);
    }

    public static CustomException conflict(String message) {
        return new CustomException(message, HttpStatus.CONFLICT);
    }

    public static CustomException internalError(String message) {
        return new CustomException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
