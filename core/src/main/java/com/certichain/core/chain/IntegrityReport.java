package com.certichain.core.chain;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class IntegrityReport {

    private final int blocksChecked;
    private final int certificatesChecked;
    private final int threadsUsed;
    private final long durationMs;
    private final long finishedAt;
    private final List<IntegrityIssue> issues;

    public IntegrityReport(int blocksChecked, int certificatesChecked, int threadsUsed,
                           long durationMs, long finishedAt, List<IntegrityIssue> issues) {
        this.blocksChecked = blocksChecked;
        this.certificatesChecked = certificatesChecked;
        this.threadsUsed = threadsUsed;
        this.durationMs = durationMs;
        this.finishedAt = finishedAt;
        this.issues = List.copyOf(issues);
    }

    public boolean isIntact() { return issues.isEmpty(); }
    public int getBlocksChecked() { return blocksChecked; }
    public int getCertificatesChecked() { return certificatesChecked; }
    public int getThreadsUsed() { return threadsUsed; }
    public long getDurationMs() { return durationMs; }
    public long getFinishedAt() { return finishedAt; }
    public java.util.Date getFinishedDate() { return new java.util.Date(finishedAt); }
    public List<IntegrityIssue> getIssues() { return issues; }
    public int getIssueCount() { return issues.size(); }

    /** Certificates that cannot be trusted any more, sorted. */
    public Set<String> getCompromisedCertificates() {
        Set<String> out = new TreeSet<>();
        for (IntegrityIssue i : issues) {
            if (i.certNo() != null) {
                out.add(i.certNo());
            }
        }
        return out;
    }

    public String summary() {
        return isIntact()
                ? "Ledger intact: " + blocksChecked + " blocks, " + certificatesChecked + " certificates verified"
                : issues.size() + " integrity issue(s) found, " + getCompromisedCertificates().size() + " certificate(s) affected";
    }
}
