package com.truckingsystem.common;

public class ExternalIdentityProviderException extends RuntimeException {
    public ExternalIdentityProviderException(String message) {
        super(message);
    }

    public ExternalIdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
