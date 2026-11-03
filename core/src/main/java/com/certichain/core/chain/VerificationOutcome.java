package com.certichain.core.chain;

public enum VerificationOutcome {
    AUTHENTIC("Authentic", "This certificate is genuine and has not been altered since it was issued."),
    REVOKED("Revoked", "This certificate was genuinely issued but has since been revoked by the institution."),
    TAMPERED("Tampered", "This record does not match the sealed ledger. Do not trust this certificate."),
    NOT_FOUND("Not found", "No certificate with this number was ever issued by the institution.");

    private final String label;
    private final String explanation;

    VerificationOutcome(String label, String explanation) {
        this.label = label;
        this.explanation = explanation;
    }

    public String getLabel() { return label; }
    public String getExplanation() { return explanation; }
}
