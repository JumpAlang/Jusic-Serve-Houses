package com.scoder.jusic.security;

/** Internal exception; it is translated to the existing NOTICE response. */
public class ProviderCallException extends RuntimeException {

    public ProviderCallException(String message) {
        super(message);
    }

    public ProviderCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
