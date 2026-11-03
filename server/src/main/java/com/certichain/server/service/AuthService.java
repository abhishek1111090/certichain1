package com.certichain.server.service;

import com.certichain.core.crypto.PasswordHasher;
import com.certichain.server.model.AdminSession;
import com.certichain.server.model.AdminUser;
import com.certichain.server.repository.AuditRepository;
import com.certichain.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Password login with brute-force protection: 5 wrong passwords lock the username for
 * 5 minutes. Attempt counters live in a ConcurrentHashMap shared by all request threads.
 */
@Service
public class AuthService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCK_MS = 5 * 60_000;

    public static final class LockedOutException extends RuntimeException {
        public LockedOutException(long secondsLeft) {
            super("Too many failed attempts. Try again in " + secondsLeft + " seconds.");
        }
    }

    private record Attempts(int failures, long lockedUntil) {
    }

    private final UserRepository users;
    private final AuditRepository audit;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public AuthService(UserRepository users, AuditRepository audit) {
        this.users = users;
        this.audit = audit;
    }

    public Optional<AdminSession> login(String username, String password, String ip) {
        String key = username == null ? "" : username.trim().toLowerCase();
        Attempts a = attempts.get(key);
        long now = System.currentTimeMillis();
        if (a != null && a.lockedUntil() > now) {
            throw new LockedOutException((a.lockedUntil() - now) / 1000 + 1);
        }
        Optional<AdminUser> user = users.findByUsername(key);
        // Always run PBKDF2 so "unknown user" and "wrong password" take the same time.
        boolean ok = PasswordHasher.verify(password == null ? new char[0] : password.toCharArray(),
                user.map(AdminUser::passwordHash).orElse(DUMMY_HASH)) && user.isPresent();
        if (!ok) {
            attempts.compute(key, (k, old) -> {
                int failures = (old == null || old.lockedUntil() > 0 && old.lockedUntil() <= now ? 0 : old.failures()) + 1;
                return new Attempts(failures, failures >= MAX_ATTEMPTS ? now + LOCK_MS : 0);
            });
            audit.log(key.isEmpty() ? "?" : key, "LOGIN", "from " + ip, "FAILED", 0);
            return Optional.empty();
        }
        attempts.remove(key);
        AdminUser u = user.get();
        audit.log(u.username(), "LOGIN", "from " + ip, "SUCCESS", 0);
        return Optional.of(new AdminSession(u.username(), u.fullName(), u.role()));
    }

    private static final String DUMMY_HASH = PasswordHasher.hash("dummy-password".toCharArray());
}
