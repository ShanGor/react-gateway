package io.github.shangor.exception;

import lombok.Getter;

@Getter
public class UserException extends RuntimeException{
    private final String message;
    private final String errorCode;
    private final int code;
    public UserException(int code, String errorCode, String message) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
        this.code = code;
    }
}
