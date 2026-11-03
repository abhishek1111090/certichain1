package com.certichain.core.model;

import java.time.Instant;

/**
 * One immutable block of the ledger. {@code blockHash} covers every other field,
 * including {@code prevHash}, so editing or deleting any block breaks the chain
 * from that point onwards.
 */
public final class LedgerBlock implements Hashable {

    public static final String GENESIS_HASH = "0".repeat(64);

    private final long seq;
    private final BlockType type;
    private final String certNo;
    private final String dataHash;
    private final String prevHash;
    private final String blockHash;
    private final String actor;
    private final long createdAt;

    public LedgerBlock(long seq, BlockType type, String certNo, String dataHash,
                       String prevHash, String blockHash, String actor, long createdAt) {
        this.seq = seq;
        this.type = type;
        this.certNo = certNo;
        this.dataHash = dataHash;
        this.prevHash = prevHash;
        this.blockHash = blockHash;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    /** Factory used when appending: computes and fills in the block hash. */
    public static LedgerBlock seal(long seq, BlockType type, String certNo, String dataHash,
                                   String prevHash, String actor, long createdAt) {
        LedgerBlock unsealed = new LedgerBlock(seq, type, certNo, dataHash, prevHash, null, actor, createdAt);
        return new LedgerBlock(seq, type, certNo, dataHash, prevHash, unsealed.computeHash(), actor, createdAt);
    }

    @Override
    public String canonicalForm() {
        return seq + "|" + type + "|" + certNo + "|" + dataHash + "|" + prevHash + "|" + actor + "|" + createdAt;
    }

    public boolean isSelfConsistent() {
        return computeHash().equals(blockHash);
    }

    public long getSeq() { return seq; }
    public BlockType getType() { return type; }
    public String getCertNo() { return certNo; }
    public String getDataHash() { return dataHash; }
    public String getPrevHash() { return prevHash; }
    public String getBlockHash() { return blockHash; }
    public String getActor() { return actor; }
    public long getCreatedAt() { return createdAt; }
    public Instant getCreatedInstant() { return Instant.ofEpochMilli(createdAt); }
    public java.util.Date getCreatedDate() { return new java.util.Date(createdAt); }
    public String getShortHash() { return blockHash == null ? "" : blockHash.substring(0, 12); }

    @Override
    public String toString() {
        return "#" + seq + " " + type + " " + certNo + " " + getShortHash();
    }
}
