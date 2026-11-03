package com.certichain.desktop.service;

import com.certichain.core.chain.ChainVerifier;
import com.certichain.core.chain.IntegrityReport;
import com.certichain.core.exception.IllegalStateTransitionException;
import com.certichain.core.exception.InvalidCertificateException;
import com.certichain.core.model.BlockType;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.CertificateStatus;
import com.certichain.core.util.CertNoGenerator;
import com.certichain.core.util.CertificateValidator;
import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.dao.CertificateDao;
import com.certichain.desktop.dao.LedgerDao;
import com.certichain.desktop.db.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Business operations of the desktop client. Transactions are managed by hand with
 * JDBC: setAutoCommit(false), commit() on success, rollback() on any failure.
 */
public class RegistrarService {

    private final Database db;
    private final CertificateDao certificates = new CertificateDao();
    private final LedgerDao ledger = new LedgerDao();
    private final AccountDao accounts = new AccountDao();

    public RegistrarService(Database db) {
        this.db = db;
    }

    public Database getDatabase() {
        return db;
    }

    public List<Certificate> allCertificates() throws SQLException {
        try (Connection c = db.connect()) {
            return certificates.findAll(c);
        }
    }

    public LedgerDao.Head head() throws SQLException {
        try (Connection c = db.connect()) {
            return ledger.head(c);
        }
    }

    public Certificate issue(Certificate draft, String actor) throws SQLException {
        return issueAll(List.of(draft), actor).get(0);
    }

    /**
     * Issues every certificate in ONE transaction: all rows and all ledger blocks are written
     * with JDBC batches, and either everything is committed or nothing is.
     */
    public List<Certificate> issueAll(List<Certificate> drafts, String actor) throws SQLException {
        for (Certificate d : drafts) {
            CertificateValidator.validate(d);
        }
        long start = System.currentTimeMillis();
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                LedgerDao.Head head = ledger.lockHead(c);
                Set<String> used = new HashSet<>();
                List<String[]> entries = new ArrayList<>();
                for (Certificate d : drafts) {
                    String no;
                    do {
                        no = CertNoGenerator.next(db.getInstitutionCode());
                    } while (!used.add(no) || certificates.exists(c, no));
                    prepare(d, no, actor);
                    entries.add(new String[]{no, d.getDataHash()});
                }
                certificates.insertBatch(c, drafts);
                ledger.appendAll(c, head, entries, BlockType.ISSUE, actor);
                accounts.audit(c, actor, drafts.size() == 1 ? "ISSUE_CERTIFICATE" : "BULK_ISSUE",
                        drafts.size() == 1 ? drafts.get(0).getCertNo() + " - " + drafts.get(0).getStudentName()
                                : drafts.size() + " certificates from CSV", "SUCCESS", System.currentTimeMillis() - start);
                c.commit();
                return drafts;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void revoke(String certNo, String reason, String actor) throws SQLException {
        if (reason == null || reason.trim().length() < 5) {
            throw new InvalidCertificateException(List.of("Give a revocation reason of at least 5 characters"));
        }
        long start = System.currentTimeMillis();
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                LedgerDao.Head head = ledger.lockHead(c);
                Certificate cert = certificates.find(c, certNo)
                        .orElseThrow(() -> new IllegalStateTransitionException(certNo + " does not exist"));
                if (cert.isRevoked() || ledger.hasRevokeBlock(c, certNo)) {
                    throw new IllegalStateTransitionException(certNo + " is already revoked");
                }
                certificates.markRevoked(c, certNo, reason.trim());
                ledger.appendAll(c, head, List.<String[]>of(new String[]{certNo, Certificate.revocationHash(certNo, reason)}),
                        BlockType.REVOKE, actor);
                accounts.audit(c, actor, "REVOKE_CERTIFICATE", certNo + ": " + reason.trim(), "SUCCESS",
                        System.currentTimeMillis() - start);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Full-chain audit with the multithreaded verifier from the core module. */
    public IntegrityReport audit(String actor) throws SQLException {
        try (Connection c = db.connect()) {
            LedgerDao.Head head = ledger.head(c);
            IntegrityReport report = new ChainVerifier().verify(ledger.findAll(c), certificates.findAll(c), head.seq(), head.hash());
            accounts.audit(c, actor, "RUN_INTEGRITY_AUDIT", report.summary(), report.isIntact() ? "SUCCESS" : "ALERT",
                    report.getDurationMs());
            return report;
        }
    }

    private static void prepare(Certificate d, String certNo, String actor) {
        d.setCertNo(certNo);
        d.setIssuedBy(actor);
        d.setStatus(CertificateStatus.VALID);
        d.setRevokeReason(null);
        d.setStudentName(d.getStudentName().trim().replaceAll("\\s+", " "));
        d.setRollNo(d.getRollNo().trim().toUpperCase());
        d.setEmail(d.getEmail() == null || d.getEmail().isBlank() ? null : d.getEmail().trim().toLowerCase());
        d.setProgram(d.getProgram().trim());
        d.setTitle(d.getTitle().trim());
        d.setGrade(d.getGrade() == null || d.getGrade().isBlank() ? null : d.getGrade().trim());
        d.setDataHash(d.computeHash());
        d.setCreatedAt(System.currentTimeMillis());
    }
}
