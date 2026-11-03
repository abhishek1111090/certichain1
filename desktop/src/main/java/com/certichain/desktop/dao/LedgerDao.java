package com.certichain.desktop.dao;

import com.certichain.core.exception.LedgerTamperedException;
import com.certichain.core.model.BlockType;
import com.certichain.core.model.LedgerBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Ledger access over raw JDBC. Appends must run with auto-commit off (inside a transaction). */
public class LedgerDao {

    public record Head(long seq, String hash) {
    }

    /** Locks the head row until the surrounding transaction ends. */
    public Head lockHead(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_seq, last_hash FROM ledger_head WHERE id = 1 FOR UPDATE")) {
            if (!rs.next()) {
                throw new SQLException("ledger_head row is missing");
            }
            Head head = new Head(rs.getLong(1), rs.getString(2));
            LedgerBlock last = findLast(c);
            long lastSeq = last == null ? 0 : last.getSeq();
            String lastHash = last == null ? LedgerBlock.GENESIS_HASH : last.getBlockHash();
            if (lastSeq != head.seq() || !lastHash.equals(head.hash())) {
                throw new LedgerTamperedException("Ledger head #" + head.seq() + " does not match last block #" + lastSeq
                        + ". Writes are frozen until an administrator investigates.");
            }
            return head;
        }
    }

    /** Seals the given blocks after {@code head} and writes them with one JDBC batch, then moves the head. */
    public List<LedgerBlock> appendAll(Connection c, Head head, List<String[]> entries, BlockType type, String actor)
            throws SQLException {
        List<LedgerBlock> sealed = new ArrayList<>();
        long seq = head.seq();
        String prev = head.hash();
        long now = System.currentTimeMillis();
        for (String[] e : entries) {
            LedgerBlock b = LedgerBlock.seal(++seq, type, e[0], e[1], prev, actor, now);
            sealed.add(b);
            prev = b.getBlockHash();
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO ledger_block (seq, block_type, cert_no, data_hash, prev_hash, block_hash, actor, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""")) {
            for (LedgerBlock b : sealed) {
                ps.setLong(1, b.getSeq());
                ps.setString(2, b.getType().name());
                ps.setString(3, b.getCertNo());
                ps.setString(4, b.getDataHash());
                ps.setString(5, b.getPrevHash());
                ps.setString(6, b.getBlockHash());
                ps.setString(7, b.getActor());
                ps.setLong(8, b.getCreatedAt());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        if (!sealed.isEmpty()) {
            LedgerBlock last = sealed.get(sealed.size() - 1);
            try (PreparedStatement ps = c.prepareStatement("UPDATE ledger_head SET last_seq = ?, last_hash = ? WHERE id = 1")) {
                ps.setLong(1, last.getSeq());
                ps.setString(2, last.getBlockHash());
                ps.executeUpdate();
            }
        }
        return sealed;
    }

    public Head head(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_seq, last_hash FROM ledger_head WHERE id = 1")) {
            return rs.next() ? new Head(rs.getLong(1), rs.getString(2)) : new Head(0, LedgerBlock.GENESIS_HASH);
        }
    }

    public List<LedgerBlock> findAll(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ledger_block ORDER BY seq")) {
            List<LedgerBlock> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        }
    }

    public LedgerBlock findLast(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ledger_block ORDER BY seq DESC LIMIT 1")) {
            return rs.next() ? map(rs) : null;
        }
    }

    public boolean hasRevokeBlock(Connection c, String certNo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM ledger_block WHERE cert_no = ? AND block_type = 'REVOKE'")) {
            ps.setString(1, certNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static LedgerBlock map(ResultSet rs) throws SQLException {
        return new LedgerBlock(rs.getLong("seq"), BlockType.valueOf(rs.getString("block_type")),
                rs.getString("cert_no"), rs.getString("data_hash"), rs.getString("prev_hash"),
                rs.getString("block_hash"), rs.getString("actor"), rs.getLong("created_at"));
    }
}
