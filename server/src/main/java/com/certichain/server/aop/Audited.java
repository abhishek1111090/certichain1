package com.certichain.server.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a service method whose every call is written to the audit trail by {@link AuditAspect}. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** Action name stored in audit_log.action, e.g. ISSUE_CERTIFICATE. */
    String value();
}
