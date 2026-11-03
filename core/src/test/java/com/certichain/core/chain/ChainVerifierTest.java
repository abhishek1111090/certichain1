package com.certichain.core.chain;

import com.certichain.core.crypto.PasswordHasher;
import com.certichain.core.model.BlockType;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.CertificateStatus;
import com.certichain.core.model.CredentialType;
import com.certichain.core.model.LedgerBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainVerifierTest {

    private final List<LedgerBlock> blocks = new ArrayList<>();
    private final List<Certificate> certs = new ArrayList<>();

    @BeforeEach
    void buildLedger() {
        for (int i = 1; i <= 300; i++) {
            Certificate c = new Certificate();
            c.setCertNo("MUJ-2026-T" + i);
            c.setStudentName("Student " + i);
            c.setRollNo("22CS" + i);
            c.setProgram("B.Tech CSE");
            c.setCredentialType(CredentialType.COURSE_COMPLETION);
            c.setTitle("Advanced Java");
            c.setGrade("A");
            c.setIssueDate(LocalDate.of(2026, 5, 1));
            c.setIssuedBy("admin");
            c.setDataHash(c.computeHash());
            certs.add(c);
            append(BlockType.ISSUE, c.getCertNo(), c.getDataHash());
        }
    }

    private void append(BlockType type, String certNo, String dataHash) {
        String prev = blocks.isEmpty() ? LedgerBlock.GENESIS_HASH : blocks.get(blocks.size() - 1).getBlockHash();
        blocks.add(LedgerBlock.seal(blocks.size() + 1, type, certNo, dataHash, prev, "admin", 1_700_000_000_000L + blocks.size()));
    }

    private IntegrityReport audit() {
        LedgerBlock last = blocks.get(blocks.size() - 1);
        return new ChainVerifier(4).verify(blocks, certs, last.getSeq(), last.getBlockHash());
    }

    @Test
    void untouchedLedgerIsIntact() {
        IntegrityReport r = audit();
        assertTrue(r.isIntact(), r.getIssues().toString());
        assertEquals(300, r.getBlocksChecked());
    }

    @Test
    void editingAGradeIsDetected() {
        certs.get(41).setGrade("A+");
        IntegrityReport r = audit();
        assertFalse(r.isIntact());
        assertEquals(IssueType.CERT_DATA_MODIFIED, r.getIssues().get(0).type());
        assertEquals("MUJ-2026-T42", r.getIssues().get(0).certNo());
    }

    @Test
    void deletingABlockIsDetected() {
        blocks.remove(150);
        IntegrityReport r = audit();
        assertTrue(r.getIssues().stream().anyMatch(i -> i.type() == IssueType.BROKEN_LINK));
        assertTrue(r.getIssues().stream().anyMatch(i -> i.type() == IssueType.UNREGISTERED_CERT));
    }

    @Test
    void forgedRowWithoutBlockIsDetected() {
        Certificate forged = new Certificate();
        forged.setCertNo("MUJ-2026-FAKE");
        forged.setStudentName("Mallory");
        forged.setRollNo("XX01");
        forged.setProgram("B.Tech CSE");
        forged.setCredentialType(CredentialType.DEGREE);
        forged.setTitle("B.Tech");
        forged.setIssueDate(LocalDate.now());
        forged.setIssuedBy("admin");
        forged.setDataHash(forged.computeHash());
        certs.add(forged);
        IntegrityReport r = audit();
        assertEquals(1, r.getIssueCount());
        assertEquals(IssueType.UNREGISTERED_CERT, r.getIssues().get(0).type());
    }

    @Test
    void singleCertificateVerification() {
        Certificate c = certs.get(9);
        LedgerBlock issue = blocks.get(9);
        assertEquals(VerificationOutcome.AUTHENTIC,
                CertificateVerifier.verify(c, List.of(issue), blocks.get(8), blocks.get(10)).getOutcome());

        c.setStudentName("Someone Else");
        assertEquals(VerificationOutcome.TAMPERED,
                CertificateVerifier.verify(c, List.of(issue), blocks.get(8), blocks.get(10)).getOutcome());
    }

    @Test
    void revocationCannotBeHidden() {
        Certificate c = certs.get(0);
        c.setStatus(CertificateStatus.REVOKED);
        c.setRevokeReason("Academic misconduct");
        append(BlockType.REVOKE, c.getCertNo(), c.revocationHash());
        assertTrue(audit().isIntact());

        c.setStatus(CertificateStatus.VALID);
        IntegrityReport r = audit();
        assertEquals(IssueType.REVOCATION_HIDDEN, r.getIssues().get(0).type());
        assertEquals(VerificationOutcome.REVOKED, CertificateVerifier.verify(c,
                List.of(blocks.get(0), blocks.get(blocks.size() - 1)), null, blocks.get(1)).getOutcome());
    }

    @Test
    void passwordHashing() {
        String h = PasswordHasher.hash("secret".toCharArray());
        assertTrue(PasswordHasher.verify("secret".toCharArray(), h));
        assertFalse(PasswordHasher.verify("Secret".toCharArray(), h));
    }
}
