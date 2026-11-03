package com.certichain.core.chain;

import com.certichain.core.model.Certificate;
import com.certichain.core.model.LedgerBlock;

import java.util.List;

/** Outcome of verifying one certificate, plus the individual checks for display. */
public final class VerificationResult {

    public record Check(String name, boolean passed, String detail) {
        public String getName() { return name; }
        public boolean isPassed() { return passed; }
        public String getDetail() { return detail; }
    }

    private final String certNo;
    private final VerificationOutcome outcome;
    private final Certificate certificate;
    private final LedgerBlock issueBlock;
    private final LedgerBlock revokeBlock;
    private final List<Check> checks;

    public VerificationResult(String certNo, VerificationOutcome outcome, Certificate certificate,
                              LedgerBlock issueBlock, LedgerBlock revokeBlock, List<Check> checks) {
        this.certNo = certNo;
        this.outcome = outcome;
        this.certificate = certificate;
        this.issueBlock = issueBlock;
        this.revokeBlock = revokeBlock;
        this.checks = List.copyOf(checks);
    }

    public static VerificationResult notFound(String certNo) {
        return new VerificationResult(certNo, VerificationOutcome.NOT_FOUND, null, null, null,
                List.of(new Check("Certificate number registered", false, "No such certificate number")));
    }

    public String getCertNo() { return certNo; }
    public VerificationOutcome getOutcome() { return outcome; }
    public Certificate getCertificate() { return certificate; }
    public LedgerBlock getIssueBlock() { return issueBlock; }
    public LedgerBlock getRevokeBlock() { return revokeBlock; }
    public List<Check> getChecks() { return checks; }
}
