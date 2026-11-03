package com.certichain.desktop.dao;

import com.certichain.core.crypto.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Logins (same admin_user table as the web console) and the shared audit trail. */
public class AccountDao {

    public record Account(String username, String fullName, String role) {
        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }

    public Optional<Account> authenticate(Connection c, String username, char[] password) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT username, full_name, role, password_hash FROM admin_user WHERE username = ?")) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordHasher.verify(password, rs.getString("password_hash"))) {
                    return Optional.of(new Account(rs.getString("username"), rs.getString("full_name"), rs.getString("role")));
                }
                return Optional.empty();
            }
        }
    }

    public int countUsers(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM admin_user");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** First run without the server: create the same default accounts it would. */
    public void createDefaultAccounts(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO admin_user (username, full_name, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)")) {
            Object[][] rows = {{"admin", "Dr. Anita Sharma", "admin123", "ADMIN"}, {"registrar", "Rohit Verma", "registrar123", "STAFF"}};
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setString(3, PasswordHasher.hash(((String) r[2]).toCharArray()));
                ps.setString(4, (String) r[3]);
                ps.setLong(5, System.currentTimeMillis());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void audit(Connection c, String actor, String action, String detail, String outcome, long ms) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO audit_log (actor, action, detail, outcome, duration_ms, source, created_at)
                VALUES (?, ?, ?, ?, ?, 'DESKTOP', ?)""")) {
            ps.setString(1, actor);
            ps.setString(2, action);
            ps.setString(3, detail.length() > 500 ? detail.substring(0, 497) + "..." : detail);
            ps.setString(4, outcome);
            ps.setLong(5, ms);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }
}
