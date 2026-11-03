package com.certichain.server.repository;

import com.certichain.server.model.AdminUser;
import com.certichain.server.model.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<AdminUser> MAPPER = (rs, n) -> new AdminUser(
            rs.getLong("id"), rs.getString("username"), rs.getString("full_name"),
            rs.getString("password_hash"), Role.valueOf(rs.getString("role")));

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AdminUser> findByUsername(String username) {
        return jdbc.query("SELECT * FROM admin_user WHERE username = ?", MAPPER, username).stream().findFirst();
    }

    public List<AdminUser> findAll() {
        return jdbc.query("SELECT * FROM admin_user ORDER BY id", MAPPER);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM admin_user", Long.class);
        return n == null ? 0 : n;
    }

    public void insert(String username, String fullName, String passwordHash, Role role) {
        jdbc.update("INSERT INTO admin_user (username, full_name, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)",
                username, fullName, passwordHash, role.name(), System.currentTimeMillis());
    }
}
