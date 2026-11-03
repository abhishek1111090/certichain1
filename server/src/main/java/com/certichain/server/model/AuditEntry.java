package com.certichain.server.model;

import java.util.Date;

public record AuditEntry(long id, String actor, String action, String detail, String outcome,
                         long durationMs, String source, long createdAt) {

    // Getters for JSP EL, which does not see record accessors.
    public long getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public String getOutcome() { return outcome; }
    public long getDurationMs() { return durationMs; }
    public String getSource() { return source; }
    public Date getCreated() { return new Date(createdAt); }
}
