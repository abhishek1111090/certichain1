package com.certichain.core.chain;

/**
 * One problem found by the integrity audit.
 *
 * @param seq    ledger block number involved, or -1 when the issue is not tied to a block
 * @param certNo certificate involved, or null
 */
public record IntegrityIssue(IssueType type, long seq, String certNo, String detail) {

    public String getType() { return type.name(); }
    public String getDescription() { return type.getDescription(); }
    public long getSeq() { return seq; }
    public String getCertNo() { return certNo; }
    public String getDetail() { return detail; }
}
