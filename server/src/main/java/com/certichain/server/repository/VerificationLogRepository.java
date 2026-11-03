package com.certichain.server.repository;

import com.certichain.server.model.VerificationEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class VerificationLogRepository {

    private static final RowMapper<VerificationEntry> MAPPER = (rs, n) -> new VerificationEntry(
            rs.getString("cert_no"), rs.getString("result"), rs.getString("channel"),
            rs.getString("client_ip"), rs.getString("user_agent"), rs.getLong("created_at"));

    private final JdbcTemplate jdbc;

    public VerificationLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String certNo, String result, String channel, String ip, String userAgent) {
        jdbc.update("""
                INSERT INTO verification_log (cert_no, result, channel, client_ip, user_agent, created_at)
                VALUES (?, ?, ?, ?, ?, ?)""",
                cut(certNo, 60), result, channel, cut(ip, 64), cut(userAgent, 255), System.currentTimeMillis());
    }

    public List<VerificationEntry> findByCertNo(String certNo, int limit) {
        return jdbc.query("SELECT * FROM verification_log WHERE cert_no = ? ORDER BY id DESC LIMIT ?", MAPPER, certNo, limit);
    }

    /** Lookups that did not come back AUTHENTIC: someone presented a fake, edited or revoked certificate. */
    public List<VerificationEntry> findSuspicious(int limit) {
        return jdbc.query("SELECT * FROM verification_log WHERE result <> 'AUTHENTIC' ORDER BY id DESC LIMIT ?", MAPPER, limit);
    }

    public long countSince(long since, boolean suspiciousOnly) {
        String sql = "SELECT COUNT(*) FROM verification_log WHERE created_at >= ?" + (suspiciousOnly ? " AND result <> 'AUTHENTIC'" : "");
        Long n = jdbc.queryForObject(sql, Long.class, since);
        return n == null ? 0 : n;
    }

    /** result -> count, for lookups since the given time. */
    public Map<String, Long> countByResultSince(long since) {
        Map<String, Long> out = new LinkedHashMap<>();
        jdbc.query("SELECT result, COUNT(*) FROM verification_log WHERE created_at >= ? GROUP BY result ORDER BY result",
                rs -> {
                    out.put(rs.getString(1), rs.getLong(2));
                }, since);
        return out;
    }

    public List<Long> timestampsSince(long since) {
        return jdbc.queryForList("SELECT created_at FROM verification_log WHERE created_at >= ?", Long.class, since);
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }
}
