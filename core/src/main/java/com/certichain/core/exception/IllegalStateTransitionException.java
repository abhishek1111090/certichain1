package com.certichain.core.exception;

/** For example: revoking a certificate that is already revoked. */
public class IllegalStateTransitionException extends CertiChainException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
