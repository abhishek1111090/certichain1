package com.certichain.server.event;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

/** Raised by the integrity watchdog or a public lookup that hits an altered record. */
public class TamperDetectedEvent extends ApplicationEvent {

    private final Set<String> certNos;
    private final String summary;

    public TamperDetectedEvent(Object source, Set<String> certNos, String summary) {
        super(source);
        this.certNos = Set.copyOf(certNos);
        this.summary = summary;
    }

    public Set<String> getCertNos() { return certNos; }
    public String getSummary() { return summary; }
}
