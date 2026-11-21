package com.truckingsystem.common;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("An account with this email already exists");
    }
}
