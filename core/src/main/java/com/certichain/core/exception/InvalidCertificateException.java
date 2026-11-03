package com.certichain.core.exception;

import java.util.List;

/** Thrown when certificate input fails validation; carries every problem found. */
public class InvalidCertificateException extends CertiChainException {

    private final List<String> errors;

    public InvalidCertificateException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
