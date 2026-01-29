package com.csee.swplus.mileage.auth.exception;

public class DoNotExistException extends RuntimeException {
    public DoNotExistException(String message) {
        super(message);
    }
}