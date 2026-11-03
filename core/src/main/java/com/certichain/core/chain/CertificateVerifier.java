package com.certichain.core.chain;

import com.certichain.core.model.BlockType;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.LedgerBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies a single certificate against its own ledger blocks and the immediate
 * neighbours of its ISSUE block. Cheap enough to run on every public lookup.
 */
public final class CertificateVerifier {

    private CertificateVerifier() {
    }

    /**
     * @param cert        the certificate row, never null
     * @param certBlocks  all blocks whose cert_no is this certificate
     * @param prevOfIssue the block just before the ISSUE block (null for block #1)
     * @param nextOfIssue the block just after the ISSUE block (null if it is the last one)
     */
    public static VerificationResult verify(Certificate cert, List<LedgerBlock> certBlocks,
                                            LedgerBlock prevOfIssue, LedgerBlock nextOfIssue) {
        List<VerificationResult.Check> checks = new ArrayList<>();
        LedgerBlock issue = null;
        LedgerBlock revoke = null;
        for (LedgerBlock b : certBlocks) {
            if (b.getType() == BlockType.ISSUE && issue == null) {
                issue = b;
            } else if (b.getType() == BlockType.REVOKE) {
                revoke = b;
            }
        }

        boolean registered = issue != null;
        checks.add(new VerificationResult.Check("Registered in the ledger", registered,
                registered ? "Sealed in block #" + issue.getSeq() : "No ISSUE block exists for this number"));
        if (!registered) {
            return new VerificationResult(cert.getCertNo(), VerificationOutcome.TAMPERED, cert, null, null, checks);
        }

        boolean blockIntact = issue.isSelfConsistent();
        checks.add(new VerificationResult.Check("Ledger block unaltered", blockIntact,
                "Block hash " + ChainVerifier.abbrev(issue.getBlockHash())));

        boolean linkedBack = issue.getSeq() == 1
                ? LedgerBlock.GENESIS_HASH.equals(issue.getPrevHash())
                : prevOfIssue != null && prevOfIssue.getBlockHash().equals(issue.getPrevHash());
        boolean linkedForward = nextOfIssue == null || issue.getBlockHash().equals(nextOfIssue.getPrevHash());
        checks.add(new VerificationResult.Check("Chain links intact", linkedBack && linkedForward,
                linkedBack && linkedForward
                        ? "Linked to " + (issue.getSeq() == 1 ? "genesis" : "block #" + (issue.getSeq() - 1))
                          + (nextOfIssue == null ? " (latest block)" : " and block #" + nextOfIssue.getSeq())
                        : "Neighbouring blocks do not point at each other"));

        String recomputed = cert.computeHash();
        boolean dataMatches = recomputed.equals(issue.getDataHash());
        checks.add(new VerificationResult.Check("Certificate data matches seal", dataMatches,
                dataMatches ? "SHA-256 " + ChainVerifier.abbrev(recomputed)
                        : "Name, grade, title or another field was changed after issue"));

        boolean revokedInLedger = revoke != null && revoke.isSelfConsistent();
        boolean revocationConsistent = cert.isRevoked() == revokedInLedger
                && (!revokedInLedger || cert.revocationHash().equals(revoke.getDataHash()));
        checks.add(new VerificationResult.Check("Revocation status consistent", revocationConsistent,
                revokedInLedger ? "Revoked in block #" + revoke.getSeq()
                        : cert.isRevoked() ? "Marked revoked without a ledger record" : "Never revoked"));

        VerificationOutcome outcome;
        if (!blockIntact || !linkedBack || !linkedForward || !dataMatches) {
            outcome = VerificationOutcome.TAMPERED;
        } else if (revokedInLedger && cert.isRevoked() && !revocationConsistent) {
            outcome = VerificationOutcome.TAMPERED;
        } else if (revokedInLedger) {
            // The ledger wins: even if someone flipped the row back to VALID, it stays revoked.
            outcome = VerificationOutcome.REVOKED;
        } else if (cert.isRevoked()) {
            outcome = VerificationOutcome.TAMPERED;
        } else {
            outcome = VerificationOutcome.AUTHENTIC;
        }
        return new VerificationResult(cert.getCertNo(), outcome, cert, issue, revoke, checks);
    }
}
