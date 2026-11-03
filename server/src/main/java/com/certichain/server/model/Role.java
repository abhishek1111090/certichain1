package com.certichain.server.model;

public enum Role {
    /** Can issue, revoke and run audits. */
    ADMIN,
    /** Can issue and view, but not revoke. */
    STAFF
}
