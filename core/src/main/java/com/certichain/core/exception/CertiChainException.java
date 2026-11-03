package com.certichain.core.exception;

/** Root of the CertiChain exception hierarchy. */
public class CertiChainException extends RuntimeException {

    public CertiChainException(String message) {
        super(message);
    }

    public CertiChainException(String message, Throwable cause) {
        super(message, cause);
    }
}
