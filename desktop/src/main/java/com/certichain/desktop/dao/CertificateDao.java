package com.certichain.desktop.dao;

import com.certichain.core.model.Certificate;
import com.certichain.core.model.CertificateStatus;
import com.certichain.core.model.CredentialType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for the certificate table using PreparedStatement and ResultSet directly. */
public class CertificateDao {

    private static final String INSERT = """
            INSERT INTO certificate (cert_no, student_name, roll_no, email, program, credential_type, title,
                                     grade, issue_date, issued_by, status, revoke_reason, data_hash, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    public List<Certificate> findAll(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM certificate ORDER BY created_at DESC")) {
            List<Certificate> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        }
    }

    public Optional<Certificate> find(Connection c, String certNo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM certificate WHERE cert_no = ?")) {
            ps.setString(1, certNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean exists(Connection c, String certNo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM certificate WHERE cert_no = ?")) {
            ps.setString(1, certNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Connection c, Certificate cert) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT)) {
            bind(ps, cert);
            ps.executeUpdate();
        }
    }

    /** JDBC batch: one round trip for many rows. Runs inside the caller's transaction. */
    public int[] insertBatch(Connection c, List<Certificate> certs) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT)) {
            for (Certificate cert : certs) {
                bind(ps, cert);
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }

    public void markRevoked(Connection c, String certNo, String reason) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE certificate SET status = 'REVOKED', revoke_reason = ? WHERE cert_no = ?")) {
            ps.setString(1, reason);
            ps.setString(2, certNo);
            ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Certificate cert) throws SQLException {
        ps.setString(1, cert.getCertNo());
        ps.setString(2, cert.getStudentName());
        ps.setString(3, cert.getRollNo());
        setNullable(ps, 4, cert.getEmail());
        ps.setString(5, cert.getProgram());
        ps.setString(6, cert.getCredentialType().name());
        ps.setString(7, cert.getTitle());
        setNullable(ps, 8, cert.getGrade());
        ps.setDate(9, Date.valueOf(cert.getIssueDate()));
        ps.setString(10, cert.getIssuedBy());
        ps.setString(11, cert.getStatus().name());
        setNullable(ps, 12, cert.getRevokeReason());
        ps.setString(13, cert.getDataHash());
        ps.setLong(14, cert.getCreatedAt());
    }

    private static void setNullable(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null || v.isBlank()) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, v);
        }
    }

    static Certificate map(ResultSet rs) throws SQLException {
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
    }
}
