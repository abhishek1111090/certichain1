package com.certichain.server.service;

import com.certichain.core.chain.ChainVerifier;
import com.certichain.core.chain.IntegrityReport;
import com.certichain.server.aop.Audited;
import com.certichain.server.event.TamperDetectedEvent;
import com.certichain.server.repository.CertificateRepository;
import com.certichain.server.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full-chain audits, run on demand and by a background watchdog. The last report is
 * kept in memory (volatile, read by request threads) for the dashboard banner.
 */
@Service
public class IntegrityService {

    private static final Logger log = LoggerFactory.getLogger(IntegrityService.class);

    private final LedgerRepository ledger;
    private final CertificateRepository certificates;
    private final ChainVerifier chainVerifier;
    private final ApplicationEventPublisher events;

    private volatile IntegrityReport lastReport;
    private volatile String lastTrigger;

    public IntegrityService(LedgerRepository ledger, CertificateRepository certificates,
                            ChainVerifier chainVerifier, ApplicationEventPublisher events) {
        this.ledger = ledger;
        this.certificates = certificates;
        this.chainVerifier = chainVerifier;
        this.events = events;
    }

    @Audited("RUN_INTEGRITY_AUDIT")
    @Transactional(readOnly = true)
    public IntegrityReport runManualAudit() {
        return audit("manual");
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "${certichain.watchdog.interval-ms:60000}")
    @Transactional(readOnly = true)
    public void watchdog() {
        IntegrityReport r = audit("watchdog");
        log.info("Watchdog: {} ({} ms on {} threads)", r.summary(), r.getDurationMs(), r.getThreadsUsed());
    }

    /** Reads blocks, certificates and head in one read-only transaction for a consistent snapshot. */
    @Transactional(readOnly = true)
    public IntegrityReport audit(String trigger) {
        LedgerRepository.Head head = ledger.head();
        IntegrityReport report = chainVerifier.verify(ledger.findAll(), certificates.findAll(), head.seq(), head.hash());
        boolean wasIntact = lastReport == null || lastReport.isIntact();
        lastReport = report;
        lastTrigger = trigger;
        // Alert on the transition to "tampered" so the watchdog does not raise one every minute.
        if (!report.isIntact() && (wasIntact || "manual".equals(trigger))) {
            events.publishEvent(new TamperDetectedEvent(this, report.getCompromisedCertificates(), report.summary()));
        }
        return report;
    }

    public IntegrityReport getLastReport() {
        return lastReport;
    }

    public String getLastTrigger() {
        return lastTrigger;
    }
}
