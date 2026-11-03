package com.certichain.server.model;

public record AdminUser(long id, String username, String fullName, String passwordHash, Role role) {
}
