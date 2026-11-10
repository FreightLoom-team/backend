package com.truckingsystem.common;

public class InvalidSignUpRequestException extends RuntimeException {
    public InvalidSignUpRequestException(String message) {
        super(message);
    }
}
