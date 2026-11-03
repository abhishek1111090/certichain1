package com.certichain.server.service;

import com.certichain.core.chain.CertificateVerifier;
import com.certichain.core.chain.VerificationOutcome;
import com.certichain.core.chain.VerificationResult;
import com.certichain.core.model.BlockType;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.LedgerBlock;
import com.certichain.server.event.TamperDetectedEvent;
import com.certichain.server.repository.CertificateRepository;
import com.certichain.server.repository.LedgerRepository;
import com.certichain.server.repository.VerificationLogRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class VerificationService {

    public enum Channel { WEB, QR, API, ADMIN }

    private final CertificateRepository certificates;
    private final LedgerRepository ledger;
    private final VerificationLogRepository verificationLog;
    private final ApplicationEventPublisher events;

    public VerificationService(CertificateRepository certificates, LedgerRepository ledger,
                               VerificationLogRepository verificationLog, ApplicationEventPublisher events) {
        this.certificates = certificates;
        this.ledger = ledger;
        this.verificationLog = verificationLog;
        this.events = events;
    }

    public static String normalise(String certNo) {
        return certNo == null ? "" : certNo.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
    }

    /** Verifies and records the lookup (except admin views, which are not "presented" certificates). */
    public VerificationResult verify(String rawCertNo, Channel channel, String ip, String userAgent) {
        String certNo = normalise(rawCertNo);
        VerificationResult result = check(certNo);
        if (channel != Channel.ADMIN && !certNo.isEmpty()) {
            verificationLog.log(certNo, result.getOutcome().name(), channel.name(), ip, userAgent);
            if (result.getOutcome() == VerificationOutcome.TAMPERED) {
                events.publishEvent(new TamperDetectedEvent(this, Set.of(certNo),
                        "Tampered certificate presented for verification via " + channel));
            }
        }
        return result;
    }

    private VerificationResult check(String certNo) {
        if (certNo.isEmpty()) {
            return VerificationResult.notFound(certNo);
        }
        Optional<Certificate> cert = certificates.findByCertNo(certNo);
        List<LedgerBlock> blocks = ledger.findByCertNo(certNo);
        if (cert.isEmpty()) {
            // A deleted row whose ledger block survives is tampering, not "never issued".
            if (blocks.stream().anyMatch(b -> b.getType() == BlockType.ISSUE)) {
                return new VerificationResult(certNo, VerificationOutcome.TAMPERED, null, blocks.get(0), null,
                        List.of(new VerificationResult.Check("Certificate record present", false,
                                "Issued in block #" + blocks.get(0).getSeq() + " but the record was deleted")));
            }
            return VerificationResult.notFound(certNo);
        }
        LedgerBlock issue = blocks.stream().filter(b -> b.getType() == BlockType.ISSUE).findFirst().orElse(null);
        LedgerBlock prev = issue == null || issue.getSeq() <= 1 ? null : ledger.findBySeq(issue.getSeq() - 1).orElse(null);
        LedgerBlock next = issue == null ? null : ledger.findBySeq(issue.getSeq() + 1).orElse(null);
        return CertificateVerifier.verify(cert.get(), blocks, prev, next);
    }
}
