package com.certichain.core.model;

public enum BlockType {
    /** A certificate was issued; the block seals the certificate's data hash. */
    ISSUE,
    /** A certificate was revoked; the block seals the revocation (cert no + reason). */
    REVOKE
}
