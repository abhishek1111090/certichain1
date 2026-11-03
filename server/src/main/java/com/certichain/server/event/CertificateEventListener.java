package com.certichain.server.event;

import com.certichain.core.model.Certificate;
import com.certichain.server.config.InstitutionProfile;
import com.certichain.server.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to ledger events. Issue/revoke listeners run only AFTER_COMMIT (a rolled back
 * issue never notifies anyone) and on a separate thread (@Async) so the admin's request
 * is not slowed down by notification work.
 */
@Component
public class CertificateEventListener {

    private static final Logger log = LoggerFactory.getLogger(CertificateEventListener.class);

    private final AuditRepository auditRepository;
    private final InstitutionProfile institution;

    public CertificateEventListener(AuditRepository auditRepository, InstitutionProfile institution) {
        this.auditRepository = auditRepository;
        this.institution = institution;
    }

    @Async
    @TransactionalEventListener
    public void onIssued(CertificateIssuedEvent event) {
        Certificate c = event.getCertificate();
        if (c.getEmail() == null || c.getEmail().isBlank()) {
            return;
        }
        // Email gateway stub: in production this would hand the message to an SMTP relay.
        String message = "To " + c.getEmail() + ": your " + c.getCredentialType().getDisplayName() + " '" + c.getTitle()
                + "' from " + institution.getName() + " is sealed in block #" + event.getBlock().getSeq()
                + ". Share /verify/" + c.getCertNo() + " with employers.";
        auditRepository.log("SYSTEM", "NOTIFY_STUDENT", message, "QUEUED", 0);
        log.info("[{}] {}", Thread.currentThread().getName(), message);
    }

    @Async
    @TransactionalEventListener
    public void onRevoked(CertificateRevokedEvent event) {
        Certificate c = event.getCertificate();
        auditRepository.log("SYSTEM", "NOTIFY_REVOCATION",
                c.getCertNo() + " revoked (" + c.getRevokeReason() + "); employers who verify it will now see REVOKED",
                "QUEUED", 0);
    }

    @EventListener
    public void onTamper(TamperDetectedEvent event) {
        log.warn("TAMPER ALERT: {} -> {}", event.getSummary(), event.getCertNos());
        auditRepository.log("SYSTEM", "TAMPER_ALERT", event.getSummary() + " " + event.getCertNos(), "ALERT", 0);
    }
}
