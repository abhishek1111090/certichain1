package com.certichain.core.model;

import com.certichain.core.crypto.HashUtil;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An academic credential. Every field that appears in {@link #canonicalForm()} is
 * protected by the ledger: changing any of them in the database (even by a DBA)
 * makes the certificate fail verification.
 */
public class Certificate implements Hashable {

    private String certNo;
    private String studentName;
    private String rollNo;
    private String email;
    private String program;
    private CredentialType credentialType;
    private String title;
    private String grade;
    private LocalDate issueDate;
    private String issuedBy;
    private CertificateStatus status = CertificateStatus.VALID;
    private String revokeReason;
    private String dataHash;
    private long createdAt;

    @Override
    public String canonicalForm() {
        return String.join("|",
                nz(certNo), nz(studentName).trim(), nz(rollNo).trim(), nz(email).trim().toLowerCase(),
                nz(program).trim(), credentialType == null ? "" : credentialType.name(),
                nz(title).trim(), nz(grade).trim(), issueDate == null ? "" : issueDate.toString(),
                nz(issuedBy));
    }

    /** Hash that a REVOKE block must carry for this certificate. */
    public String revocationHash() {
        return revocationHash(certNo, revokeReason);
    }

    public static String revocationHash(String certNo, String reason) {
        return HashUtil.sha256Hex("REVOKE|" + nz(certNo) + "|" + nz(reason).trim());
    }

    public boolean isRevoked() {
        return status == CertificateStatus.REVOKED;
    }

    /** e.g. "05 May 2026", for printing. */
    public String getIssueDateDisplay() {
        return issueDate == null ? "" : issueDate.format(DISPLAY_DATE);
    }

    private static final java.time.format.DateTimeFormatter DISPLAY_DATE =
            java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH);

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    public String getCertNo() { return certNo; }
    public void setCertNo(String certNo) { this.certNo = certNo; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public CredentialType getCredentialType() { return credentialType; }
    public void setCredentialType(CredentialType credentialType) { this.credentialType = credentialType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }
    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }
    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Certificate c && Objects.equals(certNo, c.certNo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(certNo);
    }

    @Override
    public String toString() {
        return certNo + " (" + studentName + ", " + title + ")";
    }
}
