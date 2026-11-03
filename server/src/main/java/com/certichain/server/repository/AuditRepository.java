package com.certichain.server.repository;

import com.certichain.server.model.AuditEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditRepository {

    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String actor, String action, String detail, String outcome, long durationMs) {
        String safeDetail = detail == null ? null : detail.length() > 500 ? detail.substring(0, 497) + "..." : detail;
        jdbc.update("""
                INSERT INTO audit_log (actor, action, detail, outcome, duration_ms, source, created_at)
                VALUES (?, ?, ?, ?, ?, 'WEB', ?)""",
                actor, action, safeDetail, outcome, durationMs, System.currentTimeMillis());
    }

    public List<AuditEntry> findLatest(int limit) {
        return jdbc.query("SELECT * FROM audit_log ORDER BY id DESC LIMIT ?", (rs, n) -> new AuditEntry(
                rs.getLong("id"), rs.getString("actor"), rs.getString("action"), rs.getString("detail"),
                rs.getString("outcome"), rs.getLong("duration_ms"), rs.getString("source"), rs.getLong("created_at")), limit);
    }
}
