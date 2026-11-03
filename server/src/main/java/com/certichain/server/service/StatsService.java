package com.certichain.server.service;

import com.certichain.core.model.CertificateStatus;
import com.certichain.server.repository.LedgerRepository;
import com.certichain.server.repository.VerificationLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Aggregate numbers for the dashboard and landing page. */
@Service
public class StatsService {

    public record DayBar(String label, long count, int percent) {
        public String getLabel() { return label; }
        public long getCount() { return count; }
        public int getPercent() { return percent; }
    }

    private final JdbcTemplate jdbc;
    private final LedgerRepository ledger;
    private final VerificationLogRepository verificationLog;

    public StatsService(JdbcTemplate jdbc, LedgerRepository ledger, VerificationLogRepository verificationLog) {
        this.jdbc = jdbc;
        this.ledger = ledger;
        this.verificationLog = verificationLog;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        long total = count("SELECT COUNT(*) FROM certificate");
        long revoked = count("SELECT COUNT(*) FROM certificate WHERE status = '" + CertificateStatus.REVOKED + "'");
        long dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long weekStart = LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        m.put("total", total);
        m.put("valid", total - revoked);
        m.put("revoked", revoked);
        m.put("blocks", ledger.count());
        m.put("verificationsToday", verificationLog.countSince(dayStart, false));
        m.put("suspiciousWeek", verificationLog.countSince(weekStart, true));
        m.put("verificationsWeek", verificationLog.countSince(weekStart, false));
        m.put("resultsWeek", verificationLog.countByResultSince(weekStart));
        m.put("byType", byType());
        m.put("verificationBars", verificationBars(weekStart));
        return m;
    }

    private Map<String, Long> byType() {
        Map<String, Long> out = new TreeMap<>();
        jdbc.query("SELECT credential_type, COUNT(*) FROM certificate GROUP BY credential_type",
                rs -> {
                    out.put(rs.getString(1), rs.getLong(2));
                });
        return out;
    }

    /** One bar per day for the last 7 days. */
    private List<DayBar> verificationBars(long weekStart) {
        Map<LocalDate, Long> perDay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            perDay.put(LocalDate.now().minusDays(i), 0L);
        }
        for (Long ts : verificationLog.timestampsSince(weekStart)) {
            LocalDate d = java.time.Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate();
            perDay.merge(d, 1L, Long::sum);
        }
        long max = Math.max(1, perDay.values().stream().mapToLong(Long::longValue).max().orElse(1));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
        List<DayBar> bars = new ArrayList<>();
        perDay.forEach((d, n) -> bars.add(new DayBar(d.format(f), n, (int) Math.round(100.0 * n / max))));
        return bars;
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
