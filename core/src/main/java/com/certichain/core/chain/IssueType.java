package com.certichain.core.chain;

public enum IssueType {
    BLOCK_HASH_MISMATCH("A ledger block was edited after it was sealed"),
    BROKEN_LINK("A block no longer points at the previous block (inserted/deleted/reordered)"),
    SEQUENCE_GAP("Block numbers are not continuous (a block was deleted)"),
    CERT_DATA_MODIFIED("Certificate data no longer matches what was sealed at issue time"),
    STORED_HASH_MISMATCH("The stored data hash column was edited"),
    CERT_MISSING("The ledger seals a certificate that no longer exists (row deleted)"),
    UNREGISTERED_CERT("A certificate exists in the database without any ledger block (forged entry)"),
    REVOCATION_HIDDEN("The ledger says REVOKED but the database row says VALID"),
    REVOCATION_FORGED("The database says REVOKED but the ledger has no matching revocation"),
    HEAD_MISMATCH("The ledger head pointer does not match the last block");

    private final String description;

    IssueType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
