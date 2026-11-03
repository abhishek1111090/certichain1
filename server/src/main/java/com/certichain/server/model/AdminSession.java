package com.certichain.server.model;

import java.io.Serializable;

/** What is kept in the HttpSession after a successful login. Serializable so sessions can be persisted. */
public class AdminSession implements Serializable {

    public static final String ATTR = "admin";
    public static final String CSRF_ATTR = "csrfToken";

    private final String username;
    private final String fullName;
    private final Role role;
    private final long loginAt;

    public AdminSession(String username, String fullName, Role role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.loginAt = System.currentTimeMillis();
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }
    public long getLoginAt() { return loginAt; }

    public String getInitials() {
        StringBuilder sb = new StringBuilder();
        for (String part : fullName.split("\\s+")) {
            if (!part.isEmpty() && Character.isLetter(part.charAt(0)) && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }
}
