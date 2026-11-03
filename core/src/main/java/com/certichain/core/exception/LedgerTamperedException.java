package com.certichain.core.exception;

/** Raised when a write is refused because the ledger head no longer matches the chain. */
public class LedgerTamperedException extends CertiChainException {

    public LedgerTamperedException(String message) {
        super(message);
    }
}
