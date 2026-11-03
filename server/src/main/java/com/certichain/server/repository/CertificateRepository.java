package com.certichain.server.repository;

import com.certichain.core.model.Certificate;
import com.certichain.core.model.CertificateStatus;
import com.certichain.core.model.CredentialType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CertificateRepository {

    static final RowMapper<Certificate> MAPPER = (rs, n) -> {
        Certificate c = new Certificate();
        c.setCertNo(rs.getString("cert_no"));
        c.setStudentName(rs.getString("student_name"));
        c.setRollNo(rs.getString("roll_no"));
        c.setEmail(rs.getString("email"));
        c.setProgram(rs.getString("program"));
        c.setCredentialType(CredentialType.valueOf(rs.getString("credential_type")));
        c.setTitle(rs.getString("title"));
        c.setGrade(rs.getString("grade"));
        c.setIssueDate(rs.getDate("issue_date").toLocalDate());
        c.setIssuedBy(rs.getString("issued_by"));
        c.setStatus(CertificateStatus.valueOf(rs.getString("status")));
        c.setRevokeReason(rs.getString("revoke_reason"));
        c.setDataHash(rs.getString("data_hash"));
        c.setCreatedAt(rs.getLong("created_at"));
        return c;
    };

    private final JdbcTemplate jdbc;

    public CertificateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Certificate c) {
        jdbc.update("""
                INSERT INTO certificate (cert_no, student_name, roll_no, email, program, credential_type, title,
                                         grade, issue_date, issued_by, status, revoke_reason, data_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                c.getCertNo(), c.getStudentName().trim(), c.getRollNo().trim(), emptyToNull(c.getEmail()),
                c.getProgram().trim(), c.getCredentialType().name(), c.getTitle().trim(), emptyToNull(c.getGrade()),
                Date.valueOf(c.getIssueDate()), c.getIssuedBy(), c.getStatus().name(), c.getRevokeReason(),
                c.getDataHash(), c.getCreatedAt());
    }

    public void markRevoked(String certNo, String reason) {
        jdbc.update("UPDATE certificate SET status = 'REVOKED', revoke_reason = ? WHERE cert_no = ?", reason, certNo);
    }

    public Optional<Certificate> findByCertNo(String certNo) {
        return jdbc.query("SELECT * FROM certificate WHERE cert_no = ?", MAPPER, certNo).stream().findFirst();
    }

    public boolean exists(String certNo) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM certificate WHERE cert_no = ?", Integer.class, certNo);
        return n != null && n > 0;
    }

    public List<Certificate> findAll() {
        return jdbc.query("SELECT * FROM certificate ORDER BY created_at DESC", MAPPER);
    }

    /** Dynamic search: every filter is optional, values are always bound as parameters. */
    public List<Certificate> search(String text, String status, String type, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM certificate WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            sql.append(" AND (LOWER(student_name) LIKE ? OR LOWER(roll_no) LIKE ? OR LOWER(cert_no) LIKE ? OR LOWER(title) LIKE ?)");
            String like = "%" + text.trim().toLowerCase() + "%";
            for (int i = 0; i < 4; i++) {
                args.add(like);
            }
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND credential_type = ?");
            args.add(type);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
