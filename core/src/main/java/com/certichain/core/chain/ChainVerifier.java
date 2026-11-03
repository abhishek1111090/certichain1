package com.certichain.core.chain;

import com.certichain.core.model.BlockType;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.LedgerBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Full-ledger integrity audit. Re-hashing thousands of blocks and certificates is
 * CPU bound and every block can be checked independently once the list is in memory,
 * so the work is split into chunks and run on a thread pool (Callable + Future).
 * Cross-checks that need the whole picture run afterwards on the calling thread.
 */
public class ChainVerifier {

    private static final int MIN_CHUNK = 64;

    private final int threads;

    public ChainVerifier() {
        this(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public ChainVerifier(int threads) {
        this.threads = threads;
    }

    /**
     * @param blocks   every ledger block, ordered by seq
     * @param certs    every certificate row
     * @param headSeq  last_seq stored in ledger_head
     * @param headHash last_hash stored in ledger_head
     */
    public IntegrityReport verify(List<LedgerBlock> blocks, Collection<Certificate> certs,
                                  long headSeq, String headHash) {
        long start = System.nanoTime();
        Map<String, Certificate> certByNo = new HashMap<>();
        for (Certificate c : certs) {
            certByNo.put(c.getCertNo(), c);
        }

        List<IntegrityIssue> issues = new ArrayList<>();
        AtomicInteger blocksChecked = new AtomicInteger();
        int chunkSize = Math.max(MIN_CHUNK, (blocks.size() + threads - 1) / Math.max(1, threads));
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "ledger-audit");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<List<IntegrityIssue>>> futures = new ArrayList<>();
            for (int from = 0; from < blocks.size(); from += chunkSize) {
                int to = Math.min(blocks.size(), from + chunkSize);
                futures.add(pool.submit(new ChunkTask(blocks, from, to, certByNo, blocksChecked)));
            }
            for (Future<List<IntegrityIssue>> f : futures) {
                issues.addAll(f.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Integrity audit interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Integrity audit failed", e.getCause());
        } finally {
            pool.shutdown();
        }

        crossCheck(blocks, certByNo, headSeq, headHash, issues);
        issues.sort((a, b) -> Long.compare(a.seq() < 0 ? Long.MAX_VALUE : a.seq(), b.seq() < 0 ? Long.MAX_VALUE : b.seq()));

        long ms = (System.nanoTime() - start) / 1_000_000;
        return new IntegrityReport(blocksChecked.get(), certByNo.size(), threads, ms, System.currentTimeMillis(), issues);
    }

    private void crossCheck(List<LedgerBlock> blocks, Map<String, Certificate> certByNo,
                            long headSeq, String headHash, List<IntegrityIssue> issues) {
        Set<String> issued = new HashSet<>();
        Map<String, LedgerBlock> revoked = new HashMap<>();
        for (LedgerBlock b : blocks) {
            if (b.getType() == BlockType.ISSUE) {
                issued.add(b.getCertNo());
            } else {
                revoked.put(b.getCertNo(), b);
            }
        }
        for (Certificate c : certByNo.values()) {
            if (!issued.contains(c.getCertNo())) {
                issues.add(new IntegrityIssue(IssueType.UNREGISTERED_CERT, -1, c.getCertNo(),
                        "Row for " + c.getStudentName() + " was inserted directly into the database"));
            }
            if (c.isRevoked() && !revoked.containsKey(c.getCertNo())) {
                issues.add(new IntegrityIssue(IssueType.REVOCATION_FORGED, -1, c.getCertNo(),
                        "Status was set to REVOKED without a REVOKE block"));
            }
        }
        LedgerBlock last = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
        long lastSeq = last == null ? 0 : last.getSeq();
        String lastHash = last == null ? LedgerBlock.GENESIS_HASH : last.getBlockHash();
        if (lastSeq != headSeq || !lastHash.equals(headHash)) {
            issues.add(new IntegrityIssue(IssueType.HEAD_MISMATCH, lastSeq, null,
                    "Head says #" + headSeq + ", last block is #" + lastSeq + " (trailing blocks deleted or appended outside the app)"));
        }
    }

    /** Checks blocks [from, to). Reads the shared lists only, so no locking is needed. */
    private static final class ChunkTask implements Callable<List<IntegrityIssue>> {
        private final List<LedgerBlock> blocks;
        private final int from;
        private final int to;
        private final Map<String, Certificate> certByNo;
        private final AtomicInteger counter;

        ChunkTask(List<LedgerBlock> blocks, int from, int to, Map<String, Certificate> certByNo, AtomicInteger counter) {
            this.blocks = blocks;
            this.from = from;
            this.to = to;
            this.certByNo = certByNo;
            this.counter = counter;
        }

        @Override
        public List<IntegrityIssue> call() {
            List<IntegrityIssue> found = new ArrayList<>();
            for (int i = from; i < to; i++) {
                LedgerBlock b = blocks.get(i);
                long expectedSeq = i == 0 ? 1 : blocks.get(i - 1).getSeq() + 1;
                String expectedPrev = i == 0 ? LedgerBlock.GENESIS_HASH : blocks.get(i - 1).getBlockHash();

                if (b.getSeq() != expectedSeq) {
                    found.add(new IntegrityIssue(IssueType.SEQUENCE_GAP, b.getSeq(), b.getCertNo(),
                            "Expected block #" + expectedSeq + " but found #" + b.getSeq()));
                }
                if (!b.isSelfConsistent()) {
                    found.add(new IntegrityIssue(IssueType.BLOCK_HASH_MISMATCH, b.getSeq(), b.getCertNo(),
                            "Stored " + abbrev(b.getBlockHash()) + ", recomputed " + abbrev(b.computeHash())));
                }
                if (!b.getPrevHash().equals(expectedPrev)) {
                    found.add(new IntegrityIssue(IssueType.BROKEN_LINK, b.getSeq(), b.getCertNo(),
                            "prev_hash " + abbrev(b.getPrevHash()) + " should be " + abbrev(expectedPrev)));
                }
                checkCertificate(b, found);
                counter.incrementAndGet();
            }
            return found;
        }

        private void checkCertificate(LedgerBlock b, List<IntegrityIssue> found) {
            Certificate c = certByNo.get(b.getCertNo());
            if (c == null) {
                if (b.getType() == BlockType.ISSUE) {
                    found.add(new IntegrityIssue(IssueType.CERT_MISSING, b.getSeq(), b.getCertNo(),
                            "Certificate row was deleted from the database"));
                }
                return;
            }
            if (b.getType() == BlockType.ISSUE) {
                String recomputed = c.computeHash();
                if (!recomputed.equals(b.getDataHash())) {
                    found.add(new IntegrityIssue(IssueType.CERT_DATA_MODIFIED, b.getSeq(), c.getCertNo(),
                            "Sealed " + abbrev(b.getDataHash()) + ", current data hashes to " + abbrev(recomputed)));
                }
                if (!b.getDataHash().equals(c.getDataHash())) {
                    found.add(new IntegrityIssue(IssueType.STORED_HASH_MISMATCH, b.getSeq(), c.getCertNo(),
                            "data_hash column differs from the sealed value"));
                }
            } else if (!c.isRevoked()) {
                found.add(new IntegrityIssue(IssueType.REVOCATION_HIDDEN, b.getSeq(), c.getCertNo(),
                        "Revoked in block #" + b.getSeq() + " but the row says VALID"));
            } else if (!c.revocationHash().equals(b.getDataHash())) {
                found.add(new IntegrityIssue(IssueType.CERT_DATA_MODIFIED, b.getSeq(), c.getCertNo(),
                        "Revocation reason was edited after revocation"));
            }
        }
    }

    static String abbrev(String hash) {
        return hash == null ? "null" : hash.length() > 12 ? hash.substring(0, 12) + "..." : hash;
    }
}
