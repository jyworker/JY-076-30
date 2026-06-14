package com.iyzipay.exception;

public class DuplicateTagException extends RuntimeException {

    public DuplicateTagException(String message) {
        super(message);
    }

    public DuplicateTagException(String message, Throwable cause) {
        super(message, cause);
    }
}
