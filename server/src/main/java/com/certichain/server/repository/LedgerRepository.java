package com.certichain.server.repository;

import com.certichain.core.exception.LedgerTamperedException;
import com.certichain.core.model.BlockType;
import com.certichain.core.model.LedgerBlock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LedgerRepository {

    static final RowMapper<LedgerBlock> MAPPER = (rs, n) -> new LedgerBlock(
            rs.getLong("seq"),
            BlockType.valueOf(rs.getString("block_type")),
            rs.getString("cert_no"),
            rs.getString("data_hash"),
            rs.getString("prev_hash"),
            rs.getString("block_hash"),
            rs.getString("actor"),
            rs.getLong("created_at"));

    public record Head(long seq, String hash) {
        public long getSeq() { return seq; }
        public String getHash() { return hash; }
    }

    private final JdbcTemplate jdbc;

    public LedgerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Appends a block. Must run inside the caller's transaction: the head row is locked
     * with SELECT ... FOR UPDATE, so concurrent writers (other requests, or the Swing
     * client on the same database) queue up instead of forking the chain.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerBlock append(BlockType type, String certNo, String dataHash, String actor) {
        Map<String, Object> row = jdbc.queryForMap("SELECT last_seq, last_hash FROM ledger_head WHERE id = 1 FOR UPDATE");
        long headSeq = ((Number) row.get("last_seq")).longValue();
        String headHash = (String) row.get("last_hash");

        Optional<LedgerBlock> last = findLast();
        long lastSeq = last.map(LedgerBlock::getSeq).orElse(0L);
        String lastHash = last.map(LedgerBlock::getBlockHash).orElse(LedgerBlock.GENESIS_HASH);
        if (lastSeq != headSeq || !lastHash.equals(headHash)) {
            throw new LedgerTamperedException("Ledger head (#" + headSeq + ") does not match the last block (#"
                    + lastSeq + "). Writes are frozen until an administrator investigates.");
        }

        LedgerBlock block = LedgerBlock.seal(headSeq + 1, type, certNo, dataHash, headHash, actor, System.currentTimeMillis());
        jdbc.update("""
                INSERT INTO ledger_block (seq, block_type, cert_no, data_hash, prev_hash, block_hash, actor, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                block.getSeq(), block.getType().name(), block.getCertNo(), block.getDataHash(),
                block.getPrevHash(), block.getBlockHash(), block.getActor(), block.getCreatedAt());
        jdbc.update("UPDATE ledger_head SET last_seq = ?, last_hash = ? WHERE id = 1", block.getSeq(), block.getBlockHash());
        return block;
    }

    public Head head() {
        return jdbc.queryForObject("SELECT last_seq, last_hash FROM ledger_head WHERE id = 1",
                (rs, n) -> new Head(rs.getLong(1), rs.getString(2)));
    }

    public Optional<LedgerBlock> findLast() {
        return jdbc.query("SELECT * FROM ledger_block ORDER BY seq DESC LIMIT 1", MAPPER).stream().findFirst();
    }

    public Optional<LedgerBlock> findBySeq(long seq) {
        return jdbc.query("SELECT * FROM ledger_block WHERE seq = ?", MAPPER, seq).stream().findFirst();
    }

    public List<LedgerBlock> findByCertNo(String certNo) {
        return jdbc.query("SELECT * FROM ledger_block WHERE cert_no = ? ORDER BY seq", MAPPER, certNo);
    }

    public List<LedgerBlock> findAll() {
        return jdbc.query("SELECT * FROM ledger_block ORDER BY seq", MAPPER);
    }

    public List<LedgerBlock> findLatest(int limit) {
        return jdbc.query("SELECT * FROM ledger_block ORDER BY seq DESC LIMIT ?", MAPPER, limit);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM ledger_block", Long.class);
        return n == null ? 0 : n;
    }
}
